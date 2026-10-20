package net.aetherealtech.ujpeinvoice.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * An additional field on a line or on the document (UJP07-26, UJP12-01), for data the structured
 * fields have no place for. The value is checked against the declared type: the gateway could only
 * discover a mismatch after the signature was made.
 */
public record Attribute(String name, String value, AttributeType type) {

    public Attribute {
        name = Check.required(name, "UJP12-01-01", "attribute.name", 100);
        value = Check.required(value, "UJP12-01-02", "attribute.value", 100);
        Check.present(type, "UJP12-01-03", "attribute.type");
        Check.that(fits(value, type), "UJP12-01-02", "attribute.value", "is not a valid " + type + ": " + value);
    }

    private static boolean fits(String value, AttributeType type) {
        try {
            switch (type) {
                case STR -> {
                }
                case DEC -> new BigDecimal(value);
                case INT -> new BigInteger(value);
                case DATE -> LocalDate.parse(value);
            }
            return true;
        } catch (NumberFormatException | DateTimeParseException e) {
            return false;
        }
    }
}
