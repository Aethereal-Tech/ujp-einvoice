package net.aetherealtech.ujpeinvoice.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The one rounding rule the whole domain model uses: two decimal places, half-up. Not part of the
 * public API — every public money value already comes out rounded, so callers never round again.
 */
public final class Money {

    /** Minor-unit scale for invoice money amounts (cents, deni, ...). */
    public static final int SCALE = 2;

    private Money() {
    }

    /** Rounds to {@link #SCALE} decimal places, half-up, e.g. 0.125 -&gt; 0.13. */
    public static BigDecimal round(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
