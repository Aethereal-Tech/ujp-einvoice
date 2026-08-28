package net.aetherealtech.ujpeinvoice.internal.json;

public record JsonString(String value) implements JsonValue {

    public JsonString {
        if (value == null) {
            throw new NullPointerException("JsonString value must not be null; use JsonNull.INSTANCE");
        }
    }
}
