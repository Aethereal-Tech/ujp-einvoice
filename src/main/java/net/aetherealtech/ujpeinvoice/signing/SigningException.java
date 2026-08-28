package net.aetherealtech.ujpeinvoice.signing;

/** Wraps a checked {@code java.security} failure (bad key, unavailable algorithm, ...) encountered while signing. */
public final class SigningException extends RuntimeException {

    public SigningException(String message, Throwable cause) {
        super(message, cause);
    }
}
