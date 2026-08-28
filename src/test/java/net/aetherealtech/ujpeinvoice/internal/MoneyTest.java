package net.aetherealtech.ujpeinvoice.internal;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyTest {

    @Test
    void roundsHalfUp() {
        assertThat(Money.round(new BigDecimal("0.125"))).isEqualByComparingTo("0.13");
        assertThat(Money.round(new BigDecimal("0.124"))).isEqualByComparingTo("0.12");
        assertThat(Money.round(new BigDecimal("0.005"))).isEqualByComparingTo("0.01");
    }

    @Test
    void resultHasExactlyTwoDecimalPlaces() {
        assertThat(Money.round(new BigDecimal("5")).scale()).isEqualTo(2);
    }
}
