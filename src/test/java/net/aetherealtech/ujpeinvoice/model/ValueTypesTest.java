package net.aetherealtech.ujpeinvoice.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValueTypesTest {

    private static final Address ADDRESS = new Address("Street", "1", "1000", "Skopje");

    @Test
    void anAddressNeedsAllFourPartsAndNamesTheMissingOne() {
        assertThatThrownBy(() -> new Address(null, "1", "1000", "Skopje")).hasMessage("UJP03-07-01 address.street is required");
        assertThatThrownBy(() -> new Address("S", " ", "1000", "Skopje")).hasMessage("UJP03-07-02 address.number is required");
        assertThatThrownBy(() -> new Address("S", "1", null, "Skopje")).hasMessage("UJP03-07-03 address.postalCode is required");
        assertThatThrownBy(() -> new Address("S", "1", "1000", "")).hasMessage("UJP03-07-04 address.city is required");
    }

    @Test
    void aTextBeyondItsLengthIsRefused() {
        assertThatThrownBy(() -> new Address("S".repeat(81), "1", "1000", "Skopje"))
                .hasMessage("UJP03-07-01 address.street is at most 80 characters");
    }

    @Test
    void aCountryHasACodeAndAName() {
        assertThat(Country.MK.code()).isEqualTo("MK");
        assertThatThrownBy(() -> new Country("", "x")).hasMessage("UJP03-01 country.code is required");
        assertThatThrownBy(() -> new Country("MK", null)).hasMessage("UJP03-02 country.name is required");
    }

    @Test
    void aSubsidiaryNeedsSomething() {
        assertThat(new Subsidiary("S1", null, null).code()).isEqualTo("S1");
        assertThatThrownBy(() -> new Subsidiary(null, null, null)).hasMessage("UJP03-08 subsidiary needs a code, a name or an address");
        assertThatThrownBy(() -> new Subsidiary(" ", "n", null)).hasMessageContaining("must not be blank");
    }

    @Test
    void aCompanyNeedsATinAndAnAddress() {
        Party company = Party.company("Acme", "4030012345678", ADDRESS);

        assertThat(company.isNaturalPerson()).isFalse();
        assertThat(company.country()).isEqualTo(Country.MK);
        assertThatThrownBy(() -> Party.company("Acme", null, ADDRESS)).hasMessage("UJP03-03 tin is required");
        assertThatThrownBy(() -> Party.company("Acme", "1", null)).hasMessage("UJP03-07 address is required");
    }

    @Test
    void aTinIsNotFormatChecked() {
        assertThat(Party.company("Acme", "123", ADDRESS).tin()).isEqualTo("123");
    }

    @Test
    void aNaturalPersonHasNoTaxNumberOfEitherKind() {
        assertThat(Party.naturalPerson("Ana").isNaturalPerson()).isTrue();
        assertThat(Party.naturalPerson("Ana", ADDRESS).address()).isEqualTo(ADDRESS);
        assertThat(Party.naturalPerson("Ana").withForeignTin("DE123").isNaturalPerson()).isFalse();
    }

    @Test
    void aForeignCompanyCarriesAForeignTinInPlaceOfATin() {
        Party german = Party.foreignCompany(new Country("DE", "Germany"), "DE123456789", "GmbH", ADDRESS);

        assertThat(german.tin()).isNull();
        assertThat(german.foreignTin()).isEqualTo("DE123456789");
        assertThatThrownBy(() -> Party.foreignCompany(Country.MK, null, "x", ADDRESS)).hasMessage("UJP05-04 foreignTin is required");
        assertThatThrownBy(() -> Party.foreignCompany(Country.MK, "x", "x", null)).hasMessage("UJP05-07 address is required");
    }

    @Test
    void aPartyReplacesOneOfItsOptionalPartsAtATime() {
        Party party = Party.company("Acme", "1", ADDRESS).withCountry(new Country("DE", "Germany"))
                .withVatNumber("DE1").withContact("02/1").withEmail("a@b.mk")
                .withSubsidiary(new Subsidiary("S", null, null));

        assertThat(party.country().code()).isEqualTo("DE");
        assertThat(party.vatNumber()).isEqualTo("DE1");
        assertThat(party.contact()).isEqualTo("02/1");
        assertThat(party.email()).isEqualTo("a@b.mk");
        assertThat(party.subsidiary().code()).isEqualTo("S");
    }

    @Test
    void anAbsentCountryReadsAsMacedonia() {
        assertThat(new Party(null, "1", null, null, "n", ADDRESS, null, null, null).country()).isEqualTo(Country.MK);
    }

    @Test
    void aHeaderNeedsANumberAnIdAndADate() {
        LocalDate date = LocalDate.of(2026, 1, 1);
        assertThatThrownBy(() -> Header.of(null, "1", date)).hasMessage("UJP01-09 number is required");
        assertThatThrownBy(() -> Header.of("1", "", date)).hasMessage("UJP01-11 id is required");
        assertThatThrownBy(() -> Header.of("1", "1", null)).hasMessage("UJP01-03 date is required");
    }

    @Test
    void aPeriodMayNotEndBeforeItStarts() {
        Header header = Header.of("1", "1", LocalDate.of(2026, 1, 1));

        assertThatThrownBy(() -> header.withPeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1)))
                .hasMessage("UJP01-08 periodEnd must not precede periodStart (UJP01-07)");
        assertThat(header.withPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1)).periodEnd())
                .isEqualTo(LocalDate.of(2026, 2, 1));
    }

    @Test
    void aHeaderReplacesOneOfItsOptionalPartsAtATime() {
        LocalDate date = LocalDate.of(2026, 1, 1);
        Header header = Header.of("1", "1", date).withDelivery(date, "by road").withNotes("n")
                .withTexts("memo", "footer").withIndicator(DocIndicator.KONECNA).withAlternativeMode(true)
                .withTurnoverDate(date);

        assertThat(header.deliveryDate()).isEqualTo(date);
        assertThat(header.delivery()).isEqualTo("by road");
        assertThat(header.notes()).isEqualTo("n");
        assertThat(header.headerText()).isEqualTo("memo");
        assertThat(header.footerText()).isEqualTo("footer");
        assertThat(header.indicator()).isEqualTo(DocIndicator.KONECNA);
        assertThat(header.alternativeMode()).isTrue();
        assertThat(header.turnoverDate()).isEqualTo(date);
    }

    @Test
    void anIndicatorIsValidOnlyOnTheKindsTheChangelogNames() {
        assertThat(DocIndicator.KONECNA.validOn(DocumentKind.INVOICE)).isTrue();
        assertThat(DocIndicator.KOMISIONA.validOn(DocumentKind.INVOICE)).isTrue();
        assertThat(DocIndicator.KONECNA.validOn(DocumentKind.CREDIT_NOTE)).isFalse();
        assertThat(DocIndicator.PN.validOn(DocumentKind.INVOICE)).isFalse();
    }

    @Test
    void anAttributeValueMustFitItsType() {
        assertThat(new Attribute("LOT", "3454/44", AttributeType.STR).type()).isEqualTo(AttributeType.STR);
        assertThat(new Attribute("n", "12.5", AttributeType.DEC)).isNotNull();
        assertThat(new Attribute("n", "12", AttributeType.INT)).isNotNull();
        assertThat(new Attribute("n", "2026-10-20", AttributeType.DATE)).isNotNull();
        assertThatThrownBy(() -> new Attribute("n", "abc", AttributeType.DEC)).hasMessage("UJP12-01-02 attribute.value is not a valid DEC: abc");
        assertThatThrownBy(() -> new Attribute("n", "1.5", AttributeType.INT)).hasMessageContaining("INT");
        assertThatThrownBy(() -> new Attribute("n", "20.10.2026", AttributeType.DATE)).hasMessageContaining("DATE");
        assertThatThrownBy(() -> new Attribute("n", "v", null)).hasMessage("UJP12-01-03 attribute.type is required");
        assertThatThrownBy(() -> new Attribute("", "v", AttributeType.STR)).hasMessage("UJP12-01-01 attribute.name is required");
    }

    @Test
    void aBasePriceAddsUpAndNeverGoesNegative() {
        BasePrice basePrice = new BasePrice(new BigDecimal("80"), null, null, null);

        assertThat(basePrice.add()).isEqualByComparingTo("0");
        assertThat(basePrice.deduct()).isEqualByComparingTo("0");
        assertThat(basePrice.unitPrice()).isEqualByComparingTo("80");
        assertThatThrownBy(() -> new BasePrice(null, null, null, null)).hasMessage("UJP07-22 basePrice.base is required");
        assertThatThrownBy(() -> new BasePrice(new BigDecimal("-1"), null, null, null)).hasMessageContaining("must not be negative");
        assertThatThrownBy(() -> new BasePrice(BigDecimal.ONE, new BigDecimal("-1"), null, null)).hasMessageStartingWith("UJP07-23");
        assertThatThrownBy(() -> new BasePrice(BigDecimal.ONE, null, new BigDecimal("-1"), null)).hasMessageStartingWith("UJP07-24");
        assertThatThrownBy(() -> new BasePrice(new BigDecimal("1.00001"), null, null, null)).hasMessageContaining("4 decimals");
    }

    @Test
    void aPaymentDefaultsToMkdAndBoundsItsDueDays() {
        assertThat(Payment.of(PaymentType.P_12).currency()).isEqualTo(Payment.MKD);
        assertThat(new Payment(null, null, null, null, null, null, null, Currency.getInstance("EUR")).currency().getCurrencyCode())
                .isEqualTo("EUR");
        assertThat(new Payment(null, 30, null, "terms", "interest", "discount", "note", null).dueDays()).isEqualTo(30);
        assertThatThrownBy(() -> new Payment(null, 10000, null, null, null, null, null, null))
                .hasMessageStartingWith("UJP10-03 dueDays");
        assertThatThrownBy(() -> new Payment(null, -1, null, null, null, null, null, null))
                .hasMessageStartingWith("UJP10-03 dueDays");
    }

    @Test
    void anAdvanceAndAnAdjustmentAreWholeNumbersAndNeverRounded() {
        assertThat(new Advance(LocalDate.of(2026, 1, 1), "advance", new BigDecimal("100")).amount()).isEqualByComparingTo("100");
        assertThatThrownBy(() -> new Advance(null, null, new BigDecimal("100.5"))).hasMessage("UJP08-08 advance.amount must be a whole number");
        assertThatThrownBy(() -> new Advance(null, null, null)).hasMessage("UJP08-08 advance.amount must be a whole number, zero or more");
        assertThatThrownBy(() -> new Advance(null, null, new BigDecimal("-1"))).hasMessageContaining("zero or more");
        assertThat(new Adjustments(null, null)).isEqualTo(Adjustments.NONE);
        assertThat(new Adjustments(new BigDecimal("5"), new BigDecimal("2")).increase()).isEqualByComparingTo("5");
        assertThatThrownBy(() -> new Adjustments(new BigDecimal("1.5"), null)).hasMessageStartingWith("UJP08-11");
        assertThatThrownBy(() -> new Adjustments(null, new BigDecimal("1.5"))).hasMessageStartingWith("UJP08-12");
    }

    @Test
    void aReferenceNeedsAllFourPartsAndAnEuidFitsItsNumber() {
        LocalDate date = LocalDate.of(2026, 1, 1);
        String euid = "019dd423-bef7-73b7-abe6-f69721682406";

        assertThat(new Reference(RefDocumentType.ST, date, euid).number()).isEqualTo(euid);
        assertThatThrownBy(() -> new Reference(null, date, euid)).hasMessage("UJP02-01-01 reference.type is required");
        assertThatThrownBy(() -> new Reference(RefDocumentType.NAR, null, euid)).hasMessage("UJP02-01-03 reference.date is required");
        assertThatThrownBy(() -> new Reference(RefDocumentType.NAR, date, "x".repeat(37)))
                .hasMessage("UJP02-01-04 reference.number is at most 36 characters");
        assertThatThrownBy(() -> new RefDocumentType("", "n")).hasMessageStartingWith("UJP02-01-01");
        assertThatThrownBy(() -> new RefDocumentType("X", "")).hasMessageStartingWith("UJP02-01-02");
        assertThat(RefDocumentType.PIP.code()).isEqualTo("PIP");
    }

    @Test
    void thePaymentTypesAndReasonsCarryTheirCodeListNames() {
        assertThat(PaymentType.P_11.description()).isEqualTo("Плаќање со картичка");
        assertThatThrownBy(() -> new PaymentType("", "d")).hasMessageStartingWith("UJP10-01");
        assertThatThrownBy(() -> new PaymentType("P1", "")).hasMessageStartingWith("UJP10-02");
        assertThat(VoidReason.S_1.code()).isEqualTo("S-1");
        assertThat(CorrectionReason.C01.code()).isEqualTo("C01");
        assertThatThrownBy(() -> new VoidReason("", "n")).hasMessageStartingWith("voidReason code");
        assertThatThrownBy(() -> new CorrectionReason("C", "")).hasMessageStartingWith("correctionReason name");
    }

    @Test
    void theVatGroupsAreDdvAVBAndGAtTheirRates() {
        assertThat(VatGroup.DDV_V.percent()).isEqualByComparingTo("10");
        assertThat(VatGroup.DDV_B.percent()).isEqualByComparingTo("5");
        assertThat(VatGroup.DDV_G.percent()).isEqualByComparingTo("0");
        assertThat(VatGroup.fromCode("DDV-A")).isEqualTo(VatGroup.DDV_A);
        assertThat(VatGroup.fromCode("DDV-X")).isNull();
    }

    @Test
    void aStornoAndACorrectionNeedAReasonAndOtherAskForFreeText() {
        assertThatThrownBy(() -> new Mode.Storno(null, null)).hasMessage("UJP01-14 storno.reason is required");
        assertThatThrownBy(() -> new Mode.Storno(VoidReason.S_5, null))
                .hasMessageStartingWith("UJP01-14 storno.comment is required");
        assertThat(new Mode.Storno(VoidReason.S_5, "because").comment()).isEqualTo("because");
        assertThatThrownBy(() -> new Mode.Correction(null, null)).hasMessage("UJP01-14 correction.reason is required");
        assertThatThrownBy(() -> new Mode.Correction(CorrectionReason.C08, null))
                .hasMessageStartingWith("UJP01-14 correction.comment is required");
        assertThat(new Mode.Correction(CorrectionReason.C01, null).comment()).isNull();
    }
}
