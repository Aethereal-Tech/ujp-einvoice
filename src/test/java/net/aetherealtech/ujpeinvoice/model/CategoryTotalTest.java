package net.aetherealtech.ujpeinvoice.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryTotalTest {

    @Test
    void rejectsNullCategory() {
        assertThatThrownBy(() -> new CategoryTotal(null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(InvoiceValidationException.class);
    }

    @Test
    void rejectsNullAmounts() {
        assertThatThrownBy(() -> new CategoryTotal(VatCategory.STANDARD_18, null, BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(InvoiceValidationException.class);
        assertThatThrownBy(() -> new CategoryTotal(VatCategory.STANDARD_18, BigDecimal.ZERO, null, BigDecimal.ZERO))
                .isInstanceOf(InvoiceValidationException.class);
        assertThatThrownBy(() -> new CategoryTotal(VatCategory.STANDARD_18, BigDecimal.ZERO, BigDecimal.ZERO, null))
                .isInstanceOf(InvoiceValidationException.class);
    }
}
