package net.aetherealtech.ujpeinvoice.model;

import java.math.BigDecimal;

/**
 * Amounts that change what is payable without entering the tax base (UJP08-11, UJP08-12): default
 * interest added, a financial discount taken off. Decimal 15.2 since 19.10.2026, refused beyond two
 * decimals rather than rounded; {@link Document} refuses a fraction in MKD. An absent amount counts as 0.
 */
public record Adjustments(BigDecimal increase, BigDecimal decrease) {

    public static final Adjustments NONE = new Adjustments(BigDecimal.ZERO, BigDecimal.ZERO);

    public Adjustments {
        increase = increase == null ? BigDecimal.ZERO : increase;
        decrease = decrease == null ? BigDecimal.ZERO : decrease;
        Check.amount(increase, "UJP08-11", "adjustments.increase");
        Check.amount(decrease, "UJP08-12", "adjustments.decrease");
    }
}
