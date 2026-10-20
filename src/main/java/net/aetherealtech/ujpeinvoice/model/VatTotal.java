package net.aetherealtech.ujpeinvoice.model;

import net.aetherealtech.ujpeinvoice.internal.Money;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The tax base, VAT and total of the lines that share a tax indicator and a rate group (UJP09-01),
 * summed from the lines' unrounded values and rounded once to two decimals. Derived, never accepted.
 *
 * <p>Under a PRENESEN indicator the lines carry no VAT and the document totals exclude it, but this
 * entry shows the VAT the group's rate would put on the base, with the indicator's note: the
 * description of PRENESEN says it appears "кај тоталите по даночен индикатор".
 */
public record VatTotal(TaxIndicator indicator, VatGroup group, BigDecimal taxable, BigDecimal vat,
                       BigDecimal total) {

    /** One entry per indicator and group, in the order they first appear among the lines. */
    static List<VatTotal> compute(List<Line> lines) {
        Map<List<Object>, List<Line>> grouped = new LinkedHashMap<>();
        for (Line line : lines) {
            grouped.computeIfAbsent(List.of(line.taxIndicator().code(), line.vatGroup()), key -> new ArrayList<>())
                    .add(line);
        }
        return grouped.values().stream().map(VatTotal::of).toList();
    }

    private static VatTotal of(List<Line> group) {
        Line first = group.getFirst();
        boolean transferred = first.taxIndicator().vatImpact() == VatImpact.PRENESEN;
        BigDecimal taxable = BigDecimal.ZERO;
        BigDecimal vat = BigDecimal.ZERO;
        for (Line line : group) {
            taxable = taxable.add(line.total());
            vat = vat.add(transferred ? line.notionalVat() : line.totalVat());
        }
        return new VatTotal(first.taxIndicator(), first.vatGroup(), Money.total(taxable), Money.total(vat),
                Money.total(taxable.add(vat)));
    }
}
