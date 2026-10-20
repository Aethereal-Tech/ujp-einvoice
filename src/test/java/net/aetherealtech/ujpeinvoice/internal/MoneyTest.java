package net.aetherealtech.ujpeinvoice.internal;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyTest {

    @Test
    void thereIsOneRoundingModeAndItIsHalfUp() {
        assertThat(Money.MODE).isEqualTo(RoundingMode.HALF_UP);
    }

    @Test
    void aLineValueIsCarriedToFourDecimals() {
        assertThat(Money.line(new BigDecimal("4.761905"))).isEqualByComparingTo("4.7619");
        assertThat(Money.line(new BigDecimal("305.084754"))).isEqualByComparingTo("305.0848");
        assertThat(Money.line(new BigDecimal("0.00005"))).isEqualByComparingTo("0.0001");
        assertThat(Money.line(new BigDecimal("5")).scale()).isEqualTo(4);
    }

    @Test
    void aTotalIsRoundedToTwoDecimals() {
        assertThat(Money.total(new BigDecimal("0.125"))).isEqualByComparingTo("0.13");
        assertThat(Money.total(new BigDecimal("0.124"))).isEqualByComparingTo("0.12");
        assertThat(Money.total(new BigDecimal("0.005"))).isEqualByComparingTo("0.01");
        assertThat(Money.total(new BigDecimal("5")).scale()).isEqualTo(2);
    }

    @Test
    void aWholeNumberFieldIsRoundedToZeroDecimals() {
        assertThat(Money.whole(new BigDecimal("3417.1001"))).isEqualByComparingTo("3417");
        assertThat(Money.whole(new BigDecimal("694.5"))).isEqualByComparingTo("695");
        assertThat(Money.whole(new BigDecimal("300.0032")).scale()).isZero();
    }

    @Test
    void halfRoundsAwayFromZeroSoAStornoMirrorsItsOriginal() {
        assertThat(Money.whole(new BigDecimal("-694.5"))).isEqualByComparingTo("-695");
        assertThat(Money.total(new BigDecimal("-0.125"))).isEqualByComparingTo("-0.13");
    }
}
