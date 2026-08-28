package net.aetherealtech.ujpeinvoice.signing;

import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Loads a private key directly off a PKCS#11 hardware token — the USB qualified-certificate tokens
 * (issued by bodies such as KIBS or Telekom) that integrator accounts describe UJP as requiring for
 * cert-based auth, used without ever exporting the key from the token.
 *
 * <p>This pathway is documented, not exercised by this library's own test suite: it needs a
 * physical token and a vendor-supplied native PKCS#11 module, neither of which a CI runner has. It
 * is excluded from this project's JaCoCo coverage gate for that reason (see {@code pom.xml}) — that
 * is a statement about what CI can reach, not about this code's importance.
 */
public final class Pkcs11KeyStores {

    private Pkcs11KeyStores() {
    }

    /**
     * The alias's private key on a PKCS#11 token, configured by a SunPKCS11 config file pointed at
     * the token vendor's native module. {@code pin} is the token PIN — PKCS#11 tokens have no
     * separate per-key password, only the token PIN.
     *
     * <p>Typical config file, pointed at the vendor module for the KIBS or Telekom token:
     * <pre>{@code
     * name = UjpToken
     * library = /usr/lib/pkcs11/vendor-pkcs11.so
     * }</pre>
     */
    public static PrivateKey loadPrivateKey(Path pkcs11ConfigFile, char[] pin, String alias)
            throws GeneralSecurityException, IOException {
        return KeyStoreEntries.privateKeyEntry(open(pkcs11ConfigFile, pin), alias, pin);
    }

    /** The alias's full certificate chain on a PKCS#11 token, for use as a JWS {@code x5c} header. */
    public static List<X509Certificate> loadCertificateChain(Path pkcs11ConfigFile, char[] pin, String alias)
            throws GeneralSecurityException, IOException {
        return KeyStoreEntries.certificateChain(open(pkcs11ConfigFile, pin), alias);
    }

    private static KeyStore open(Path pkcs11ConfigFile, char[] pin) throws GeneralSecurityException, IOException {
        Provider base = Security.getProvider("SunPKCS11");
        if (base == null) {
            throw new IllegalStateException(
                    "The SunPKCS11 security provider is not available on this JDK/platform");
        }
        Provider configured = base.configure(pkcs11ConfigFile.toString());
        Security.addProvider(configured);
        KeyStore keyStore = KeyStore.getInstance("PKCS11", configured);
        keyStore.load(null, pin);
        return keyStore;
    }
}
