package net.aetherealtech.ujpeinvoice.model;

import java.math.BigDecimal;

/**
 * The one place a refusal is worded, so every message reads "UJPnn-nn field problem" and cites the
 * field's UJP code. The code is a citation of the type page, not a wire name.
 */
final class Check {

    private Check() {
    }

    static void that(boolean holds, String ujp, String field, String problem) {
        if (!holds) {
            throw new DocumentValidationException(ujp + " " + field + " " + problem);
        }
    }

    static <T> T present(T value, String ujp, String field) {
        that(value != null, ujp, field, "is required");
        return value;
    }

    static String required(String value, String ujp, String field, int maxLength) {
        that(value != null && !value.isBlank(), ujp, field, "is required");
        that(value.length() <= maxLength, ujp, field, "is at most " + maxLength + " characters");
        return value;
    }

    /** An absent value is {@code null}; a blank one is a missing value pretending to be present. */
    static String optional(String value, String ujp, String field, int maxLength) {
        if (value == null) {
            return null;
        }
        that(!value.isBlank(), ujp, field, "must not be blank; omit it instead");
        that(value.length() <= maxLength, ujp, field, "is at most " + maxLength + " characters");
        return value;
    }

    static BigDecimal decimals(BigDecimal value, int maxScale, String ujp, String field) {
        that(value.stripTrailingZeros().scale() <= maxScale, ujp, field,
                "has at most " + maxScale + " decimals; the model refuses rather than rounds");
        return value;
    }

    /** A Decimal 15.2 amount that may not be negative: UJP08-08, UJP08-11 and UJP08-12 since the 19.10.2026 changelog. */
    static BigDecimal amount(BigDecimal value, String ujp, String field) {
        that(value != null && value.signum() >= 0, ujp, field, "must be an amount of zero or more");
        return decimals(value, 2, ujp, field);
    }

    /** The 19.10.2026 note: a fraction is for a foreign currency, and an MKD sum is a whole number. */
    static void wholeInMkd(BigDecimal value, String ujp, String field) {
        that(value.stripTrailingZeros().scale() <= 0, ujp, field,
                "must be a whole number in MKD: decimal values are used only for foreign currencies "
                        + "(changelog 19.10.2026)");
    }
}
