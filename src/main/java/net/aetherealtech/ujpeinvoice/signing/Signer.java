package net.aetherealtech.ujpeinvoice.signing;

/**
 * Produces a compact JWS (RFC 7515) over a payload. One implementation ships with this library,
 * {@link JwsRs256Signer}, using RS256 — the algorithm hands-on integrator accounts describe the UJP
 * gateway as requiring. This interface exists so a different algorithm, or a hardware-backed signer
 * that never exposes the private key to this process at all, is a new implementation rather than a
 * change to anything in {@code net.aetherealtech.ujpeinvoice.transport}.
 */
public interface Signer {

    /**
     * Signs {@code payload} and returns the compact serialization: base64url(header) + "." +
     * base64url(payload) + "." + base64url(signature).
     */
    String signCompact(byte[] payload);
}
