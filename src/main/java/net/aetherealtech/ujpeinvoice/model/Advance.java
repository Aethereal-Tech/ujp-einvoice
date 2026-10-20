package net.aetherealtech.ujpeinvoice.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * An advance already paid, deducted on a final invoice (UJP08-06 to UJP08-08). The amount is a whole
 * number, as UJP08-08 types it, and is refused rather than rounded: the caller states the money.
 */
public record Advance(LocalDate date, String description, BigDecimal amount) {

    public Advance {
        description = Check.optional(description, "UJP08-07", "advance.description", 50);
        Check.wholeNumber(amount, "UJP08-08", "advance.amount");
    }
}
