package net.aetherealtech.ujpeinvoice.internal;

import net.aetherealtech.ujpeinvoice.ProvisionalSpec;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The one rounding rule the library uses: {@link RoundingMode#HALF_UP}, applied at the scale each
 * layer of UJP's document is defined to carry (/json-struktura, "Заокружување"). A line's values
 * are carried to 4 decimals and not rounded further; VAT and document totals are summed from those
 * unrounded values and rounded once, to 2; the fields of UJP08-08 to UJP08-12 are whole numbers in
 * MKD and carry two decimals in a foreign currency. A second
 * rounding mode, or rounding a line before it is summed, makes the totals disagree with the
 * gateway's own recomputation. Not part of the public API.
 */
public final class Money {

    public static final RoundingMode MODE = RoundingMode.HALF_UP;

    /** Decimals a line value (quantity, price, VAT per unit or per line) is carried to. */
    public static final int LINE_SCALE = 4;

    /** Decimals a VAT total or a document total is rounded to. */
    public static final int TOTAL_SCALE = 2;

    /**
     * Decimals of UJP08-08 to UJP08-12 (advance, final amount, gross rounded, increase, decrease) in
     * MKD. They became Decimal 15.2 on 19.10.2026; the changelog keeps an MKD sum a whole number and
     * reserves the decimals for a foreign currency, where they carry {@link #TOTAL_SCALE}.
     */
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

    /**
     * Rounds to a whole number, e.g. 3417.1001 -&gt; 3417. The 28.04 changelog calls docGrossAmountR
     * "банкарско заокружување"; the newer /json-struktura says ordinary half-up. No example is a tie
     * (the 21.10 multi-item sums 667.5032, and 667.5 would be 668 under either mode), so none settles it.
     */
    @ProvisionalSpec("docGrossAmountR: banking rounding (28.04 changelog) or half-up (/json-struktura, followed here)")
    public static BigDecimal whole(BigDecimal value) {
        return value.setScale(WHOLE_SCALE, MODE);
    }

    /**
     * docGrossAmountR: the gross rounded to a whole number in MKD, and to the two decimals of a
     * foreign currency, where the 19.10.2026 changelog makes the field Decimal 15.2.
     */
    @ProvisionalSpec("docGrossAmountR in a foreign currency is the gross at 2 decimals: the 19.10 changelog "
            + "allows decimals there and names no rule, and the 21.10 EUR examples (kind 510, page 63, and kind 630, page 76) "
            + "have a whole gross, so they cannot show whether it is rounded to 0 or kept at 2")
    public static BigDecimal grossRounded(BigDecimal gross, boolean domestic) {
        return domestic ? whole(gross) : total(gross);
    }
}
