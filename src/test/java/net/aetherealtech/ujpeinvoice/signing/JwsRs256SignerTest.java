package net.aetherealtech.ujpeinvoice.signing;

import net.aetherealtech.ujpeinvoice.internal.json.JsonObject;
import net.aetherealtech.ujpeinvoice.internal.json.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Signs with {@link JwsRs256Signer} and verifies independently, using nothing but
 * {@link java.security.Signature} against the matching public key — the same class the signer
 * itself uses, but driven the other direction, so this proves the compact serialization is
 * standards-shaped (base64url, {@code header.payload.signature}) and not just self-consistent.
 *
 * <p>A second, out-of-process cross-check against OpenSSL lives in {@code scripts/verify-jws.py} —
 * see that file's header comment for how to run it and what it demonstrated.
 */
class JwsRs256SignerTest {

    @Test
    void producesAVerifiableCompactJws() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        JwsRs256Signer signer = new JwsRs256Signer(keyPair.getPrivate());
        byte[] payload = "{\"invoiceNumber\":\"INV-1\"}".getBytes(StandardCharsets.UTF_8);

        String compact = signer.signCompact(payload);

        String[] parts = compact.split("\\.", -1);
        assertThat(parts).hasSize(3);

        Base64.Decoder decoder = Base64.getUrlDecoder();
        String header = new String(decoder.decode(parts[0]), StandardCharsets.UTF_8);
        assertThat(JsonParser.parse(header)).isEqualTo(JsonObject.builder().put("alg", "RS256").build());
        assertThat(decoder.decode(parts[1])).isEqualTo(payload);

        assertThat(verifiesIndependently(parts, keyPair.getPublic())).isTrue();
    }

    @Test
    void aTamperedPayloadFailsIndependentVerification() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        JwsRs256Signer signer = new JwsRs256Signer(keyPair.getPrivate());
        String compact = signer.signCompact("original".getBytes(StandardCharsets.UTF_8));

        String[] parts = compact.split("\\.", -1);
        String tamperedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("tampered".getBytes(StandardCharsets.UTF_8));
        String[] tampered = {parts[0], tamperedPayload, parts[2]};

        assertThat(verifiesIndependently(tampered, keyPair.getPublic())).isFalse();
    }

    @Test
    void aDifferentKeyPairFailsVerification() throws Exception {
        KeyPair signingKeyPair = generateRsaKeyPair();
        KeyPair otherKeyPair = generateRsaKeyPair();
        JwsRs256Signer signer = new JwsRs256Signer(signingKeyPair.getPrivate());
        String compact = signer.signCompact("payload".getBytes(StandardCharsets.UTF_8));

        assertThat(verifiesIndependently(compact.split("\\.", -1), otherKeyPair.getPublic())).isFalse();
    }

    @Test
    void rejectsNonRsaPrivateKeys() throws Exception {
        KeyPairGenerator ec = KeyPairGenerator.getInstance("EC");
        ec.initialize(256);
        PrivateKey ecKey = ec.generateKeyPair().getPrivate();

        assertThatThrownBy(() -> new JwsRs256Signer(ecKey)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullPrivateKey() {
        assertThatThrownBy(() -> new JwsRs256Signer(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void wrapsASigningFailureAsSigningException() {
        // A PrivateKey that claims to be RSA (so the constructor accepts it) but is not a key any
        // provider recognizes: Signature#initSign rejects it, and that GeneralSecurityException must
        // come out as this library's own SigningException, not leak the java.security type directly.
        PrivateKey notARealKey = new PrivateKey() {
            public String getAlgorithm() {
                return "RSA";
            }

            public String getFormat() {
                return "PKCS#8";
            }

            public byte[] getEncoded() {
                return new byte[]{1, 2, 3};
            }
        };
        JwsRs256Signer signer = new JwsRs256Signer(notARealKey);

        assertThatThrownBy(() -> signer.signCompact("payload".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(SigningException.class);
    }

    @Test
    void embedsTheCertificateChainAsX5cWhenSupplied() throws Exception {
        KeyStoreFixture fixture = KeyStoreFixture.load();
        JwsRs256Signer signer = new JwsRs256Signer(fixture.privateKey(), fixture.certificateChain());

        String compact = signer.signCompact("payload".getBytes(StandardCharsets.UTF_8));
        String[] parts = compact.split("\\.", -1);
        String header = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        JsonObject headerJson = (JsonObject) JsonParser.parse(header);

        assertThat(headerJson.getString("alg")).isEqualTo("RS256");
        List<X509Certificate> chain = fixture.certificateChain();
        String expectedDer = Base64.getEncoder().encodeToString(chain.get(0).getEncoded());
        assertThat(headerJson.getArray("x5c").items()).hasSize(chain.size());
        assertThat(headerJson.getArray("x5c").items().get(0))
                .isEqualTo(new net.aetherealtech.ujpeinvoice.internal.json.JsonString(expectedDer));

        assertThat(verifiesIndependently(parts, chain.get(0).getPublicKey())).isTrue();
    }

    private boolean verifiesIndependently(String[] parts, PublicKey publicKey) throws Exception {
        byte[] signingInput = (parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII);
        byte[] signatureBytes = Base64.getUrlDecoder().decode(parts[2]);

        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(publicKey);
        verifier.update(signingInput);
        return verifier.verify(signatureBytes);
    }

    private static KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }
}
