package net.aetherealtech.ujpeinvoice.signing;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads a private key (and its certificate chain) from a {@code .p12}/{@code .pfx} keystore — the
 * form a UJP e-Faktura signing credential most commonly arrives in, e.g. exported from a qualified
 * certificate for use without the issuing USB token. See {@link Pkcs11KeyStores} for loading
 * directly off the token instead.
 *
 * <h2>Bytes, not paths</h2>
 * <p>{@link #load(byte[], char[])} is the entry point to reach for. A caller holding one keystore
 * per organization typically keeps them encrypted in object storage rather than on a filesystem, so
 * the byte and stream forms parse what they are handed, in memory: nothing here writes a temporary
 * file, and a decrypted keystore never touches disk. {@link #load(InputStream, char[])} reads its
 * stream to the end and does <strong>not</strong> close it — the caller owns what it opened.
 *
 * <p>The password is taken as a {@code char[]} and is never copied into a {@code String}: nothing
 * here outlives the call, so a caller may zero the array as soon as the method returns. It never
 * appears in an exception message either.
 *
 * <h2>Enumerated, not addressed by alias</h2>
 * <p>The {@code load} methods expect the keystore to hold exactly one private key entry and find it
 * themselves; a store holding none, or several, is refused by name rather than resolved by guessing
 * which one was meant. That is the shape a per-organization signing credential actually has, and it
 * means a caller never has to store an alias alongside the bytes. The alias-addressed methods
 * remain for a keystore that genuinely holds several identities.
 */
public final class Pkcs12KeyStores {

    private Pkcs12KeyStores() {
    }

    /**
     * The sole private key entry of an in-memory PKCS#12 keystore, with its certificate chain.
     *
     * @throws SigningException if the bytes are not a readable PKCS#12 keystore under this password,
     *     or if they hold anything other than exactly one private key entry
     */
    public static SigningCredential load(byte[] pkcs12, char[] password) {
        return credential(open(pkcs12, password), password);
    }

    /**
     * The sole private key entry of a PKCS#12 keystore read from {@code pkcs12}, with its
     * certificate chain. The stream is read to the end and left open for its owner to close.
     */
    public static SigningCredential load(InputStream pkcs12, char[] password) {
        return load(readFully(pkcs12), password);
    }

    /** The sole private key entry of a PKCS#12 file, with its certificate chain. */
    public static SigningCredential load(Path pkcs12File, char[] password) {
        return load(readFully(pkcs12File), password);
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

    private static SigningCredential credential(KeyStore keyStore, char[] password) {
        try {
            String alias = soleKeyEntryAlias(keyStore);
            return new SigningCredential(KeyStoreEntries.privateKeyEntry(keyStore, alias, password),
                    KeyStoreEntries.certificateChain(keyStore, alias));
        } catch (GeneralSecurityException e) {
            throw new SigningException("The PKCS#12 keystore's key entry cannot be used for signing: "
                    + e.getMessage(), e);
        }
    }

    private static String soleKeyEntryAlias(KeyStore keyStore) throws GeneralSecurityException {
        List<String> keyAliases = new ArrayList<>();
        for (String alias : Collections.list(keyStore.aliases())) {
            if (keyStore.isKeyEntry(alias)) {
                keyAliases.add(alias);
            }
        }
        if (keyAliases.size() != 1) {
            throw new SigningException("Expected exactly one private key entry in the PKCS#12 keystore, found "
                    + keyAliases.size() + ": " + keyAliases);
        }
        return keyAliases.getFirst();
    }

    private static KeyStore open(byte[] pkcs12, char[] password) {
        try (InputStream in = new ByteArrayInputStream(pkcs12)) {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(in, password);
            return keyStore;
        } catch (IOException | GeneralSecurityException e) {
            // The JDK reports a wrong PKCS#12 password as an IOException ("keystore password was
            // incorrect"), not a GeneralSecurityException, and truncated or non-PKCS#12 bytes arrive
            // here too as a DER parse failure. The cause's own message tells them apart; none of
            // them can carry the password, which is why it is safe to quote here.
            throw new SigningException("Could not open the PKCS#12 keystore — the password may be wrong, or the "
                    + "bytes may be truncated or not a PKCS#12 keystore: " + e.getMessage(), e);
        }
    }

    private static KeyStore open(Path pkcs12File, char[] password) throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(pkcs12File)) {
            keyStore.load(in, password);
        }
        return keyStore;
    }

    private static byte[] readFully(InputStream pkcs12) {
        try {
            return pkcs12.readAllBytes();
        } catch (IOException e) {
            throw new SigningException("Could not read the PKCS#12 keystore from the supplied stream: "
                    + e.getMessage(), e);
        }
    }

    private static byte[] readFully(Path pkcs12File) {
        try {
            return Files.readAllBytes(pkcs12File);
        } catch (IOException e) {
            throw new SigningException("Could not read the PKCS#12 keystore file: " + e.getMessage(), e);
        }
    }
}
