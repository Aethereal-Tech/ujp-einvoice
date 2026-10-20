package net.aetherealtech.ujpeinvoice.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The one rounding rule the library uses: {@link RoundingMode#HALF_UP}, applied at the scale each
 * layer of UJP's document is defined to carry (/json-struktura, "Заокружување"). A line's values
 * are carried to 4 decimals and not rounded further; VAT and document totals are summed from those
 * unrounded values and rounded once, to 2; the whole-number fields are rounded to 0. A second
 * rounding mode, or rounding a line before it is summed, makes the totals disagree with the
 * gateway's own recomputation. Not part of the public API.
 */
public final class Money {

    public static final RoundingMode MODE = RoundingMode.HALF_UP;

    /** Decimals a line value (quantity, price, VAT per unit or per line) is carried to. */
    public static final int LINE_SCALE = 4;

    /** Decimals a VAT total or a document total is rounded to. */
    public static final int TOTAL_SCALE = 2;

    /** Decimals of the whole-number fields (UJP08-08, UJP08-09, UJP08-10, UJP08-11, UJP08-12). */
    public static final int WHOLE_SCALE = 0;

    private Money() {
    }

    /** Rounds to {@link #LINE_SCALE} decimals, e.g. 4.761905 -&gt; 4.7619. */
    public static BigDecimal line(BigDecimal value) {
        return value.setScale(LINE_SCALE, MODE);
    }

    /** Rounds to {@link #TOTAL_SCALE} decimals, e.g. 0.125 -&gt; 0.13. */
    public static BigDecimal total(BigDecimal value) {
        return value.setScale(TOTAL_SCALE, MODE);
    }

    /** Rounds to a whole number, e.g. 3417.1001 -&gt; 3417. */
    public static BigDecimal whole(BigDecimal value) {
        return value.setScale(WHOLE_SCALE, MODE);
    }

    /** Rounds to {@link #TOTAL_SCALE} decimals. Kept until the old model that calls it is removed. */
    public static BigDecimal round(BigDecimal value) {
        return total(value);
    }
}
