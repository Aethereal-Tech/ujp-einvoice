package net.aetherealtech.ujpeinvoice.model;

import net.aetherealtech.ujpeinvoice.testsupport.DocumentFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;

import static net.aetherealtech.ujpeinvoice.testsupport.DocumentFixtures.DATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentTest {

    private static final Reference ORIGINAL = new Reference(RefDocumentType.ST, DATE, "019dd423-bef7-73b7-abe6-f69721682406");

    private static Document.Builder of(DocumentKind kind) {
        Document.Builder builder = Document.builder(kind)
                .header(Header.of("N-1", "1", DATE).withTurnoverDate(DATE))
                .seller(DocumentFixtures.seller())
                .buyer(DocumentFixtures.buyer())
                .line(DocumentFixtures.consultingHour("2"));
        return kind.requiresPaymentType() ? builder.payment(Payment.of(PaymentType.P_12)) : builder;
    }

    private static void assertRefused(Document.Builder builder, String message) {
        assertThatThrownBy(builder::build).isInstanceOf(DocumentValidationException.class).hasMessage(message);
    }

    @ParameterizedTest
    @EnumSource(DocumentKind.class)
    void aDocumentOfAnyKindIsBuiltFromItsMandatoryParts(DocumentKind kind) {
        Document document = of(kind).build();

        assertThat(document.kind()).isEqualTo(kind);
        assertThat(document.mode()).isEqualTo(Mode.ORDINARY);
        assertThat(document.adjustments()).isEqualTo(Adjustments.NONE);
        assertThat(document.totals().gross()).isEqualByComparingTo("236");
    }

    @Test
    void theTotalsAreDerivedFromTheLines() {
        Document document = DocumentFixtures.simpleInvoice();

        DocumentTotals totals = document.totals();
        assertThat(totals.net()).isEqualByComparingTo("200");
        assertThat(totals.discount()).isEqualByComparingTo("0");
        assertThat(totals.netAfterDiscount()).isEqualByComparingTo("200");
        assertThat(totals.vat()).isEqualByComparingTo("36");
        assertThat(totals.gross()).isEqualByComparingTo("236");
        assertThat(totals.grossRounded()).isEqualByComparingTo("236");
        assertThat(totals.finalAmount()).isEqualByComparingTo("236");
        assertThat(totals.net().scale()).isEqualTo(2);
        assertThat(totals.grossRounded().scale()).isZero();
    }

    @Test
    void theDiscountIsSummedAndTheNetIsTakenBeforeIt() {
        Document document = DocumentFixtures.invoice()
                .line(Line.builder().description("x").quantity("2").unitPrice("1694.9153").unitDiscount("169.4915")
                        .standardRate(VatGroup.DDV_A).build())
                .build();

        assertThat(document.totals().net()).isEqualByComparingTo("3389.83");
        assertThat(document.totals().discount()).isEqualByComparingTo("338.98");
        assertThat(document.totals().netAfterDiscount()).isEqualByComparingTo("3050.85");
    }

    @Test
    void theFinalAmountTakesTheAdvanceTheIncreaseAndTheDecrease() {
        Document document = DocumentFixtures.invoice()
                .line(Line.builder().description("x").quantity("1").unitPrice("847.46").standardRate(VatGroup.DDV_A).build())
                .advance(new Advance(DATE, "advance", new BigDecimal("500")))
                .adjustments(new Adjustments(new BigDecimal("10"), new BigDecimal("5")))
                .build();

        DocumentTotals totals = document.totals();
        assertThat(totals.grossRounded()).isEqualByComparingTo("1000");
        assertThat(totals.advanceAmount()).isEqualByComparingTo("500");
        assertThat(totals.increase()).isEqualByComparingTo("10");
        assertThat(totals.decrease()).isEqualByComparingTo("5");
        assertThat(totals.finalAmount()).isEqualByComparingTo("505");
    }

    @Test
    void aFractionInMkdIsRefusedByNameAndCitesTheChangelog() {
        String note = " must be a whole number in MKD: decimal values are used only for foreign currencies (changelog 19.10.2026)";
        assertRefused(DocumentFixtures.invoice().line(DocumentFixtures.consultingHour("1"))
                .advance(new Advance(DATE, null, new BigDecimal("10.50"))), "UJP08-08 advance.amount" + note);
        assertRefused(DocumentFixtures.invoice().line(DocumentFixtures.consultingHour("1"))
                .adjustments(new Adjustments(new BigDecimal("0.10"), null)), "UJP08-11 adjustments.increase" + note);
        assertRefused(DocumentFixtures.invoice().line(DocumentFixtures.consultingHour("1"))
                .adjustments(new Adjustments(null, new BigDecimal("2.5"))), "UJP08-12 adjustments.decrease" + note);
        assertRefused(DocumentFixtures.invoice().line(DocumentFixtures.consultingHour("1"))
                .payment(Payment.of(PaymentType.P_12))
                .adjustments(new Adjustments(new BigDecimal("0.10"), null)), "UJP08-11 adjustments.increase" + note);
    }

    @Test
    void aWholeAmountWrittenWithTwoDecimalsIsStillWholeInMkd() {
        Document document = DocumentFixtures.invoice().line(DocumentFixtures.consultingHour("1"))
                .adjustments(new Adjustments(new BigDecimal("102.00"), null)).build();

        assertThat(document.totals().increase()).isEqualByComparingTo("102");
        assertThat(document.totals().finalAmount()).isEqualByComparingTo("220");
    }

    @Test
    void aForeignCurrencyCarriesTwoDecimalsInTheAmountsOfTheFinalFormula() {
        Payment euro = new Payment(PaymentType.P_12, null, null, null, null, null, null, Currency.getInstance("EUR"));
        Document document = DocumentFixtures.invoice().payment(euro)
                .line(Line.builder().description("x").quantity("1").unitPrice("100.40").standardRate(VatGroup.DDV_A).build())
                .advance(new Advance(DATE, "advance", new BigDecimal("10.25")))
                .adjustments(new Adjustments(new BigDecimal("1.10"), new BigDecimal("0.35")))
                .build();

        DocumentTotals totals = document.totals();
        assertThat(document.currency()).isEqualTo(Currency.getInstance("EUR"));
        assertThat(totals.gross()).isEqualByComparingTo("118.47");
        assertThat(totals.grossRounded()).isEqualByComparingTo("118.47");
        assertThat(totals.advanceAmount()).isEqualByComparingTo("10.25");
        assertThat(totals.finalAmount()).isEqualByComparingTo("108.97");
    }

    @Test
    void theCurrencyIsMkdWhereThereIsNoPaymentGroup() {
        assertThat(of(DocumentKind.CREDIT_NOTE).build().currency()).isEqualTo(Payment.MKD);
        assertThat(of(DocumentKind.INVOICE).build().currency()).isEqualTo(Payment.MKD);
    }

    @Test
    void aStornoNegatesEveryQuantityAndTheTotalsFollow() {
        Document storno = of(DocumentKind.INVOICE)
                .line(Line.builder().description("y").quantity("1").unitPrice("50").standardRate(VatGroup.DDV_V).build())
                .mode(new Mode.Storno(VoidReason.S_1, null))
                .reference(ORIGINAL)
                .build();

        assertThat(storno.lines()).extracting(Line::quantity).allSatisfy(q -> assertThat(q.signum()).isPositive());
        assertThat(storno.effectiveLines()).extracting(Line::quantity)
                .allSatisfy(q -> assertThat(q.signum()).isNegative());
        assertThat(storno.effectiveLines().getFirst().unitPrice()).isEqualByComparingTo("100");
        assertThat(storno.effectiveLines().getFirst().unitVat()).isEqualByComparingTo("18");
        assertThat(storno.totals().gross()).isEqualByComparingTo("-291");
        assertThat(storno.totals().finalAmount()).isEqualByComparingTo("-291");
        assertThat(storno.vatTotals()).extracting(VatTotal::total)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(new BigDecimal("-236"), new BigDecimal("-55"));
    }

    @Test
    void aCorrectionKeepsItsAmountsPositive() {
        Document correction = of(DocumentKind.INVOICE)
                .mode(new Mode.Correction(CorrectionReason.C01, null))
                .reference(ORIGINAL)
                .build();

        assertThat(correction.effectiveLines()).isEqualTo(correction.lines());
        assertThat(correction.totals().gross()).isEqualByComparingTo("236");
    }

    @Test
    void vatTotalsAreOnePerIndicatorAndGroupInFirstAppearanceOrder() {
        Document document = DocumentFixtures.invoice()
                .line(Line.builder().description("b").quantity("1").unitPrice("100").standardRate(VatGroup.DDV_B).build())
                .line(Line.builder().description("a").quantity("1").unitPrice("200").standardRate(VatGroup.DDV_A).build())
                .line(Line.builder().description("b2").quantity("2").unitPrice("10").standardRate(VatGroup.DDV_B).build())
                .build();

        List<VatTotal> totals = document.vatTotals();
        assertThat(totals).extracting(total -> total.indicator().code()).containsExactly("DDV-B", "DDV-A");
        assertThat(totals.get(0).taxable()).isEqualByComparingTo("120");
        assertThat(totals.get(0).vat()).isEqualByComparingTo("6");
        assertThat(totals.get(0).total()).isEqualByComparingTo("126");
        assertThat(totals.get(1).taxable()).isEqualByComparingTo("200");
        assertThat(totals.get(1).vat()).isEqualByComparingTo("36");
    }

    @Test
    void oneIndicatorInTwoGroupsGivesTwoEntries() {
        TaxIndicator several = TaxIndicators.SNAPSHOT_2026_08_27.get("DDV-14-4");
        Document document = DocumentFixtures.invoice()
                .line(Line.builder().description("v").quantity("1").unitPrice("100").tax(VatGroup.DDV_V, several).build())
                .line(Line.builder().description("b").quantity("1").unitPrice("100").tax(VatGroup.DDV_B, several).build())
                .build();

        assertThat(document.vatTotals()).extracting(VatTotal::group).containsExactly(VatGroup.DDV_V, VatGroup.DDV_B);
    }

    @Test
    void aTransferredTaxIsInThePerIndicatorTotalsAndNotInTheDocumentTotals() {
        TaxIndicator transferred = TaxIndicators.SNAPSHOT_2026_08_27.get("DDV-11-A-G");
        Document document = DocumentFixtures.invoice()
                .line(Line.builder().description("works").quantity("1").unitPrice("1000").tax(VatGroup.DDV_A, transferred).build())
                .build();

        assertThat(document.totals().vat()).isEqualByComparingTo("0");
        assertThat(document.totals().gross()).isEqualByComparingTo("1000");
        VatTotal total = document.vatTotals().getFirst();
        assertThat(total.taxable()).isEqualByComparingTo("1000");
        assertThat(total.vat()).isEqualByComparingTo("180");
        assertThat(total.total()).isEqualByComparingTo("1180");
        assertThat(total.indicator().note()).isNotBlank();
    }

    @Test
    void anExemptSaleHasNoVatAnywhere() {
        TaxIndicator exempt = TaxIndicators.SNAPSHOT_2026_08_27.get("DDV-9-1");
        Document document = DocumentFixtures.invoice()
                .line(Line.builder().description("service").quantity("1").unitPrice("1111").tax(VatGroup.DDV_G, exempt).build())
                .build();

        assertThat(document.totals().vat()).isEqualByComparingTo("0");
        assertThat(document.vatTotals().getFirst().vat()).isEqualByComparingTo("0");
        assertThat(document.vatTotals().getFirst().total()).isEqualByComparingTo("1111");
    }

    // ---- refusals, by name ----------------------------------------------------------------------

    @ParameterizedTest
    @EnumSource(value = DocumentKind.class, names = {"INVOICE", "CASH_TURNOVER"})
    void aTurnoverDateIsRequiredWhereThePageSaysSo(DocumentKind kind) {
        assertRefused(of(kind).header(Header.of("N-1", "1", DATE)),
                "UJP01-06 turnoverDate is required on a " + kind.code());
    }

    @ParameterizedTest
    @EnumSource(value = DocumentKind.class, names = {"CREDIT_NOTE", "DEBIT_NOTE", "ADVANCE_INVOICE"})
    void aTurnoverDateIsNotRequiredElsewhere(DocumentKind kind) {
        assertThat(of(kind).header(Header.of("N-1", "1", DATE)).build().header().turnoverDate()).isNull();
    }

    @Test
    void theIdOfACashTurnoverIsAtMostTwelveCharacters() {
        assertRefused(of(DocumentKind.CASH_TURNOVER).header(Header.of("N-1", "1234567890123", DATE).withTurnoverDate(DATE)),
                "UJP01-11 id is at most 12 characters on a 310");
        assertThat(of(DocumentKind.CASH_TURNOVER).header(Header.of("N-1", "123456789012", DATE).withTurnoverDate(DATE)).build())
                .isNotNull();
    }

    @Test
    void anIndicatorIsRefusedOnAKindThatDoesNotListIt() {
        assertRefused(of(DocumentKind.CREDIT_NOTE).header(Header.of("N-1", "1", DATE).withIndicator(DocIndicator.KONECNA)),
                "UJP01-15 indicator is not valid on a 110: KONECNA");
        assertThat(of(DocumentKind.INVOICE).header(DocumentFixtures.header().withIndicator(DocIndicator.KONECNA)).build())
                .isNotNull();
    }

    @Test
    void aMissingKindOrHeaderIsRefused() {
        assertThatThrownBy(() -> Document.builder(null).build()).hasMessage("UJP01-01 kind is required");
        assertThatThrownBy(() -> Document.builder(DocumentKind.INVOICE).build()).hasMessage("UJP01 header is required");
    }

    @ParameterizedTest
    @EnumSource(value = DocumentKind.class, names = {"CREDIT_NOTE", "DEBIT_NOTE"})
    void aNoteHasNoPaymentGroup(DocumentKind kind) {
        assertRefused(of(kind).payment(Payment.of(PaymentType.P_11)), "UJP10 payment is not on the page of a " + kind.code());
    }

    @ParameterizedTest
    @EnumSource(value = DocumentKind.class, names = {"ADVANCE_INVOICE", "CASH_TURNOVER"})
    void aPaymentTypeIsRequiredOn130And310(DocumentKind kind) {
        assertRefused(of(kind).payment(null), "UJP10-01 payment.type is required on a " + kind.code());
        assertRefused(of(kind).payment(new Payment(null, 30, null, null, null, null, null, null)),
                "UJP10-01 payment.type is required on a " + kind.code());
    }

    @Test
    void aPaymentWithoutATypeIsAcceptedOnAnInvoice() {
        assertThat(of(DocumentKind.INVOICE).payment(new Payment(null, 30, null, null, null, null, null, null)).build().payment())
                .isNotNull();
    }

    @ParameterizedTest
    @EnumSource(value = DocumentKind.class, names = {"CREDIT_NOTE", "DEBIT_NOTE", "ADVANCE_INVOICE", "CASH_TURNOVER"})
    void anAdvanceDeductionIsOnTheInvoicePageOnly(DocumentKind kind) {
        assertRefused(of(kind).advance(new Advance(DATE, null, BigDecimal.TEN)),
                "UJP08-06 advance is not on the page of a " + kind.code());
    }

    @ParameterizedTest
    @EnumSource(value = DocumentKind.class, names = {"CREDIT_NOTE", "DEBIT_NOTE", "ADVANCE_INVOICE", "CASH_TURNOVER"})
    void attributesAreOnTheInvoicePageOnly(DocumentKind kind) {
        Attribute attribute = new Attribute("LOT", "1", AttributeType.STR);

        assertRefused(of(kind).attribute(attribute), "UJP12-01 attributes are not on the page of a " + kind.code());
        assertRefused(of(kind).line(Line.builder().description("x").unit("kg").quantity("1").unitPrice("1")
                        .standardRate(VatGroup.DDV_A).attributes(List.of(attribute)).build()),
                "UJP07-26 lines[2].attributes are not on the page of a " + kind.code());
    }

    @Test
    void anInvoiceTakesDocumentAndLineAttributes() {
        Document document = of(DocumentKind.INVOICE)
                .attribute(new Attribute("LOT", "3454/44", AttributeType.STR))
                .line(Line.builder().description("x").quantity("1").unitPrice("1").standardRate(VatGroup.DDV_A)
                        .attributes(List.of(new Attribute("expiry", "2027-01-01", AttributeType.DATE))).build())
                .build();

        assertThat(document.attributes()).hasSize(1);
        assertThat(document.lines().get(1).attributes()).hasSize(1);
    }

    @ParameterizedTest
    @EnumSource(value = DocumentKind.class, names = {"ADVANCE_INVOICE", "CASH_TURNOVER"})
    void aUnitIsRequiredOn130And310(DocumentKind kind) {
        assertRefused(of(kind).line(Line.builder().description("x").quantity("1").unitPrice("1").standardRate(VatGroup.DDV_A).build()),
                "UJP07-07 lines[2].unit is required on a " + kind.code());
    }

    @Test
    void aNaturalPersonBuyerIsRefusedBecauseThatIsKind170() {
        assertRefused(of(DocumentKind.INVOICE).buyer(Party.naturalPerson("Ana", new Address("S", "1", "1000", "Skopje"))),
                "UJP05-03 buyer.tin is required: a natural-person buyer is kind 170, which this library does not ship");
    }

    @Test
    void aBuyerInMacedoniaNeedsAnEdbAndAForeignBuyerNeedsAForeignTin() {
        Party macedonianWithForeignTin = new Party(Country.MK, null, "DE123", null, "n", DocumentFixtures.buyer().address(),
                null, null, null);

        assertRefused(of(DocumentKind.INVOICE).buyer(macedonianWithForeignTin),
                "UJP05-03 buyer.tin is required for a buyer in MK; foreignTin is for a non-resident");
        Party german = Party.foreignCompany(new Country("DE", "Germany"), "DE123", "GmbH", DocumentFixtures.buyer().address());
        assertThat(of(DocumentKind.INVOICE).buyer(german).build().buyer().foreignTin()).isEqualTo("DE123");
    }

    @Test
    void theSellerIsAnIdentifiedBusiness() {
        Party withoutTin = new Party(Country.MK, null, "DE123", null, "n", DocumentFixtures.seller().address(), null, null, null);

        assertRefused(of(DocumentKind.INVOICE).seller(withoutTin),
                "UJP03-03 seller.tin is required: a seller is always an identified business");
        assertRefused(of(DocumentKind.INVOICE).seller(null), "UJP03 seller is required");
        assertRefused(of(DocumentKind.INVOICE).buyer(null), "UJP05 buyer is required");
    }

    @Test
    void aPartyNeedsAnAddress() {
        Party withoutAddress = new Party(Country.MK, "1", null, null, "n", null, null, null, null);

        assertRefused(of(DocumentKind.INVOICE).seller(withoutAddress), "UJP03-07 seller.address is required");
    }

    @Test
    void thePartyLengthsAreShorterOnACashTurnover() {
        Address longStreet = new Address("S".repeat(61), "1", "1000", "Skopje");
        Address longNumber = new Address("S", "1".repeat(11), "1000", "Skopje");
        Party longName = Party.company("N".repeat(81), "1", DocumentFixtures.seller().address());
        Subsidiary longSubsidiary = new Subsidiary(null, "S".repeat(101), null);

        assertRefused(of(DocumentKind.CASH_TURNOVER).seller(Party.company("Acme", "1", longStreet)),
                "UJP03-07-01 seller.address.street is at most 60 characters on a 310");
        assertRefused(of(DocumentKind.CASH_TURNOVER).seller(Party.company("Acme", "1", longNumber)),
                "UJP03-07-02 seller.address.number is at most 10 characters on a 310");
        assertRefused(of(DocumentKind.CASH_TURNOVER).buyer(longName),
                "UJP05-06 buyer.name is at most 80 characters on a 310");
        assertRefused(of(DocumentKind.CASH_TURNOVER).buyer(DocumentFixtures.buyer().withSubsidiary(longSubsidiary)),
                "UJP05-09 buyer.subsidiary.name is at most 100 characters on a 310");
        assertThat(of(DocumentKind.INVOICE).seller(Party.company("N".repeat(150), "1", DocumentFixtures.seller().address())).build())
                .isNotNull();
    }

    @Test
    void aDocumentNeedsALineAndEveryDescribedQuantityIsPositive() {
        assertRefused(Document.builder(DocumentKind.INVOICE).header(DocumentFixtures.header())
                .seller(DocumentFixtures.seller()).buyer(DocumentFixtures.buyer()), "UJP07-01 lines needs at least one line");
        Line negative = DocumentFixtures.consultingHour("1");
        Line negated = new Line(negative.sku(), negative.senderCode(), negative.receiverCode(), negative.description(),
                negative.unit(), new BigDecimal("-1"), negative.unitPrice(), negative.unitDiscount(),
                negative.vatGroup(), negative.taxIndicator(), null, null, null);

        assertRefused(of(DocumentKind.INVOICE).line(negated),
                "UJP07-08 lines[2].quantity must be positive: describe the original's lines, a storno negates them itself");
    }

    @Test
    void aStornoAndACorrectionNeedExactlyOneReferenceToTheOriginal() {
        assertRefused(of(DocumentKind.INVOICE).mode(new Mode.Storno(VoidReason.S_1, null)),
                "UJP02-01-01 references needs exactly one reference of type ST naming the original, to be stornoed");
        assertRefused(of(DocumentKind.INVOICE).mode(new Mode.Correction(CorrectionReason.C01, null)),
                "UJP02-01-01 references needs exactly one reference of type ST naming the original, to be corrected");
        assertRefused(of(DocumentKind.INVOICE).mode(new Mode.Storno(VoidReason.S_1, null)).reference(ORIGINAL).reference(ORIGINAL),
                "UJP02-01-01 references needs exactly one reference of type ST naming the original, to be stornoed");
    }

    @Test
    void anOrdinaryDocumentMayNotNameAnOriginal() {
        assertRefused(of(DocumentKind.INVOICE).reference(ORIGINAL),
                "UJP02-01-01 references may not contain type ST on an ordinary document: "
                        + "ST names the original of a storno or correction");
        assertThat(of(DocumentKind.INVOICE).reference(new Reference(RefDocumentType.NAR, DATE, "order-7")).build().references())
                .hasSize(1);
    }

    @Test
    void aNoteMayHaveNoReferencesAlthoughThePageMarksTheGroupMandatory() {
        assertThat(DocumentKind.CREDIT_NOTE.requiresReferences()).isTrue();
        assertThat(of(DocumentKind.CREDIT_NOTE).build().references()).isEmpty();
    }

    @Test
    void aDocumentIsImmutableAfterItIsBuilt() {
        Document.Builder builder = of(DocumentKind.INVOICE);
        Document document = builder.build();
        builder.line(DocumentFixtures.consultingHour("5"));

        assertThat(document.lines()).hasSize(1);
        assertThatThrownBy(() -> document.lines().add(DocumentFixtures.consultingHour("1")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void aPeriodAndDeliveryAreCarriedThrough() {
        Header header = DocumentFixtures.header().withDelivery(LocalDate.of(2026, 8, 20), "by road")
                .withPeriod(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        assertThat(of(DocumentKind.INVOICE).header(header).build().header().delivery()).isEqualTo("by road");
    }
}
