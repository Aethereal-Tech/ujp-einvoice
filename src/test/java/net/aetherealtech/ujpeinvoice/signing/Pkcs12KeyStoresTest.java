package net.aetherealtech.ujpeinvoice.signing;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
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

    @Test
    void theFileTheByteAndTheStreamFormsYieldTheSameCredential() throws Exception {
        SigningCredential fromPath = Pkcs12KeyStores.load(p12Path(), KeyStoreFixture.PASSWORD);
        SigningCredential fromBytes = Pkcs12KeyStores.load(KeyStoreFixture.bytes(), KeyStoreFixture.PASSWORD);
        SigningCredential fromStream = Pkcs12KeyStores.load(
                new ByteArrayInputStream(KeyStoreFixture.bytes()), KeyStoreFixture.PASSWORD);

        assertThat(fromBytes.privateKey().getEncoded()).isEqualTo(fromPath.privateKey().getEncoded());
        assertThat(fromStream.privateKey().getEncoded()).isEqualTo(fromPath.privateKey().getEncoded());
        assertThat(fromBytes.certificateChain()).isEqualTo(fromPath.certificateChain());
        assertThat(fromStream.certificateChain()).isEqualTo(fromPath.certificateChain());
        assertThat(fromPath.certificateChain().get(0).getSubjectX500Principal().getName())
                .contains("UJP Test Signer");
    }

    @Test
    void findsTheSoleKeyEntryWithoutBeingToldItsAlias() throws Exception {
        SigningCredential credential = Pkcs12KeyStores.load(KeyStoreFixture.bytes(), KeyStoreFixture.PASSWORD);

        assertThat(credential.privateKey().getAlgorithm()).isEqualTo("RSA");
        assertThat(credential.certificateChain()).hasSize(1);
    }

    @Test
    void leavesTheCallersStreamOpenForTheCallerToClose() throws Exception {
        CloseTrackingStream stream = new CloseTrackingStream(KeyStoreFixture.bytes());

        Pkcs12KeyStores.load(stream, KeyStoreFixture.PASSWORD);

        assertThat(stream.closed).isFalse();
    }

    @Test
    void doesNotRetainThePasswordSoTheCallerCanZeroIt() throws Exception {
        char[] password = "changeit".toCharArray();

        SigningCredential credential = Pkcs12KeyStores.load(KeyStoreFixture.bytes(), password);
        Arrays.fill(password, '\0');

        // The key is fully materialized before load returns, so zeroing the caller's array cannot
        // reach back into it — which is what makes handing this method a char[] worth anything.
        assertThat(new JwsRs256Signer(credential.privateKey())
                .signCompact("payload".getBytes(StandardCharsets.UTF_8))).isNotEmpty();
    }

    @Test
    void refusesAWrongPasswordWithoutQuotingIt() throws Exception {
        byte[] p12 = KeyStoreFixture.bytes();

        assertThatThrownBy(() -> Pkcs12KeyStores.load(p12, "hunter2-not-the-password".toCharArray()))
                .isInstanceOf(SigningException.class)
                .hasMessageContaining("PKCS#12")
                .hasMessageContaining("password")
                .hasMessageNotContaining("hunter2-not-the-password");
    }

    @Test
    void refusesTruncatedKeystoreBytes() throws Exception {
        byte[] truncated = Arrays.copyOf(KeyStoreFixture.bytes(), 64);

        assertThatThrownBy(() -> Pkcs12KeyStores.load(truncated, KeyStoreFixture.PASSWORD))
                .isInstanceOf(SigningException.class)
                .hasMessageContaining("truncated")
                .hasCauseInstanceOf(Exception.class);
    }

    @Test
    void refusesATruncatedStreamTheSameWay() throws Exception {
        InputStream truncated = new ByteArrayInputStream(Arrays.copyOf(KeyStoreFixture.bytes(), 64));

        assertThatThrownBy(() -> Pkcs12KeyStores.load(truncated, KeyStoreFixture.PASSWORD))
                .isInstanceOf(SigningException.class)
                .hasMessageContaining("PKCS#12");
    }

    @Test
    void refusesAKeystoreHoldingNoKeyAtAllAndSaysHowMany() throws Exception {
        byte[] empty = KeyStoreFixture.withoutAnyKeyEntry();

        assertThatThrownBy(() -> Pkcs12KeyStores.load(empty, KeyStoreFixture.PASSWORD))
                .isInstanceOf(SigningException.class)
                .hasMessageContaining("exactly one private key entry")
                .hasMessageContaining("found 0");
    }

    @Test
    void refusesAKeystoreHoldingTwoKeysAndNamesThem() throws Exception {
        byte[] two = KeyStoreFixture.withTwoKeyEntries();

        assertThatThrownBy(() -> Pkcs12KeyStores.load(two, KeyStoreFixture.PASSWORD))
                .isInstanceOf(SigningException.class)
                .hasMessageContaining("found 2")
                .hasMessageContaining("first")
                .hasMessageContaining("second");
    }

    @Test
    void refusesAKeyEntryThatIsNotAPrivateKey() throws Exception {
        byte[] secretOnly = KeyStoreFixture.withOnlyASecretKeyEntry();

        assertThatThrownBy(() -> Pkcs12KeyStores.load(secretOnly, KeyStoreFixture.PASSWORD))
                .isInstanceOf(SigningException.class)
                .hasMessageContaining("not a private key entry");
    }

    @Test
    void refusesAKeyProtectedByADifferentPassword() throws Exception {
        byte[] p12 = KeyStoreFixture.withKeyUnderADifferentPassword();

        assertThatThrownBy(() -> Pkcs12KeyStores.load(p12, KeyStoreFixture.PASSWORD))
                .isInstanceOf(SigningException.class)
                .hasMessageContaining("Could not recover the key")
                .hasMessageNotContaining("changeit");
    }

    @Test
    void refusesAStreamThatFailsMidRead() {
        InputStream failing = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("the object store dropped the connection");
            }
        };

        assertThatThrownBy(() -> Pkcs12KeyStores.load(failing, KeyStoreFixture.PASSWORD))
                .isInstanceOf(SigningException.class)
                .hasMessageContaining("the object store dropped the connection");
    }

    @Test
    void refusesAMissingFileByName() {
        Path missing = Path.of("no-such-directory", "no-such-keystore.p12");

        assertThatThrownBy(() -> Pkcs12KeyStores.load(missing, KeyStoreFixture.PASSWORD))
                .isInstanceOf(SigningException.class)
                .hasMessageContaining("no-such-keystore.p12");
    }

    private Path p12Path() throws URISyntaxException {
        return KeyStoreFixture.path();
    }

    /** An {@link InputStream} that remembers being closed, to prove this library never closes it. */
    private static final class CloseTrackingStream extends ByteArrayInputStream {
        private boolean closed;

        private CloseTrackingStream(byte[] bytes) {
            super(bytes);
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
