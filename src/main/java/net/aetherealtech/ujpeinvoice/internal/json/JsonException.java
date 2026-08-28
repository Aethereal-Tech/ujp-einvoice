package net.aetherealtech.ujpeinvoice.internal.json;

/** Malformed JSON text, or a well-formed document that does not have the shape a caller asked for. */
public final class JsonException extends RuntimeException {

    public JsonException(String message) {
        super(message);
    }
}
