package net.aetherealtech.ujpeinvoice.model;

import net.aetherealtech.ujpeinvoice.internal.Money;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

/**
 * The totals of a document (UJP08), derived from its lines and never accepted from a caller. They
 * follow the published rule: the lines' unrounded values are summed, then rounded once, to two
 * decimals; the rounded gross, the advance, the increase and the decrease are whole numbers.
 *
 * <p>{@code gross} is the sum of the lines' unrounded gross, rounded, and not the rounded net plus the
 * rounded VAT; the two can differ by a cent, well inside the gateway's tolerance of 0.1.
 */
public record DocumentTotals(BigDecimal net, BigDecimal discount, BigDecimal netAfterDiscount, BigDecimal vat,
                             BigDecimal gross, BigDecimal grossRounded, BigDecimal advanceAmount,
                             BigDecimal increase, BigDecimal decrease, BigDecimal finalAmount) {

    static DocumentTotals compute(List<Line> lines, Advance advance, Adjustments adjustments) {
        BigDecimal net = sum(lines, Line::totalBeforeDiscount);
        BigDecimal discount = sum(lines, Line::totalDiscount);
        BigDecimal netAfterDiscount = sum(lines, Line::total);
        BigDecimal vat = sum(lines, Line::totalVat);
        BigDecimal unroundedGross = sum(lines, Line::totalWithVat);
        BigDecimal grossRounded = Money.whole(unroundedGross);
        BigDecimal advanceAmount = advance == null ? BigDecimal.ZERO : advance.amount();
        BigDecimal finalAmount = grossRounded.subtract(advanceAmount)
                .add(adjustments.increase()).subtract(adjustments.decrease());
        return new DocumentTotals(Money.total(net), Money.total(discount), Money.total(netAfterDiscount),
                Money.total(vat), Money.total(unroundedGross), grossRounded, Money.whole(advanceAmount),
                adjustments.increase(), adjustments.decrease(), finalAmount);
    }

    private static BigDecimal sum(List<Line> lines, Function<Line, BigDecimal> value) {
        return lines.stream().map(value).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
