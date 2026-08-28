package net.aetherealtech.ujpeinvoice.internal.json;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A JSON object. Member order is preserved (a {@link LinkedHashMap} underneath), because
 * {@code UjpJsonSerializer}'s golden-file tests compare the written text field-for-field in a fixed
 * order, not just structurally.
 */
public record JsonObject(Map<String, JsonValue> members) implements JsonValue {

    public JsonObject {
        members = Collections.unmodifiableMap(new LinkedHashMap<>(members));
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The raw member, or empty if absent. Never throws for a missing key. */
    public Optional<JsonValue> get(String key) {
        return Optional.ofNullable(members.get(key));
    }

    /** The member as a string. Throws {@link JsonException} if absent or not a string. */
    public String getString(String key) {
        return required(key, JsonString.class).value();
    }

    /** The member as a {@link BigDecimal}. Throws {@link JsonException} if absent or not a number. */
    public BigDecimal getNumber(String key) {
        return required(key, JsonNumber.class).value();
    }

    /** The member as a nested object. Throws {@link JsonException} if absent or not an object. */
    public JsonObject getObject(String key) {
        return required(key, JsonObject.class);
    }

    /** The member as an array. Throws {@link JsonException} if absent or not an array. */
    public JsonArray getArray(String key) {
        return required(key, JsonArray.class);
    }

    /** The member as a string, or {@code null} if the key is absent or its value is JSON null. */
    public String getOptionalString(String key) {
        JsonValue value = members.get(key);
        if (value == null || value instanceof JsonNull) {
            return null;
        }
        if (value instanceof JsonString s) {
            return s.value();
        }
        throw new JsonException("Field '" + key + "' is not a string: " + value);
    }

    private <T extends JsonValue> T required(String key, Class<T> type) {
        JsonValue value = members.get(key);
        if (value == null) {
            throw new JsonException("Missing required field: '" + key + "'");
        }
        if (!type.isInstance(value)) {
            throw new JsonException("Field '" + key + "' is not a " + type.getSimpleName() + ": " + value);
        }
        return type.cast(value);
    }

    /** Builds a {@link JsonObject} field by field, in the order fields are added. */
    public static final class Builder {
        private final Map<String, JsonValue> members = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder put(String key, JsonValue value) {
            members.put(key, value == null ? JsonNull.INSTANCE : value);
            return this;
        }

        public Builder put(String key, String value) {
            return put(key, value == null ? JsonNull.INSTANCE : new JsonString(value));
        }

        public Builder put(String key, BigDecimal value) {
            return put(key, value == null ? JsonNull.INSTANCE : new JsonNumber(value));
        }

        public Builder put(String key, boolean value) {
            return put(key, JsonBoolean.of(value));
        }

        public JsonObject build() {
            return new JsonObject(members);
        }
    }
}
