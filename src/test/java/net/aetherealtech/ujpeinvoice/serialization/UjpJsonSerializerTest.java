package net.aetherealtech.ujpeinvoice.serialization;

import net.aetherealtech.ujpeinvoice.internal.json.JsonArray;
import net.aetherealtech.ujpeinvoice.internal.json.JsonBoolean;
import net.aetherealtech.ujpeinvoice.internal.json.JsonNull;
import net.aetherealtech.ujpeinvoice.internal.json.JsonObject;
import net.aetherealtech.ujpeinvoice.internal.json.JsonParser;
import net.aetherealtech.ujpeinvoice.model.Advance;
import net.aetherealtech.ujpeinvoice.model.Attribute;
import net.aetherealtech.ujpeinvoice.model.AttributeType;
import net.aetherealtech.ujpeinvoice.model.BasePrice;
import net.aetherealtech.ujpeinvoice.model.DocIndicator;
import net.aetherealtech.ujpeinvoice.model.Document;
import net.aetherealtech.ujpeinvoice.model.DocumentKind;
import net.aetherealtech.ujpeinvoice.model.Line;
import net.aetherealtech.ujpeinvoice.model.Payment;
import net.aetherealtech.ujpeinvoice.model.PaymentType;
import net.aetherealtech.ujpeinvoice.model.Subsidiary;
import net.aetherealtech.ujpeinvoice.model.TaxIndicators;
import net.aetherealtech.ujpeinvoice.model.VatGroup;
import net.aetherealtech.ujpeinvoice.testsupport.DocumentFixtures;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;

import static net.aetherealtech.ujpeinvoice.testsupport.DocumentFixtures.DATE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * What the worked examples do not exercise: the optional parts of a document and how each is written.
 * The examples themselves, with their goldens, are in {@link UjpExamplesTest}.
 */
class UjpJsonSerializerTest {

    private static final LocalDateTime AT = LocalDateTime.of(2026, 10, 20, 9, 5, 0);

    private final UjpJsonSerializer serializer = new UjpJsonSerializer();

    private JsonObject written(Document document) {
        return (JsonObject) JsonParser.parse(new String(serializer.serialize(document, AT), StandardCharsets.UTF_8));
    }

    private JsonObject body(Document document) {
        return written(document).getObject("document");
    }

    @Test
    void theContentTypeIsJson() {
        assertThat(serializer.contentType()).isEqualTo("application/json");
    }

    @Test
    void theTimestampKeepsItsSecondsEvenWhenTheyAreZero() {
        assertThat(written(DocumentFixtures.simpleInvoice()).getString("requestTimestamp"))
                .isEqualTo("2026-10-20T09:05:00");
    }

    @Test
    void anInvoiceWithoutPaymentWritesNullForTheGroup() {
        Document document = DocumentFixtures.invoice().line(DocumentFixtures.consultingHour("1")).build();

        assertThat(body(document).get("docPayment")).contains(JsonNull.INSTANCE);
    }

    @Test
    void aPaymentWritesEveryPartItIsGiven() {
        Payment payment = new Payment(PaymentType.P_12, 30, LocalDate.of(2026, 9, 27), "terms", "2% a month",
                "1% in 10 days", "ref 12/2026", Currency.getInstance("EUR"));
        JsonObject written = body(DocumentFixtures.invoice().payment(payment)
                .line(DocumentFixtures.consultingHour("1")).build()).getObject("docPayment");

        assertThat(written.getString("docPaymentTypeCode")).isEqualTo("P12");
        assertThat(written.getNumber("docPaymentTypeDueDays")).isEqualByComparingTo("30");
        assertThat(written.getString("docPaymentTypeDueDate")).isEqualTo("2026-09-27");
        assertThat(written.getString("docPaymentTerms")).isEqualTo("terms");
        assertThat(written.getString("docPaymentInterest")).isEqualTo("2% a month");
        assertThat(written.getString("docPaymentDiscount")).isEqualTo("1% in 10 days");
        assertThat(written.getString("docPaymentNote")).isEqualTo("ref 12/2026");
        assertThat(written.getString("docCurrency")).isEqualTo("EUR");
        assertThat(written.members()).doesNotContainKeys("docCurrencyDate", "docCurrencyExchRate");
    }

    @Test
    void aForeignCurrencyOnA130WritesNoRateBecauseTheModelHasNone() {
        Document document = Document.builder(DocumentKind.ADVANCE_INVOICE)
                .header(DocumentFixtures.header()).seller(DocumentFixtures.seller()).buyer(DocumentFixtures.buyer())
                .payment(new Payment(PaymentType.P_12, null, null, null, null, null, null, Currency.getInstance("EUR")))
                .line(DocumentFixtures.consultingHour("1")).build();
        JsonObject payment = body(document).getObject("docPayment");

        assertThat(payment.get("docCurrencyDate")).contains(JsonNull.INSTANCE);
        assertThat(payment.get("docCurrencyExchRate")).contains(JsonNull.INSTANCE);
    }

    @Test
    void anInvoiceWritesItsAttributesAtDocumentAndLineLevel() {
        Document document = DocumentFixtures.invoice()
                .attribute(new Attribute("LOT", "3454/44", AttributeType.STR))
                .line(Line.builder().description("x").quantity("1").unitPrice("1").standardRate(VatGroup.DDV_A)
                        .attributes(List.of(new Attribute("expiry", "2027-01-01", AttributeType.DATE))).build())
                .build();
        JsonObject written = body(document);

        JsonObject attribute = (JsonObject) written.getArray("docAttributes").items().getFirst();
        assertThat(attribute.getString("name")).isEqualTo("LOT");
        assertThat(attribute.getString("type")).isEqualTo("STR");
        JsonObject lineAttribute = (JsonObject) ((JsonObject) written.getArray("docItems").items().getFirst())
                .getArray("attributes").items().getFirst();
        assertThat(lineAttribute.getString("type")).isEqualTo("DATE");
    }

    @Test
    void aNoteWritesNeitherDocAttributesNorLineAttributes() {
        Document note = Document.builder(DocumentKind.CREDIT_NOTE).header(DocumentFixtures.header())
                .seller(DocumentFixtures.seller()).buyer(DocumentFixtures.buyer())
                .line(DocumentFixtures.consultingHour("1")).build();
        String json = new String(serializer.serialize(note, AT), StandardCharsets.UTF_8);

        assertThat(json).doesNotContain("docAttributes", "\"attributes\"");
    }

    @Test
    void aBasePriceWritesItsFourMembersAndNothingWhenAbsent() {
        BasePrice excise = new BasePrice(new BigDecimal("80"), new BigDecimal("30"), new BigDecimal("10"), "excise");
        Document with = DocumentFixtures.invoice().line(Line.builder().description("x").quantity("1").unitPrice("100")
                .standardRate(VatGroup.DDV_A).basePrice(excise).build()).build();
        JsonObject item = (JsonObject) body(with).getArray("docItems").items().getFirst();

        assertThat(item.getNumber("docItemBasePriceWoVat")).isEqualByComparingTo("80");
        assertThat(item.getNumber("docItemBasePriceWoVatAdd")).isEqualByComparingTo("30");
        assertThat(item.getNumber("docItemBasePriceWoVatDeduct")).isEqualByComparingTo("10");
        assertThat(item.getString("docItemBasePriceType")).isEqualTo("excise");
        JsonObject plain = (JsonObject) body(DocumentFixtures.simpleInvoice()).getArray("docItems").items().getFirst();
        assertThat(plain.members()).doesNotContainKey("docItemBasePriceWoVat");
    }

    @Test
    void aSubsidiaryAndContactsAreWrittenWhenPresentAndAbsentOtherwise() {
        Document document = DocumentFixtures.invoice()
                .buyer(DocumentFixtures.buyer().withSubsidiary(new Subsidiary("B1", "Branch", "Ul. Branch 2"))
                        .withContact("02/1").withEmail("a@b.mk").withVatNumber("MK1"))
                .line(DocumentFixtures.consultingHour("1")).build();
        JsonObject buyer = body(document).getObject("buyer");

        assertThat(buyer.getString("buyerSubsidiaryCode")).isEqualTo("B1");
        assertThat(buyer.getString("buyerSubsidiaryName")).isEqualTo("Branch");
        assertThat(buyer.getString("buyerSubsidiaryAddress")).isEqualTo("Ul. Branch 2");
        assertThat(buyer.getString("buyerContact")).isEqualTo("02/1");
        assertThat(buyer.getString("buyerEmail")).isEqualTo("a@b.mk");
        assertThat(buyer.getString("buyerVatNumber")).isEqualTo("MK1");
        assertThat(body(document).getObject("seller").members()).doesNotContainKey("sellerSubsidiaryCode");
    }

    @Test
    void theHeaderWritesDeliveryPeriodIndicatorAndTheAlternativeMode() {
        Document document = DocumentFixtures.invoice()
                .header(DocumentFixtures.header().withDelivery(LocalDate.of(2026, 8, 20), "by road")
                        .withPeriod(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)).withNotes("n")
                        .withTexts("memo", "footer").withIndicator(DocIndicator.KONECNA).withAlternativeMode(true))
                .line(DocumentFixtures.consultingHour("1")).build();
        JsonObject header = body(document).getObject("header");

        assertThat(header.getString("docDeliveryDate")).isEqualTo("2026-08-20");
        assertThat(header.getString("docDelivery")).isEqualTo("by road");
        assertThat(header.getString("docPeriodStartDate")).isEqualTo("2026-08-01");
        assertThat(header.getString("docPeriodEndDate")).isEqualTo("2026-08-31");
        assertThat(header.getString("docNotes")).isEqualTo("n");
        assertThat(header.getString("docHeader")).isEqualTo("memo");
        assertThat(header.getString("docFooter")).isEqualTo("footer");
        assertThat(header.getString("docIndicator")).isEqualTo("KONECNA");
        assertThat(header.get("docAlt")).contains(JsonBoolean.of(true));
    }

    @Test
    void anAdvanceIsWrittenInTheTotals() {
        Document document = DocumentFixtures.invoice()
                .advance(new Advance(DATE, "advance for goods", new BigDecimal("100")))
                .line(DocumentFixtures.consultingHour("2")).build();
        JsonObject totals = body(document).getObject("docTotals");

        assertThat(totals.getString("docAvansDate")).isEqualTo("2026-08-28");
        assertThat(totals.getString("docAvansDesc")).isEqualTo("advance for goods");
        assertThat(totals.getNumber("docAvansAmount")).isEqualByComparingTo("100");
        assertThat(totals.getNumber("docFinalAmount")).isEqualByComparingTo("136");
    }

    @Test
    void aTransferredIndicatorIsWrittenWithItsNoteAndTheVatOnlyInThePerIndicatorTotals() {
        Document document = DocumentFixtures.invoice()
                .line(Line.builder().description("works").quantity("1").unitPrice("1000")
                        .tax(VatGroup.DDV_A, TaxIndicators.SNAPSHOT_2026_08_27.get("DDV-11-A-G")).build())
                .build();
        JsonObject written = body(document);
        JsonObject vat = (JsonObject) written.getArray("vatTotals").items().getFirst();

        assertThat(((JsonObject) written.getArray("docItems").items().getFirst()).getNumber("docItemTotalVat"))
                .isEqualByComparingTo("0");
        assertThat(written.getObject("docTotals").getNumber("docVatAmount")).isEqualByComparingTo("0");
        assertThat(vat.getNumber("vatAmount")).isEqualByComparingTo("180");
        assertThat(vat.getNumber("vatTotalAmount")).isEqualByComparingTo("1180");
        assertThat(vat.getString("vatTaxIndicatorNote")).startsWith("Пренесување на даночна обврска");
    }

    @Test
    void theLineNumberIsThePositionFromOne() {
        Document document = DocumentFixtures.invoice().line(DocumentFixtures.consultingHour("1"))
                .line(DocumentFixtures.consultingHour("2")).build();
        JsonArray items = body(document).getArray("docItems");

        assertThat(((JsonObject) items.items().get(0)).getNumber("docItemLineNo")).isEqualByComparingTo("1");
        assertThat(((JsonObject) items.items().get(1)).getNumber("docItemLineNo")).isEqualByComparingTo("2");
    }

    @Test
    void theSameDocumentSerializesToTheSameBytes() {
        Document document = DocumentFixtures.simpleInvoice();

        assertThat(serializer.serialize(document, AT)).isEqualTo(serializer.serialize(document, AT));
    }
}
