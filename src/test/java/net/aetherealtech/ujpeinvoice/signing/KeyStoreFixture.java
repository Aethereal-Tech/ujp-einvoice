package net.aetherealtech.ujpeinvoice.signing;

import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Loads the throwaway self-signed test keystore at {@code src/test/resources/test-signing-key.p12}
 * (alias {@code ujp-test}, password {@code changeit} — a fixture key, not a secret). Generated once
 * with {@code keytool -genkeypair} and committed; nothing about it is sensitive.
 *
 * <p>Also assembles the malformed keystores the enumerating {@code load} methods must refuse. Those
 * are built in memory from the committed fixture's own key rather than generated fresh, so a test
 * that asserts a refusal costs no RSA key generation and cannot vary between runs.
 */
final class KeyStoreFixture {

    static final char[] PASSWORD = "changeit".toCharArray();
    static final String ALIAS = "ujp-test";

    private final PrivateKey privateKey;
    private final List<X509Certificate> certificateChain;

    private KeyStoreFixture(PrivateKey privateKey, List<X509Certificate> certificateChain) {
        this.privateKey = privateKey;
        this.certificateChain = certificateChain;
    }

    static KeyStoreFixture load() throws Exception {
        Path p12 = path();
        PrivateKey key = Pkcs12KeyStores.loadPrivateKey(p12, PASSWORD, ALIAS);
        List<X509Certificate> chain = Pkcs12KeyStores.loadCertificateChain(p12, PASSWORD, ALIAS);
        return new KeyStoreFixture(key, chain);
    }

    PrivateKey privateKey() {
        return privateKey;
    }

    List<X509Certificate> certificateChain() {
        return certificateChain;
    }

    static Path path() throws URISyntaxException {
        return resourcePath("/test-signing-key.p12");
    }

    static byte[] bytes() throws Exception {
        return Files.readAllBytes(path());
    }

    /**
     * A second, unrelated keystore ({@code test-signing-key-2.p12}, alias {@code ujp-test-2}, same
     * password) — one organization's credential standing in for another's, so a test can prove two
     * signers in one JVM keep their own keys.
     */
    static byte[] secondaryBytes() throws Exception {
        return Files.readAllBytes(resourcePath("/test-signing-key-2.p12"));
    }

    /** A PKCS#12 keystore holding no entries at all. */
    static byte[] withoutAnyKeyEntry() throws Exception {
        return serialize(emptyKeyStore());
    }

    /** The fixture's own key and chain, stored twice under two aliases. */
    static byte[] withTwoKeyEntries() throws Exception {
        KeyStore source = openFixture();
        KeyStore target = emptyKeyStore();
        Certificate[] chain = source.getCertificateChain(ALIAS);
        Key key = source.getKey(ALIAS, PASSWORD);
        target.setKeyEntry("first", key, PASSWORD, chain);
        target.setKeyEntry("second", key, PASSWORD, chain);
        return serialize(target);
    }

    /**
     * The fixture's key, protected by a password other than the one that opens the keystore — legal
     * in PKCS#12, and indistinguishable from a well-formed store until the key itself is read.
     */
    static byte[] withKeyUnderADifferentPassword() throws Exception {
        KeyStore source = openFixture();
        KeyStore target = emptyKeyStore();
        target.setKeyEntry(ALIAS, source.getKey(ALIAS, PASSWORD), "a-different-password".toCharArray(),
                source.getCertificateChain(ALIAS));
        return serialize(target);
    }

    /**
     * A PKCS#12 keystore whose one key entry is a symmetric key. {@code KeyStore#isKeyEntry} says
     * yes to it, so enumeration finds it and only the read that follows can tell it is unusable.
     */
    static byte[] withOnlyASecretKeyEntry() throws Exception {
        KeyStore keyStore = emptyKeyStore();
        keyStore.setEntry("secret",
                new KeyStore.SecretKeyEntry(new SecretKeySpec(new byte[16], "AES")),
                new KeyStore.PasswordProtection(PASSWORD));
        return serialize(keyStore);
    }

    private static KeyStore openFixture() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(new ByteArrayInputStream(bytes()), PASSWORD);
        return keyStore;
    }

    private static KeyStore emptyKeyStore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, PASSWORD);
        return keyStore;
    }

    private static byte[] serialize(KeyStore keyStore) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        keyStore.store(out, PASSWORD);
        return out.toByteArray();
    }

    private static Path resourcePath(String resource) throws URISyntaxException {
        return Path.of(KeyStoreFixture.class.getResource(resource).toURI());
    }
}
