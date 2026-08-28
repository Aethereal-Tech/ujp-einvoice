package net.aetherealtech.ujpeinvoice.internal.json;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonRoundTripTest {

    @Test
    void writesAndReparsesAnObject() {
        JsonObject original = JsonObject.builder()
                .put("name", "Skopje \"Center\"")
                .put("amount", new BigDecimal("236.00"))
                .put("active", true)
                .put("nested", JsonObject.builder().put("inner", "value").build())
                .put("tags", JsonArray.of(java.util.List.of(new JsonString("a"), new JsonString("b"))))
                .put("missing", (JsonValue) null)
                .build();

        String written = JsonWriter.write(original);
        JsonValue reparsed = JsonParser.parse(written);

        assertThat(reparsed).isInstanceOf(JsonObject.class);
        JsonObject object = (JsonObject) reparsed;
        assertThat(object.getString("name")).isEqualTo("Skopje \"Center\"");
        assertThat(object.getNumber("amount")).isEqualByComparingTo("236.00");
        assertThat(object.getObject("nested").getString("inner")).isEqualTo("value");
        assertThat(object.getArray("tags").items()).hasSize(2);
        assertThat(object.get("missing")).contains(JsonNull.INSTANCE);
    }

    @Test
    void escapesControlCharactersAndBackslashes() {
        String written = JsonWriter.write(new JsonString("line1\nline2\ttab\\backslash"));
        JsonValue reparsed = JsonParser.parse(written);

        assertThat(((JsonString) reparsed).value()).isEqualTo("line1\nline2\ttab\\backslash");
    }

    @Test
    void writesNumbersInPlainNotationNeverScientific() {
        String written = JsonWriter.write(new JsonNumber(new BigDecimal("0.00000001")));
        assertThat(written).doesNotContain("E").doesNotContain("e");
    }

    @Test
    void parsesAllPrimitiveLiterals() {
        assertThat(JsonParser.parse("true")).isEqualTo(JsonBoolean.TRUE);
        assertThat(JsonParser.parse("false")).isEqualTo(JsonBoolean.FALSE);
        assertThat(JsonParser.parse("null")).isEqualTo(JsonNull.INSTANCE);
        assertThat(JsonParser.parse("42")).isEqualTo(new JsonNumber(new BigDecimal("42")));
        assertThat(JsonParser.parse("-3.5e2")).isEqualTo(new JsonNumber(new BigDecimal("-3.5e2")));
        assertThat(JsonParser.parse("[]")).isEqualTo(new JsonArray(java.util.List.of()));
        assertThat(JsonParser.parse("{}")).isEqualTo(new JsonObject(java.util.Map.of()));
    }

    @Test
    void rejectsTrailingContent() {
        assertThatThrownBy(() -> JsonParser.parse("{}garbage")).isInstanceOf(JsonException.class);
    }

    @Test
    void rejectsUnterminatedString() {
        assertThatThrownBy(() -> JsonParser.parse("\"unterminated")).isInstanceOf(JsonException.class);
    }

    @Test
    void rejectsMalformedObject() {
        assertThatThrownBy(() -> JsonParser.parse("{\"a\":1,}")).isInstanceOf(JsonException.class);
    }

    @Test
    void objectAccessorsThrowOnMissingOrWrongType() {
        JsonObject object = JsonObject.builder().put("a", "text").build();

        assertThatThrownBy(() -> object.getNumber("a")).isInstanceOf(JsonException.class);
        assertThatThrownBy(() -> object.getString("missing")).isInstanceOf(JsonException.class);
    }

    @Test
    void getOptionalStringReturnsNullForAbsentOrJsonNull() {
        JsonObject object = JsonObject.builder().put("present", "value").put("explicitNull", (JsonValue) null).build();

        assertThat(object.getOptionalString("present")).isEqualTo("value");
        assertThat(object.getOptionalString("explicitNull")).isNull();
        assertThat(object.getOptionalString("absent")).isNull();
    }
}
