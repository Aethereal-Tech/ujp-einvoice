package net.aetherealtech.ujpeinvoice.transport;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.aetherealtech.ujpeinvoice.internal.json.JsonArray;
import net.aetherealtech.ujpeinvoice.internal.json.JsonObject;
import net.aetherealtech.ujpeinvoice.internal.json.JsonParser;
import net.aetherealtech.ujpeinvoice.internal.json.JsonString;
import net.aetherealtech.ujpeinvoice.internal.json.JsonValue;

/**
 * The {@code errorStatus} member of an {@code einvoice_api} response: {@code ErrorStatusBean} in
 * {@code openapi-2026-10-20.md} (the gateway's published API description, fetched 2026-10-20), carried by
 * every response as {@code GenericAPIResponse{success, createdDate, errorStatus}}.
 *
 * <p>All three members are kept as served. {@code errorCode} is a string, not a {@link UjpErrorCode}: the
 * description's enum lists 72 codes, 17 of which D5 does not list under eInvoiceApi, and a code the
 * catalogue does not know must survive the round trip rather than be refused. Resolve it with
 * {@link UjpErrorCodes#lookup}. {@code errorPropertyMap} maps a property name to its messages, and is never
 * {@code null}.
 *
 * <p>The JSON Receiver's own error body is not described by any obtained document; this type makes no claim
 * about it.
 */
public record ErrorStatus(String errorCode, String errorMessage, Map<String, List<String>> errorPropertyMap) {

    public ErrorStatus {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        if (errorPropertyMap != null) {
            errorPropertyMap.forEach((property, messages) -> copy.put(property, List.copyOf(messages)));
        }
        errorPropertyMap = Collections.unmodifiableMap(copy);
    }

    /**
     * Reads the error of a response body, or empty if the body is not a JSON object or carries neither shape.
     * Two shapes are read, because the API description itself declares both: the {@code errorStatus} member of a
     * {@code GenericAPIResponse} (the shape its examples show on every operation), and a bare
     * {@code ErrorStatusBean} at the top level, which {@code current-status} declares for its 400 and 500 responses. A body that is not JSON, or whose {@code errorStatus} is not
     * the documented shape, is empty rather than an error: this is for enriching a failure that is already being
     * reported.
     */
    public static Optional<ErrorStatus> fromResponseBody(String body) {
        if (body == null) {
            return Optional.empty();
        }
        try {
            if (JsonParser.parse(body) instanceof JsonObject response) {
                if (response.get("errorStatus").orElse(null) instanceof JsonObject status) {
                    return Optional.of(fromJson(status));
                }
                if (response.get("errorCode").orElse(null) instanceof JsonString) {
                    return Optional.of(fromJson(response));
                }
            }
        } catch (RuntimeException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private static ErrorStatus fromJson(JsonObject status) {
        Map<String, List<String>> properties = new LinkedHashMap<>();
        if (status.get("errorPropertyMap").orElse(null) instanceof JsonObject map) {
            map.members().forEach((property, messages) -> properties.put(property, messagesOf(messages)));
        }
        return new ErrorStatus(status.getOptionalString("errorCode"), status.getOptionalString("errorMessage"),
                properties);
    }

    private static List<String> messagesOf(JsonValue messages) {
        if (messages instanceof JsonArray array) {
            return array.items().stream()
                    .filter(JsonString.class::isInstance)
                    .map(item -> ((JsonString) item).value())
                    .toList();
        }
        return List.of();
    }
}
