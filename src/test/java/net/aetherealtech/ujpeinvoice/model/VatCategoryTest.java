package net.aetherealtech.ujpeinvoice.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VatCategoryTest {

    @Test
    void ratesMatchMacedonianVatLaw() {
        assertThat(VatCategory.STANDARD_18.rate()).isEqualByComparingTo("0.18");
        assertThat(VatCategory.REDUCED_10.rate()).isEqualByComparingTo("0.10");
        assertThat(VatCategory.REDUCED_5.rate()).isEqualByComparingTo("0.05");
        assertThat(VatCategory.ZERO.rate()).isEqualByComparingTo("0.00");
        assertThat(VatCategory.EXEMPT.rate()).isEqualByComparingTo("0.00");
    }

    @Test
    void everyCategoryHasANonBlankCode() {
        for (VatCategory category : VatCategory.values()) {
            assertThat(category.code()).isNotBlank();
        }
    }
}
