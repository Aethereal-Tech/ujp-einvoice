package net.aetherealtech.ujpeinvoice.internal.json;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A recursive-descent parser for the RFC 8259 subset this library ever needs to read: response
 * bodies from the UJP gateway. It reads whatever is well-formed; it does not attempt to recover
 * from what is not, or to accept trailing content, comments, or any other non-standard extension.
 */
public final class JsonParser {

    private final String text;
    private int pos;

    private JsonParser(String text) {
        this.text = text;
    }

    public static JsonValue parse(String text) {
        JsonParser parser = new JsonParser(text);
        parser.skipWhitespace();
        JsonValue value = parser.parseValue();
        parser.skipWhitespace();
        if (parser.pos != text.length()) {
            throw new JsonException("Unexpected trailing content at position " + parser.pos);
        }
        return value;
    }

    private JsonValue parseValue() {
        if (pos >= text.length()) {
            throw new JsonException("Unexpected end of input");
        }
        char c = text.charAt(pos);
        return switch (c) {
            case '{' -> parseObject();
            case '[' -> parseArray();
            case '"' -> new JsonString(parseStringLiteral());
            case 't', 'f' -> parseBoolean();
            case 'n' -> parseNull();
            default -> parseNumber();
        };
    }

    private JsonObject parseObject() {
        expect('{');
        Map<String, JsonValue> members = new LinkedHashMap<>();
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            return new JsonObject(members);
        }
        while (true) {
            skipWhitespace();
            String key = parseStringLiteral();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            members.put(key, parseValue());
            skipWhitespace();
            char c = next();
            if (c == ',') {
                continue;
            }
            if (c == '}') {
                break;
            }
            throw new JsonException("Expected ',' or '}' at position " + (pos - 1));
        }
        return new JsonObject(members);
    }

    private JsonArray parseArray() {
        expect('[');
        List<JsonValue> items = new ArrayList<>();
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return new JsonArray(items);
        }
        while (true) {
            skipWhitespace();
            items.add(parseValue());
            skipWhitespace();
            char c = next();
            if (c == ',') {
                continue;
            }
            if (c == ']') {
                break;
            }
            throw new JsonException("Expected ',' or ']' at position " + (pos - 1));
        }
        return new JsonArray(items);
    }

    private String parseStringLiteral() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (pos >= text.length()) {
                throw new JsonException("Unterminated string literal");
            }
            char c = text.charAt(pos++);
            if (c == '"') {
                break;
            }
            if (c == '\\') {
                if (pos >= text.length()) {
                    throw new JsonException("Unterminated escape sequence");
                }
                char escaped = text.charAt(pos++);
                switch (escaped) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (pos + 4 > text.length()) {
                            throw new JsonException("Truncated \\u escape");
                        }
                        String hex = text.substring(pos, pos + 4);
                        pos += 4;
                        sb.append((char) Integer.parseInt(hex, 16));
                    }
                    default -> throw new JsonException("Invalid escape sequence: \\" + escaped);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private JsonValue parseNumber() {
        int start = pos;
        if (peek() == '-') {
            pos++;
        }
        while (pos < text.length() && Character.isDigit(text.charAt(pos))) {
            pos++;
        }
        if (pos < text.length() && text.charAt(pos) == '.') {
            pos++;
            while (pos < text.length() && Character.isDigit(text.charAt(pos))) {
                pos++;
            }
        }
        if (pos < text.length() && (text.charAt(pos) == 'e' || text.charAt(pos) == 'E')) {
            pos++;
            if (pos < text.length() && (text.charAt(pos) == '+' || text.charAt(pos) == '-')) {
                pos++;
            }
            while (pos < text.length() && Character.isDigit(text.charAt(pos))) {
                pos++;
            }
        }
        if (pos == start) {
            throw new JsonException("Expected a value at position " + pos);
        }
        String literal = text.substring(start, pos);
        try {
            return new JsonNumber(new BigDecimal(literal));
        } catch (NumberFormatException e) {
            throw new JsonException("Invalid number literal: " + literal);
        }
    }

    private JsonValue parseBoolean() {
        if (text.startsWith("true", pos)) {
            pos += 4;
            return JsonBoolean.TRUE;
        }
        if (text.startsWith("false", pos)) {
            pos += 5;
            return JsonBoolean.FALSE;
        }
        throw new JsonException("Expected 'true' or 'false' at position " + pos);
    }

    private JsonValue parseNull() {
        if (text.startsWith("null", pos)) {
            pos += 4;
            return JsonNull.INSTANCE;
        }
        throw new JsonException("Expected 'null' at position " + pos);
    }

    private void skipWhitespace() {
        while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
            pos++;
        }
    }

    private char peek() {
        if (pos >= text.length()) {
            throw new JsonException("Unexpected end of input");
        }
        return text.charAt(pos);
    }

    private char next() {
        if (pos >= text.length()) {
            throw new JsonException("Unexpected end of input");
        }
        return text.charAt(pos++);
    }

    private void expect(char expected) {
        char actual = next();
        if (actual != expected) {
            throw new JsonException("Expected '" + expected + "' but found '" + actual + "' at position " + (pos - 1));
        }
    }
}
