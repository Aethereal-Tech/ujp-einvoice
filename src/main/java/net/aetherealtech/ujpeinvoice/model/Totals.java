package net.aetherealtech.ujpeinvoice.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * An invoice's money summary: a subtotal per {@link VatCategory} plus the three grand totals.
 *
 * <p>The only way to obtain a correct {@link Totals} is {@link #compute(List)} — there is no public
 * constructor that lets net/VAT/gross be set independently of the line items they describe. {@link
 * Invoice}'s canonical constructor still re-derives and reconciles totals against the line items it
 * is given (see {@link #reconciles(List)}), because a {@code Totals} value can also arrive from
 * outside this library — e.g. once deserialized from a wire payload — where nothing has enforced
 * that invariant yet.
 */
public record Totals(Map<VatCategory, CategoryTotal> byCategory, BigDecimal netTotal, BigDecimal vatTotal,
                      BigDecimal grossTotal) {

    public Totals {
        if (byCategory == null) {
            throw new InvoiceValidationException("Totals.byCategory must not be null");
        }
        byCategory = Collections.unmodifiableMap(new EnumMap<>(byCategory));
        if (netTotal == null || vatTotal == null || grossTotal == null) {
            throw new InvoiceValidationException("Totals amounts must not be null");
        }
    }

    /**
     * Sums each line's already-rounded {@link LineItem#netAmount()} and {@link LineItem#vatAmount()}
     * per category, then sums the categories. Every summand is already scaled to two decimals, so
     * the sums need no further rounding and this method is exact given its inputs.
     */
    public static Totals compute(List<LineItem> lineItems) {
        if (lineItems == null || lineItems.isEmpty()) {
            throw new InvoiceValidationException("At least one line item is required");
        }

        Map<VatCategory, BigDecimal> netByCategory = new EnumMap<>(VatCategory.class);
        Map<VatCategory, BigDecimal> vatByCategory = new EnumMap<>(VatCategory.class);
        for (LineItem item : lineItems) {
            netByCategory.merge(item.vatCategory(), item.netAmount(), BigDecimal::add);
            vatByCategory.merge(item.vatCategory(), item.vatAmount(), BigDecimal::add);
        }

        Map<VatCategory, CategoryTotal> byCategory = new EnumMap<>(VatCategory.class);
        BigDecimal netTotal = BigDecimal.ZERO.setScale(2);
        BigDecimal vatTotal = BigDecimal.ZERO.setScale(2);
        for (VatCategory category : netByCategory.keySet()) {
            BigDecimal net = netByCategory.get(category);
            BigDecimal vat = vatByCategory.get(category);
            BigDecimal gross = net.add(vat);
            byCategory.put(category, new CategoryTotal(category, net, vat, gross));
            netTotal = netTotal.add(net);
            vatTotal = vatTotal.add(vat);
        }
        BigDecimal grossTotal = netTotal.add(vatTotal);
        return new Totals(byCategory, netTotal, vatTotal, grossTotal);
    }

    /**
     * Whether this {@code Totals} is what {@link #compute} would produce from {@code lineItems}.
     * Compares numerically ({@link BigDecimal#compareTo}), not via {@link BigDecimal#equals}, so a
     * value written as {@code 2.50} and one written as {@code 2.5} are not treated as a mismatch
     * over a difference that is only ever about scale, never about magnitude.
     */
    public boolean reconciles(List<LineItem> lineItems) {
        return numericallyEquals(compute(lineItems));
    }

    private boolean numericallyEquals(Totals other) {
        if (!byCategory.keySet().equals(other.byCategory.keySet())) {
            return false;
        }
        for (Map.Entry<VatCategory, CategoryTotal> entry : byCategory.entrySet()) {
            CategoryTotal mine = entry.getValue();
            CategoryTotal theirs = other.byCategory.get(entry.getKey());
            if (theirs == null
                    || mine.net().compareTo(theirs.net()) != 0
                    || mine.vat().compareTo(theirs.vat()) != 0
                    || mine.gross().compareTo(theirs.gross()) != 0) {
                return false;
            }
        }
        return netTotal.compareTo(other.netTotal) == 0
                && vatTotal.compareTo(other.vatTotal) == 0
                && grossTotal.compareTo(other.grossTotal) == 0;
    }
}
