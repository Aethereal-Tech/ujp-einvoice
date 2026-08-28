package net.aetherealtech.ujpeinvoice.internal.json;

import java.math.BigDecimal;

/** A JSON number, held as {@link BigDecimal} throughout so a money value never passes through a
 * binary floating-point representation on its way to or from the wire. */
public record JsonNumber(BigDecimal value) implements JsonValue {

    public JsonNumber {
        if (value == null) {
            throw new NullPointerException("JsonNumber value must not be null; use JsonNull.INSTANCE");
        }
    }
}
