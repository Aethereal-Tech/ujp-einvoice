package net.aetherealtech.ujpeinvoice.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LineItemTest {

    @ParameterizedTest(name = "{0} x {1} @ {2} -> net={3} vat={4} gross={5}")
    @CsvSource({
            // description omitted from CSV (fixed below), quantity, unitPrice, category, net, vat, gross
            "2, 100.00, STANDARD_18, 200.00, 36.00, 236.00",
            "1, 40.00, REDUCED_10, 40.00, 4.00, 44.00",
            "5, 20.00, REDUCED_5, 100.00, 5.00, 105.00",
            "1, 500.00, ZERO, 500.00, 0.00, 500.00",
            "2, 75.00, EXEMPT, 150.00, 0.00, 150.00",
    })
    void computesAmountsPerVatCategory(BigDecimal quantity, BigDecimal unitPrice, VatCategory category,
                                        BigDecimal expectedNet, BigDecimal expectedVat, BigDecimal expectedGross) {
        LineItem item = new LineItem("Line", quantity, unitPrice, category);

        assertThat(item.netAmount()).isEqualByComparingTo(expectedNet);
        assertThat(item.vatAmount()).isEqualByComparingTo(expectedVat);
        assertThat(item.grossAmount()).isEqualByComparingTo(expectedGross);
    }

    @Test
    void roundsVatHalfUp() {
        // net = 0.05 * 3 = 0.15, vat @ 18% = 0.027 -> rounds to 0.03 half-up.
        LineItem item = new LineItem("Rounding case", new BigDecimal("3"), new BigDecimal("0.05"),
                VatCategory.STANDARD_18);

        assertThat(item.netAmount()).isEqualByComparingTo("0.15");
        assertThat(item.vatAmount()).isEqualByComparingTo("0.03");
        assertThat(item.grossAmount()).isEqualByComparingTo("0.18");
    }

    @Test
    void roundsNetHalfUp() {
        // 3 * 0.145 = 0.435 -> rounds to 0.44 (0.435 rounds up under HALF_UP, not banker's rounding).
        LineItem item = new LineItem("Net rounding", new BigDecimal("3"), new BigDecimal("0.145"),
                VatCategory.ZERO);

        assertThat(item.netAmount()).isEqualByComparingTo("0.44");
    }

    @Test
    void rejectsBlankDescription() {
        assertThatThrownBy(() -> new LineItem("  ", BigDecimal.ONE, BigDecimal.TEN, VatCategory.STANDARD_18))
                .isInstanceOf(InvoiceValidationException.class);
    }

    @Test
    void rejectsNonPositiveQuantity() {
        assertThatThrownBy(() -> new LineItem("Item", BigDecimal.ZERO, BigDecimal.TEN, VatCategory.STANDARD_18))
                .isInstanceOf(InvoiceValidationException.class);
        assertThatThrownBy(() -> new LineItem("Item", new BigDecimal("-1"), BigDecimal.TEN, VatCategory.STANDARD_18))
                .isInstanceOf(InvoiceValidationException.class);
    }

    @Test
    void rejectsNegativeUnitPrice() {
        assertThatThrownBy(() -> new LineItem("Item", BigDecimal.ONE, new BigDecimal("-0.01"), VatCategory.STANDARD_18))
                .isInstanceOf(InvoiceValidationException.class);
    }

    @Test
    void allowsZeroUnitPrice() {
        LineItem item = new LineItem("Free sample", BigDecimal.ONE, BigDecimal.ZERO, VatCategory.STANDARD_18);
        assertThat(item.netAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void rejectsNullVatCategory() {
        assertThatThrownBy(() -> new LineItem("Item", BigDecimal.ONE, BigDecimal.TEN, null))
                .isInstanceOf(InvoiceValidationException.class);
    }
}
