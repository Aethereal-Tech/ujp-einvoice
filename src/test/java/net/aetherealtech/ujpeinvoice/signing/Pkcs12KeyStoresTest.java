package net.aetherealtech.ujpeinvoice.signing;

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Pkcs12KeyStoresTest {

    @Test
    void loadsThePrivateKeyFromAPkcs12File() throws Exception {
        PrivateKey key = Pkcs12KeyStores.loadPrivateKey(p12Path(), KeyStoreFixture.PASSWORD, KeyStoreFixture.ALIAS);

        assertThat(key.getAlgorithm()).isEqualTo("RSA");
    }

    @Test
    void loadsTheCertificateChainFromAPkcs12File() throws Exception {
        List<X509Certificate> chain = Pkcs12KeyStores.loadCertificateChain(
                p12Path(), KeyStoreFixture.PASSWORD, KeyStoreFixture.ALIAS);

        assertThat(chain).isNotEmpty();
        assertThat(chain.get(0).getSubjectX500Principal().getName()).contains("UJP Test Signer");
    }

    @Test
    void rejectsAnUnknownAlias() {
        assertThatThrownBy(() -> Pkcs12KeyStores.loadPrivateKey(p12Path(), KeyStoreFixture.PASSWORD, "no-such-alias"))
                .isInstanceOf(GeneralSecurityException.class);
    }

    @Test
    void rejectsAWrongPassword() {
        // KeyStore#load(InputStream, char[]) reports a bad PKCS#12 password as IOException, not a
        // GeneralSecurityException — that is the JDK's own contract, which loadPrivateKey passes
        // through rather than papering over.
        assertThatThrownBy(() -> Pkcs12KeyStores.loadPrivateKey(p12Path(), "wrong-password".toCharArray(), KeyStoreFixture.ALIAS))
                .isInstanceOf(java.io.IOException.class);
    }

    @Test
    void rejectsAnUnknownAliasForCertificateChain() {
        assertThatThrownBy(() -> Pkcs12KeyStores.loadCertificateChain(p12Path(), KeyStoreFixture.PASSWORD, "no-such-alias"))
                .isInstanceOf(GeneralSecurityException.class);
    }

    private Path p12Path() throws URISyntaxException {
        return Path.of(getClass().getResource("/test-signing-key.p12").toURI());
    }
}
