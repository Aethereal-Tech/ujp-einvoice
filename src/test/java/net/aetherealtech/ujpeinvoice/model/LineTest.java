package net.aetherealtech.ujpeinvoice.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LineTest {

    private static Line.Builder line() {
        return Line.builder().description("Widget").quantity("2").unitPrice("100").standardRate(VatGroup.DDV_A);
    }

    @Test
    void aLineDerivesEveryFigureFromQuantityPriceDiscountAndTax() {
        Line line = line().quantity("3").unitPrice("1694.9153").unitDiscount("169.4915").build();

        assertThat(line.unitPriceAfterDiscount()).isEqualByComparingTo("1525.4238");
        assertThat(line.unitVat()).isEqualByComparingTo("274.5763");
        assertThat(line.totalBeforeDiscount()).isEqualByComparingTo("5084.7459");
        assertThat(line.totalDiscount()).isEqualByComparingTo("508.4745");
        assertThat(line.total()).isEqualByComparingTo("4576.2714");
        assertThat(line.totalVat()).isEqualByComparingTo("823.7289");
        assertThat(line.totalWithVat()).isEqualByComparingTo("5400.0003");
    }

    @Test
    void derivedValuesAreCarriedToFourDecimalsAndNotRoundedFurther() {
        Line line = line().quantity("1").unitPrice("95.2381").standardRate(VatGroup.DDV_B).build();

        assertThat(line.unitVat().scale()).isEqualTo(4);
        assertThat(line.unitVat()).isEqualByComparingTo("4.7619");
    }

    @Test
    void aNonStandardIndicatorCarriesNoVatOnTheLine() {
        TaxIndicator exempt = TaxIndicators.SNAPSHOT_2026_10_16.get("DDV-9-1");
        Line line = line().tax(VatGroup.DDV_G, exempt).build();

        assertThat(line.unitVat()).isEqualByComparingTo("0");
        assertThat(line.totalVat()).isEqualByComparingTo("0");
        assertThat(line.totalWithVat()).isEqualByComparingTo("200");
    }

    @Test
    void aTransferredLineCarriesNoVatButKnowsWhatTheRateWouldAdd() {
        TaxIndicator transferred = TaxIndicators.SNAPSHOT_2026_10_16.get("DDV-11-A-G");
        Line line = line().tax(VatGroup.DDV_A, transferred).build();

        assertThat(line.totalVat()).isEqualByComparingTo("0");
        assertThat(line.notionalVat()).isEqualByComparingTo("36");
    }

    @Test
    void aQuantityAndAPriceBeyondFourDecimalsAreRefusedNotRounded() {
        assertThatThrownBy(() -> line().quantity("1.00001").build())
                .isInstanceOf(DocumentValidationException.class).hasMessageStartingWith("UJP07-08 quantity");
        assertThatThrownBy(() -> line().unitPrice("0.00001").build())
                .isInstanceOf(DocumentValidationException.class).hasMessageStartingWith("UJP07-09 unitPrice");
        assertThatThrownBy(() -> line().unitDiscount("0.00001").build())
                .isInstanceOf(DocumentValidationException.class).hasMessageStartingWith("UJP07-10 unitDiscount");
    }

    @Test
    void trailingZerosBeyondFourDecimalsAreNotDecimals() {
        assertThat(line().unitPrice("100.000000").build().unitPrice()).isEqualByComparingTo("100");
    }

    @Test
    void aZeroQuantityIsRefused() {
        assertThatThrownBy(() -> line().quantity("0").build())
                .hasMessage("UJP07-08 quantity must not be zero");
    }

    @Test
    void aNegativePriceOrDiscountIsRefusedAndAStornoNegatesTheQuantityInstead() {
        assertThatThrownBy(() -> line().unitPrice("-1").build())
                .hasMessageStartingWith("UJP07-09 unitPrice must not be negative");
        assertThatThrownBy(() -> line().unitDiscount("-1").build())
                .hasMessage("UJP07-10 unitDiscount must not be negative");
    }

    @Test
    void aDiscountLargerThanThePriceIsRefused() {
        assertThatThrownBy(() -> line().unitDiscount("100.01").build())
                .hasMessage("UJP07-10 unitDiscount must not exceed the unit price");
    }

    @Test
    void aZeroPriceIsAccepted() {
        assertThat(line().unitPrice("0").build().total()).isEqualByComparingTo("0");
    }

    @Test
    void aBlankDescriptionIsRefused() {
        assertThatThrownBy(() -> line().description(" ").build())
                .hasMessage("UJP07-06 description is required");
    }

    @Test
    void theGroupAndTheIndicatorMustAgree() {
        assertThatThrownBy(() -> line().tax(VatGroup.DDV_B, TaxIndicator.standard(VatGroup.DDV_A)).build())
                .hasMessage("UJP07-14 vatGroup is DDV-B but the tax indicator DDV-A belongs to DDV-A");
    }

    @Test
    void anIndicatorOfSeveralGroupsFollowsTheLinesGroup() {
        TaxIndicator several = TaxIndicators.SNAPSHOT_2026_10_16.get("DDV-14-4");

        assertThat(several.group()).isNull();
        assertThat(line().tax(VatGroup.DDV_V, several).build().vatGroup()).isEqualTo(VatGroup.DDV_V);
        assertThat(line().tax(VatGroup.DDV_B, several).build().vatGroup()).isEqualTo(VatGroup.DDV_B);
    }

    @Test
    void aMarginSchemeIndicatorIsRefusedBecauseNoDocumentSaysHowItIsComputed() {
        TaxIndicator margin = TaxIndicators.SNAPSHOT_2026_10_16.get("DDV-M-18");

        assertThatThrownBy(() -> line().tax(VatGroup.DDV_A, margin).build())
                .hasMessageContaining("margin-scheme");
    }

    @Test
    void aMissingGroupOrIndicatorIsRefused() {
        assertThatThrownBy(() -> Line.builder().description("x").quantity("1").unitPrice("1").build())
                .hasMessage("UJP07-14 vatGroup is required");
        assertThatThrownBy(() -> Line.builder().description("x").quantity("1").unitPrice("1")
                .tax(VatGroup.DDV_A, null).build()).hasMessage("UJP07-19 taxIndicator is required");
        assertThatThrownBy(() -> Line.builder().description("x").unitPrice("1").standardRate(VatGroup.DDV_A).build())
                .hasMessage("UJP07-08 quantity is required");
        assertThatThrownBy(() -> Line.builder().description("x").quantity("1").standardRate(VatGroup.DDV_A).build())
                .hasMessage("UJP07-09 unitPrice is required");
    }

    @Test
    void aBasePriceMustAddUpToTheUnitPrice() {
        BasePrice excise = new BasePrice(new BigDecimal("80"), new BigDecimal("30"), new BigDecimal("10"), "excise");

        assertThat(line().basePrice(excise).build().basePrice()).isEqualTo(excise);
        assertThatThrownBy(() -> line().unitPrice("101").basePrice(excise).build())
                .hasMessage("UJP07-22 basePrice base + add - deduct must equal the unit price 101");
    }

    @Test
    void theBuilderTakesBigDecimalsAndAttributes() {
        Line line = Line.builder().description("x").quantity(new BigDecimal("1")).unitPrice(new BigDecimal("2"))
                .unitDiscount(new BigDecimal("1")).standardRate(VatGroup.DDV_V).sku("a").senderCode("b")
                .receiverCode("c").domesticProduct(true)
                .attributes(List.of(new Attribute("LOT", "3454/44", AttributeType.STR))).build();

        assertThat(line.attributes()).hasSize(1);
        assertThat(line.domesticProduct()).isTrue();
        assertThat(line.unitPriceAfterDiscount()).isEqualByComparingTo("1");
    }

    @Test
    void anAbsentAttributeListIsEmpty() {
        assertThat(line().build().attributes()).isEmpty();
    }
}
