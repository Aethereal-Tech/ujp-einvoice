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

    static BigDecimal wholeNumber(BigDecimal value, String ujp, String field) {
        that(value != null && value.signum() >= 0, ujp, field, "must be a whole number, zero or more");
        that(value.stripTrailingZeros().scale() <= 0, ujp, field, "must be a whole number");
        return value;
    }
}
