package net.aetherealtech.ujpeinvoice.signing;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Loads a private key (and its certificate chain) from a {@code .p12}/{@code .pfx} file — the form
 * a UJP e-Faktura signing credential most commonly arrives in, e.g. exported from a qualified
 * certificate for use without the issuing USB token. See {@link Pkcs11KeyStores} for loading
 * directly off the token instead.
 */
public final class Pkcs12KeyStores {

    private Pkcs12KeyStores() {
    }

    /** The alias's private key from a PKCS#12 file. */
    public static PrivateKey loadPrivateKey(Path pkcs12File, char[] password, String alias)
            throws GeneralSecurityException, IOException {
        return KeyStoreEntries.privateKeyEntry(open(pkcs12File, password), alias, password);
    }

    /** The alias's full certificate chain from a PKCS#12 file, for use as a JWS {@code x5c} header. */
    public static List<X509Certificate> loadCertificateChain(Path pkcs12File, char[] password, String alias)
            throws GeneralSecurityException, IOException {
        return KeyStoreEntries.certificateChain(open(pkcs12File, password), alias);
    }

    private static KeyStore open(Path pkcs12File, char[] password) throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(pkcs12File)) {
            keyStore.load(in, password);
        }
        return keyStore;
    }
}
