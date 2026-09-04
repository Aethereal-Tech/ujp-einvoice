package net.aetherealtech.ujpeinvoice.signing;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * One JVM, many organizations: each has its own qualified certificate, and a signer built for one
 * must never sign with another's key. Nothing in {@link JwsRs256Signer} is shared or global, and
 * these tests exist to keep it that way — a cache, a lazily-initialized default key, or a
 * {@code Security.addProvider} side effect introduced later would fail here rather than in
 * production, where the symptom is an invoice signed by the wrong taxpayer.
 */
class PerOrganizationSignerTest {

    private static final byte[] PAYLOAD = "{\"invoiceNumber\":\"INV-1\"}".getBytes(StandardCharsets.UTF_8);

    @Test
    void twoSignersFromTwoKeystoresEachSignWithTheirOwnKey() throws Exception {
        SigningCredential first = Pkcs12KeyStores.load(KeyStoreFixture.bytes(), KeyStoreFixture.PASSWORD);
        SigningCredential second = Pkcs12KeyStores.load(KeyStoreFixture.secondaryBytes(), KeyStoreFixture.PASSWORD);

        JwsRs256Signer firstSigner = new JwsRs256Signer(first.privateKey(), first.certificateChain());
        JwsRs256Signer secondSigner = new JwsRs256Signer(second.privateKey(), second.certificateChain());

        String firstJws = firstSigner.signCompact(PAYLOAD);
        String secondJws = secondSigner.signCompact(PAYLOAD);

        assertThat(firstJws).isNotEqualTo(secondJws);
        assertThat(verifies(firstJws, publicKeyOf(first))).isTrue();
        assertThat(verifies(secondJws, publicKeyOf(second))).isTrue();
        assertThat(verifies(firstJws, publicKeyOf(second))).isFalse();
        assertThat(verifies(secondJws, publicKeyOf(first))).isFalse();
    }

    @Test
    void buildingAnotherSignerDoesNotDisturbAnExistingOne() throws Exception {
        SigningCredential first = Pkcs12KeyStores.load(KeyStoreFixture.bytes(), KeyStoreFixture.PASSWORD);
        SigningCredential second = Pkcs12KeyStores.load(KeyStoreFixture.secondaryBytes(), KeyStoreFixture.PASSWORD);
        JwsRs256Signer firstSigner = new JwsRs256Signer(first.privateKey(), first.certificateChain());

        String before = firstSigner.signCompact(PAYLOAD);
        new JwsRs256Signer(second.privateKey(), second.certificateChain()).signCompact(PAYLOAD);
        new JwsRs256Signer(second.privateKey()).signCompact(PAYLOAD);
        String after = firstSigner.signCompact(PAYLOAD);

        // RS256 is RSASSA-PKCS1-v1_5: deterministic, so the same signer over the same payload must
        // produce the identical token. Any drift here is shared state, not randomness.
        assertThat(after).isEqualTo(before);
        assertThat(verifies(after, publicKeyOf(first))).isTrue();
    }

    @Test
    void theSignerHoldsNoMutableStaticState() {
        for (Field field : JwsRs256Signer.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                assertThat(Modifier.isFinal(field.getModifiers()))
                        .withFailMessage("JwsRs256Signer.%s is static but not final: per-organization "
                                + "signers must not share state", field.getName())
                        .isTrue();
                assertThat(field.getType())
                        .withFailMessage("JwsRs256Signer.%s is a static field of a type this test cannot "
                                + "vouch for as immutable", field.getName())
                        .isEqualTo(String.class);
            }
        }
    }

    private static PublicKey publicKeyOf(SigningCredential credential) {
        return credential.certificateChain().get(0).getPublicKey();
    }

    private static boolean verifies(String compactJws, PublicKey publicKey) throws Exception {
        String[] parts = compactJws.split("\\.", -1);
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(publicKey);
        verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
        return verifier.verify(Base64.getUrlDecoder().decode(parts[2]));
    }
}
