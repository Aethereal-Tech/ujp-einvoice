package net.aetherealtech.ujpeinvoice.signing;

/** Wraps a checked {@code java.security} failure (bad key, unavailable algorithm, ...) encountered while signing. */
public final class SigningException extends RuntimeException {

    public SigningException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * A refusal this library raises itself, with no underlying exception to carry — a keystore that
     * holds no private key, or several, which no {@code java.security} call reports as a failure.
     */
    public SigningException(String message) {
        super(message);
    }
}
