package net.aetherealtech.ujpeinvoice.signing;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Loads the throwaway self-signed test keystore at {@code src/test/resources/test-signing-key.p12}
 * (alias {@code ujp-test}, password {@code changeit} — a fixture key, not a secret). Generated once
 * with {@code keytool -genkeypair} and committed; nothing about it is sensitive.
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
        Path p12 = resourcePath("/test-signing-key.p12");
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

    private static Path resourcePath(String resource) throws URISyntaxException {
        return Path.of(KeyStoreFixture.class.getResource(resource).toURI());
    }
}
