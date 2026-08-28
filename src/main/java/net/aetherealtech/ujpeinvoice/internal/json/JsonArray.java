package net.aetherealtech.ujpeinvoice.internal.json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record JsonArray(List<JsonValue> items) implements JsonValue {

    public JsonArray {
        items = Collections.unmodifiableList(new ArrayList<>(items));
    }

    public static JsonArray of(List<? extends JsonValue> items) {
        return new JsonArray(List.copyOf(items));
    }
}
