package net.aetherealtech.ujpeinvoice.signing;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/** The alias-lookup logic shared by {@link Pkcs12KeyStores} and {@link Pkcs11KeyStores}. */
final class KeyStoreEntries {

    private KeyStoreEntries() {
    }

    static PrivateKey privateKeyEntry(KeyStore keyStore, String alias, char[] password) throws GeneralSecurityException {
        Key key;
        try {
            key = keyStore.getKey(alias, password);
        } catch (UnrecoverableKeyException e) {
            throw new KeyStoreException("Could not recover the key for alias '" + alias
                    + "' — check the password/PIN", e);
        }
        if (key == null) {
            throw new KeyStoreException("No such alias in keystore: '" + alias + "'");
        }
        if (!(key instanceof PrivateKey privateKey)) {
            throw new KeyStoreException("Alias '" + alias + "' is not a private key entry");
        }
        return privateKey;
    }

    static List<X509Certificate> certificateChain(KeyStore keyStore, String alias) throws GeneralSecurityException {
        Certificate[] chain = keyStore.getCertificateChain(alias);
        if (chain == null) {
            throw new KeyStoreException("No certificate chain for alias: '" + alias + "'");
        }
        List<X509Certificate> result = new ArrayList<>(chain.length);
        for (Certificate certificate : chain) {
            if (!(certificate instanceof X509Certificate x509)) {
                throw new KeyStoreException("Non-X.509 certificate found in chain for alias: '" + alias + "'");
            }
            result.add(x509);
        }
        return result;
    }
}
