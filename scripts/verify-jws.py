#!/usr/bin/env python3
"""Independent cross-check of JwsRs256Signer against OpenSSL.

JwsRs256SignerTest already verifies signatures with java.security.Signature — but that is the same
JDK RS256 implementation the signer itself uses underneath, so it cannot catch a bug shared by both
directions. This script asks a second, unrelated implementation (OpenSSL's own RSA/SHA-256) to
verify a JWS this library actually produced, with a key OpenSSL itself generated.

What it does, end to end:
  1. Generates a fresh 2048-bit RSA key pair with `openssl genpkey` (PKCS#8 DER + a PEM public key).
  2. Compiles and runs a throwaway Java program against this project's target/classes that loads
     that PKCS#8 key and signs a fixed payload with JwsRs256Signer, printing the compact JWS.
  3. Splits the compact JWS, base64url-decodes the signature, and shells out to
     `openssl dgst -sha256 -verify` with the OpenSSL-generated public key to verify the signing
     input (header + "." + payload) against that signature.
  4. Also decodes the header and payload to confirm they round-trip to the expected JSON.

Prerequisites: `openssl`, `javac`/`java` (JDK 25+) on PATH, and `mvn ... compile` already run so
target/classes exists (run `./mvnw compile` first if needed).

Usage: python3 scripts/verify-jws.py
Exit code 0 and "PASS" on success; non-zero and "FAIL" otherwise.

This is a developer verification tool, not part of the build or the test suite — JaCoCo and CI never
invoke it. Keep it working, but treat it as documentation-by-demonstration rather than a gate.
"""
import base64
import pathlib
import subprocess
import sys
import tempfile

REPO_ROOT = pathlib.Path(__file__).resolve().parent.parent
TARGET_CLASSES = REPO_ROOT / "target" / "classes"

SIGNER_HARNESS = """
import net.aetherealtech.ujpeinvoice.signing.JwsRs256Signer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;

public class SignerHarness {
    public static void main(String[] args) throws Exception {
        byte[] der = Files.readAllBytes(Path.of(args[0]));
        PrivateKey key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        JwsRs256Signer signer = new JwsRs256Signer(key);
        byte[] payload = args[1].getBytes(StandardCharsets.UTF_8);
        System.out.print(signer.signCompact(payload));
    }
}
"""

PAYLOAD = '{"invoiceNumber":"INV-CROSS-CHECK-1","grossTotal":"236.00"}'


def run(cmd, **kwargs):
    result = subprocess.run(cmd, capture_output=True, text=True, **kwargs)
    if result.returncode != 0:
        raise RuntimeError(f"Command failed: {' '.join(cmd)}\nstdout: {result.stdout}\nstderr: {result.stderr}")
    return result


def b64url_decode(segment: str) -> bytes:
    padding = "=" * (-len(segment) % 4)
    return base64.urlsafe_b64decode(segment + padding)


def main() -> int:
    if not TARGET_CLASSES.exists():
        print(f"FAIL: {TARGET_CLASSES} does not exist — run './mvnw compile' first.")
        return 1

    with tempfile.TemporaryDirectory() as tmp:
        tmp_path = pathlib.Path(tmp)
        private_pkcs8_pem = tmp_path / "private.pem"
        private_der = tmp_path / "private.der"
        public_pem = tmp_path / "public.pem"

        print("1. Generating a fresh RSA key pair with OpenSSL...")
        run(["openssl", "genpkey", "-algorithm", "RSA", "-pkeyopt", "rsa_keygen_bits:2048",
             "-out", str(private_pkcs8_pem)])
        run(["openssl", "pkey", "-in", str(private_pkcs8_pem), "-outform", "DER", "-out", str(private_der)])
        run(["openssl", "pkey", "-in", str(private_pkcs8_pem), "-pubout", "-out", str(public_pem)])

        print("2. Compiling and running the signing harness against JwsRs256Signer...")
        harness_java = tmp_path / "SignerHarness.java"
        harness_java.write_text(SIGNER_HARNESS)
        run(["javac", "--release", "25", "-cp", str(TARGET_CLASSES), "-d", str(tmp_path), str(harness_java)])
        signed = run(["java", "-cp", f"{tmp_path}{':' if sys.platform != 'win32' else ';'}{TARGET_CLASSES}",
                      "SignerHarness", str(private_der), PAYLOAD])
        compact_jws = signed.stdout.strip()
        print(f"   Compact JWS: {compact_jws}")

        parts = compact_jws.split(".")
        if len(parts) != 3:
            print(f"FAIL: expected 3 dot-separated segments, got {len(parts)}")
            return 1
        header_b64, payload_b64, signature_b64 = parts

        header_json = b64url_decode(header_b64).decode("utf-8")
        payload_json = b64url_decode(payload_b64).decode("utf-8")
        print(f"3. Decoded header:  {header_json}")
        print(f"   Decoded payload: {payload_json}")
        if header_json != '{"alg":"RS256"}':
            print(f"FAIL: unexpected header shape: {header_json}")
            return 1
        if payload_json != PAYLOAD:
            print("FAIL: decoded payload does not match what was signed")
            return 1

        signing_input = tmp_path / "signing_input.bin"
        signature_bin = tmp_path / "signature.bin"
        signing_input.write_bytes(f"{header_b64}.{payload_b64}".encode("ascii"))
        signature_bin.write_bytes(b64url_decode(signature_b64))

        print("4. Verifying the signature independently with `openssl dgst -verify`...")
        verify = run(["openssl", "dgst", "-sha256", "-verify", str(public_pem),
                      "-signature", str(signature_bin), str(signing_input)])
        print(f"   OpenSSL says: {verify.stdout.strip()}")
        if "Verified OK" not in verify.stdout:
            print("FAIL: OpenSSL did not report the signature as verified")
            return 1

    print("PASS: JwsRs256Signer's output verifies under an independent OpenSSL RS256 implementation.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
