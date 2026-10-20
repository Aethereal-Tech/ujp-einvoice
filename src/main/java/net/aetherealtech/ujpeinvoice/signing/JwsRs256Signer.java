package net.aetherealtech.ujpeinvoice.signing;

import net.aetherealtech.ujpeinvoice.ProvisionalSpec;
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
 * <p>The protected header is exactly {@code {"alg":"RS256","typ":"JWT"}}, as UJP's FAQ states it
 * (efakturawiki.ujp.gov.mk, {@code /ЧПП}, page {@code updated-at} 2026-06-09, retrieved 2026-10-19, section
 * 2 „Генерирање на JWS барање": the header example {@code {"alg":"RS256","typ":"JWT"}} and
 * {@code signature = RSA-SHA256(BASE64URL(header) . BASE64URL(payload))}). The gateway's API
 * description shows a truncated example token whose header decodes to {@code {"alg":"RS256"}}; that example
 * has an empty payload and is not a statement of the header, so the FAQ is followed.
 *
 * <p><strong>{@code x5c} is opt-in, and its acceptance is unconfirmed.</strong> No obtained document mentions
 * it: the gateway identifies the signing certificate by the one registered for the company plus the
 * {@code X-SERIAL-NUMBER} request header ({@code E5012}, serial mismatch). So the single-argument
 * constructor writes no {@code x5c}, and the header is the two members above. Only the constructor that is
 * handed a certificate chain adds {@code x5c} (RFC 7515 §4.1.6, base64 DER, leaf first), which a verifier
 * may use to recover the certificate from the token alone. Whether the gateway accepts, ignores or rejects
 * the claim is not known.
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
        JsonObject.Builder builder = JsonObject.builder().put("alg", "RS256").put("typ", "JWT");
        if (!certificateChain.isEmpty()) {
            builder.put("x5c", x5c());
        }
        return JsonWriter.write(builder.build());
    }

    @ProvisionalSpec("x5c: no obtained document mentions it; the gateway identifies the certificate by the "
            + "registered certificate and X-SERIAL-NUMBER (E5012). Written only when a chain is supplied.")
    private JsonArray x5c() {
        List<JsonValue> encoded = new ArrayList<>(certificateChain.size());
        for (X509Certificate certificate : certificateChain) {
            try {
                encoded.add(new JsonString(Base64.getEncoder().encodeToString(certificate.getEncoded())));
            } catch (CertificateEncodingException e) {
                throw new SigningException("Failed to DER-encode a certificate for the JWS x5c header", e);
            }
        }
        return JsonArray.of(encoded);
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
