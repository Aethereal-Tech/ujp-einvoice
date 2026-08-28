package net.aetherealtech.ujpeinvoice.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TotalsTest {

    @Test
    void computesSingleCategoryTotals() {
        List<LineItem> items = List.of(
                new LineItem("A", new BigDecimal("2"), new BigDecimal("100.00"), VatCategory.STANDARD_18));

        Totals totals = Totals.compute(items);

        assertThat(totals.netTotal()).isEqualByComparingTo("200.00");
        assertThat(totals.vatTotal()).isEqualByComparingTo("36.00");
        assertThat(totals.grossTotal()).isEqualByComparingTo("236.00");
        assertThat(totals.byCategory()).hasSize(1);
        CategoryTotal categoryTotal = totals.byCategory().get(VatCategory.STANDARD_18);
        assertThat(categoryTotal.net()).isEqualByComparingTo("200.00");
        assertThat(categoryTotal.vat()).isEqualByComparingTo("36.00");
        assertThat(categoryTotal.gross()).isEqualByComparingTo("236.00");
    }

    @Test
    void sumsMultipleLinesWithinTheSameCategory() {
        List<LineItem> items = List.of(
                new LineItem("A", BigDecimal.ONE, new BigDecimal("10.00"), VatCategory.STANDARD_18),
                new LineItem("B", BigDecimal.ONE, new BigDecimal("20.00"), VatCategory.STANDARD_18));

        Totals totals = Totals.compute(items);

        assertThat(totals.byCategory()).hasSize(1);
        assertThat(totals.netTotal()).isEqualByComparingTo("30.00");
        assertThat(totals.vatTotal()).isEqualByComparingTo("5.40");
    }

    @Test
    void aggregatesEveryVatCategoryAcrossTheWholeInvoice() {
        List<LineItem> items = List.of(
                new LineItem("Standard", new BigDecimal("3"), new BigDecimal("50.00"), VatCategory.STANDARD_18),
                new LineItem("Reduced10", BigDecimal.ONE, new BigDecimal("40.00"), VatCategory.REDUCED_10),
                new LineItem("Reduced5", new BigDecimal("5"), new BigDecimal("20.00"), VatCategory.REDUCED_5),
                new LineItem("Zero", BigDecimal.ONE, new BigDecimal("500.00"), VatCategory.ZERO),
                new LineItem("Exempt", new BigDecimal("2"), new BigDecimal("75.00"), VatCategory.EXEMPT));

        Totals totals = Totals.compute(items);

        assertThat(totals.byCategory()).hasSize(5);
        assertThat(totals.netTotal()).isEqualByComparingTo("940.00");
        assertThat(totals.vatTotal()).isEqualByComparingTo("36.00");
        assertThat(totals.grossTotal()).isEqualByComparingTo("976.00");
    }

    @Test
    void rejectsEmptyLineItems() {
        assertThatThrownBy(() -> Totals.compute(List.of()))
                .isInstanceOf(InvoiceValidationException.class);
    }

    @Test
    void rejectsNullLineItems() {
        assertThatThrownBy(() -> Totals.compute(null))
                .isInstanceOf(InvoiceValidationException.class);
    }

    @Test
    void reconcilesWithTheLineItemsItWasComputedFrom() {
        List<LineItem> items = List.of(
                new LineItem("A", new BigDecimal("2"), new BigDecimal("100.00"), VatCategory.STANDARD_18));
        Totals totals = Totals.compute(items);

        assertThat(totals.reconciles(items)).isTrue();
    }

    @Test
    void doesNotReconcileWhenALineChanges() {
        List<LineItem> original = List.of(
                new LineItem("A", new BigDecimal("2"), new BigDecimal("100.00"), VatCategory.STANDARD_18));
        Totals totals = Totals.compute(original);

        List<LineItem> changed = List.of(
                new LineItem("A", new BigDecimal("3"), new BigDecimal("100.00"), VatCategory.STANDARD_18));

        assertThat(totals.reconciles(changed)).isFalse();
    }

    @Test
    void reconciliationIsScaleInsensitive() {
        // 2.50 and 2.5 are equal numerically but not via BigDecimal#equals; reconciles() must treat
        // them as the same amount.
        List<LineItem> items = List.of(
                new LineItem("A", BigDecimal.ONE, new BigDecimal("2.50"), VatCategory.ZERO));
        Map<VatCategory, CategoryTotal> byCategory = new EnumMap<>(VatCategory.class);
        byCategory.put(VatCategory.ZERO, new CategoryTotal(VatCategory.ZERO,
                new BigDecimal("2.5"), new BigDecimal("0"), new BigDecimal("2.5")));
        Totals handWritten = new Totals(byCategory, new BigDecimal("2.5"), new BigDecimal("0"), new BigDecimal("2.5"));

        assertThat(handWritten.reconciles(items)).isTrue();
    }

    @Test
    void rejectsNullByCategory() {
        assertThatThrownBy(() -> new Totals(null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(InvoiceValidationException.class);
    }

    @Test
    void rejectsNullAmounts() {
        Map<VatCategory, CategoryTotal> empty = new EnumMap<>(VatCategory.class);
        assertThatThrownBy(() -> new Totals(empty, null, BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(InvoiceValidationException.class);
        assertThatThrownBy(() -> new Totals(empty, BigDecimal.ZERO, null, BigDecimal.ZERO))
                .isInstanceOf(InvoiceValidationException.class);
        assertThatThrownBy(() -> new Totals(empty, BigDecimal.ZERO, BigDecimal.ZERO, null))
                .isInstanceOf(InvoiceValidationException.class);
    }
}
