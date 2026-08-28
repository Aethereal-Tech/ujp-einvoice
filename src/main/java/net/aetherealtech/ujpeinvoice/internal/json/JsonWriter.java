package net.aetherealtech.ujpeinvoice.internal.json;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Writes a {@link JsonValue} tree as compact JSON text (no insignificant whitespace), per RFC 8259
 * section 7's escaping rules. Numbers are written via {@link BigDecimal#toPlainString()} — never
 * {@code toString()} — so a value never comes out in scientific notation.
 */
public final class JsonWriter {

    private JsonWriter() {
    }

    public static String write(JsonValue value) {
        StringBuilder out = new StringBuilder();
        writeValue(value, out);
        return out.toString();
    }

    private static void writeValue(JsonValue value, StringBuilder out) {
        switch (value) {
            case JsonObject object -> writeObject(object, out);
            case JsonArray array -> writeArray(array, out);
            case JsonString string -> writeString(string.value(), out);
            case JsonNumber number -> out.append(number.value().toPlainString());
            case JsonBoolean bool -> out.append(bool.value());
            case JsonNull ignored -> out.append("null");
        }
    }

    private static void writeObject(JsonObject object, StringBuilder out) {
        out.append('{');
        boolean first = true;
        for (Map.Entry<String, JsonValue> entry : object.members().entrySet()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            writeString(entry.getKey(), out);
            out.append(':');
            writeValue(entry.getValue(), out);
        }
        out.append('}');
    }

    private static void writeArray(JsonArray array, StringBuilder out) {
        out.append('[');
        boolean first = true;
        for (JsonValue item : array.items()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            writeValue(item, out);
        }
        out.append(']');
    }

    private static void writeString(String value, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }
}
