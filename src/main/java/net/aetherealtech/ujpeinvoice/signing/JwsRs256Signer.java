package net.aetherealtech.ujpeinvoice.signing;

import net.aetherealtech.ujpeinvoice.internal.json.JsonArray;
import net.aetherealtech.ujpeinvoice.internal.json.JsonObject;
import net.aetherealtech.ujpeinvoice.internal.json.JsonString;
import net.aetherealtech.ujpeinvoice.internal.json.JsonValue;
import net.aetherealtech.ujpeinvoice.internal.json.JsonWriter;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Signs a payload as a compact JWS using RS256 (RSASSA-PKCS1-v1_5 with SHA-256), built from JDK-only
 * primitives — {@link Signature}, {@link Base64}, and this library's own minimal JSON writer for the
 * header — rather than a JOSE library. RS256 is four moving parts (a header, a base64url encode, a
 * {@code SHA256withRSA} signature, another base64url encode); pulling in Nimbus JOSE+JWT for that
 * would trade a few hundred lines this project can read and test for a dependency, its own
 * transitive closure, and a CVE feed to track — for a JWS implementation this library needs to hold
 * to a much smaller contract than a general-purpose one does (always RS256, always JWS not JWE,
 * never has to verify a compact serialization it did not itself produce). If a future requirement
 * needs more of the JOSE surface than that, reconsider then.
 *
 * <p>The certificate chain, if supplied, is carried in the JWS header's {@code x5c} claim (RFC 7515
 * §4.1.6) so a verifier can recover the signer's certificate from the token itself — the shape a
 * qualified-certificate signature commonly takes. Whether the UJP gateway actually expects {@code
 * x5c} on this endpoint is unconfirmed (see the README's "Specification status" section); omitting
 * the chain produces a bare {@code {"alg":"RS256"}} header, which is the safer default until that is
 * confirmed one way or the other.
 */
public final class JwsRs256Signer implements Signer {

    private static final String JAVA_SECURITY_ALGORITHM = "SHA256withRSA";

    private final PrivateKey privateKey;
    private final List<X509Certificate> certificateChain;

    public JwsRs256Signer(PrivateKey privateKey) {
        this(privateKey, List.of());
    }

    public JwsRs256Signer(PrivateKey privateKey, List<X509Certificate> certificateChain) {
        if (privateKey == null) {
            throw new NullPointerException("privateKey must not be null");
        }
        if (!"RSA".equals(privateKey.getAlgorithm())) {
            throw new IllegalArgumentException(
                    "JwsRs256Signer requires an RSA private key, got algorithm: " + privateKey.getAlgorithm());
        }
        this.privateKey = privateKey;
        this.certificateChain = List.copyOf(certificateChain);
    }

    @Override
    public String signCompact(byte[] payload) {
        String encodedHeader = base64Url(header().getBytes(StandardCharsets.UTF_8));
        String encodedPayload = base64Url(payload);
        byte[] signingInput = (encodedHeader + "." + encodedPayload).getBytes(StandardCharsets.US_ASCII);
        String encodedSignature = base64Url(sign(signingInput));
        return encodedHeader + "." + encodedPayload + "." + encodedSignature;
    }

    private String header() {
        JsonObject.Builder builder = JsonObject.builder().put("alg", "RS256");
        if (!certificateChain.isEmpty()) {
            List<JsonValue> x5c = new ArrayList<>(certificateChain.size());
            for (X509Certificate certificate : certificateChain) {
                try {
                    x5c.add(new JsonString(Base64.getEncoder().encodeToString(certificate.getEncoded())));
                } catch (CertificateEncodingException e) {
                    throw new SigningException("Failed to DER-encode a certificate for the JWS x5c header", e);
                }
            }
            builder.put("x5c", JsonArray.of(x5c));
        }
        return JsonWriter.write(builder.build());
    }

    private byte[] sign(byte[] signingInput) {
        try {
            Signature signature = Signature.getInstance(JAVA_SECURITY_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(signingInput);
            return signature.sign();
        } catch (GeneralSecurityException e) {
            throw new SigningException("Failed to compute the RS256 signature", e);
        }
    }

    private static String base64Url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }
}
