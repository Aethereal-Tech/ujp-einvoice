package net.aetherealtech.ujpeinvoice.signing;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * One signing identity as it came out of one keystore: the private key, and the certificate chain
 * that belongs to it.
 *
 * <p>The two are returned together because they are read in a single pass, under a single password.
 * A caller holding many organizations' credentials at once — one keystore each — would otherwise
 * have to parse and unlock the same bytes twice to build a {@link JwsRs256Signer} carrying an
 * {@code x5c} header.
 *
 * <p>The chain may be empty: a keystore can hold a key with no certificate, and a JWS signed
 * without {@code x5c} is still a valid JWS (see {@link JwsRs256Signer}).
 */
public record SigningCredential(PrivateKey privateKey, List<X509Certificate> certificateChain) {

    public SigningCredential {
        if (privateKey == null) {
            throw new NullPointerException("privateKey must not be null");
        }
        certificateChain = List.copyOf(certificateChain);
    }
}
