package net.aetherealtech.ujpeinvoice.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * An advance already paid, deducted on a final invoice (UJP08-06 to UJP08-08). UJP08-08 is Decimal 15.2
 * since 19.10.2026, refused beyond two decimals rather than rounded: the caller states the money.
 * {@link Document} refuses a fraction in MKD, where the changelog says the sum is a whole number.
 */
public record Advance(LocalDate date, String description, BigDecimal amount) {

    public Advance {
        description = Check.optional(description, "UJP08-07", "advance.description", 50);
        Check.amount(amount, "UJP08-08", "advance.amount");
    }
}
