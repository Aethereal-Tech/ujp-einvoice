package net.aetherealtech.ujpeinvoice.testsupport;

import net.aetherealtech.ujpeinvoice.model.Address;
import net.aetherealtech.ujpeinvoice.model.Adjustments;
import net.aetherealtech.ujpeinvoice.model.Attribute;
import net.aetherealtech.ujpeinvoice.model.AttributeType;
import net.aetherealtech.ujpeinvoice.model.CorrectionReason;
import net.aetherealtech.ujpeinvoice.model.Document;
import net.aetherealtech.ujpeinvoice.model.DocumentKind;
import net.aetherealtech.ujpeinvoice.model.Header;
import net.aetherealtech.ujpeinvoice.model.Line;
import net.aetherealtech.ujpeinvoice.model.Mode;
import net.aetherealtech.ujpeinvoice.model.Party;
import net.aetherealtech.ujpeinvoice.model.Payment;
import net.aetherealtech.ujpeinvoice.model.PaymentType;
import net.aetherealtech.ujpeinvoice.model.RefDocumentType;
import net.aetherealtech.ujpeinvoice.model.Reference;
import net.aetherealtech.ujpeinvoice.model.VatGroup;
import net.aetherealtech.ujpeinvoice.model.VoidReason;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The values of the worked examples in UJP's primer_za_json_21.10.pdf (87 pages, Last-Modified
 * 2026-10-22 14:52:11 GMT, sha256 60cdaa1a...), one document per scoped kind and mode and one per
 * feature the set added on 21.10, built through this library's own model. Only the inputs are here:
 * the amounts the model derives are asserted against the numbers the PDF prints, in
 * {@code UjpExamplesTest}. The PDF's JSON is not committed. The page numbers are the PDF's physical
 * pages, which the 21.10 table of contents matches.
 *
 * <p>The parties are the sample taxpayer of UJP's own examples, who is both seller and buyer there.
 */
public final class UjpExamples {

    private static final String EUID_OF_THE_STORNOED = "01a15e7f-5e4e-7dcf-849e-ccbf9f65386d";
    private static final String EUID_OF_THE_CORRECTED = "01a15e8a-8136-7d08-9703-e85ce5a206d5";
    private static final String EUID_OF_THE_REFERENCED = "01a15da2-21b5-70ae-8f26-a85dd44fa74f";

    private UjpExamples() {
    }

    private static Party sampleTaxpayer() {
        return Party.company("АКЦЕНТ Д.О.О.Е.Л. ЕКСПОРТ-ИМПОРТ СКОПЈЕ", "4030995135699",
                new Address("ОРЦЕ НИКОЛОВ", "133/Д3", "1000", "СКОПЈЕ-ЦЕНТАР"))
                .withVatNumber("МК4030995135699");
    }

    private static Line.Builder testItemBuilder(String unitPrice) {
        return Line.builder()
                .sku("123566554581851685416841685415")
                .senderCode("OC-001")
                .description("TEST OC 0 0 1")
                .unit("par.")
                .quantity("1")
                .unitPrice(unitPrice)
                .standardRate(VatGroup.DDV_A)
                .domesticProduct(true);
    }

    private static Line testItem(String unitPrice) {
        return testItemBuilder(unitPrice).build();
    }

    private static Line wirelessMouse() {
        return Line.builder()
                .sku("123456789012")
                .senderCode("WM-001")
                .description("Wireless Mouse")
                .unit("Piece")
                .quantity("1")
                .unitPrice("29.99")
                .standardRate(VatGroup.DDV_A)
                .domesticProduct(true)
                .build();
    }

    private static Header header(String number, String id, String date, String turnoverDate) {
        Header header = Header.of(number, id, LocalDate.parse(date));
        return turnoverDate == null ? header : header.withTurnoverDate(LocalDate.parse(turnoverDate));
    }

    private static Payment cash() {
        return Payment.of(PaymentType.P_10);
    }

    private static Payment card() {
        return Payment.of(PaymentType.P_11);
    }

    private static Payment noTypeStated() {
        return Payment.of(null);
    }

    private static Document.Builder ordinary100(String number, String id, String turnoverDate) {
        return Document.builder(DocumentKind.INVOICE)
                .header(header(number, id, "2026-10-21", turnoverDate))
                .seller(sampleTaxpayer())
                .buyer(sampleTaxpayer())
                .payment(cash());
    }

    // ---- Kind 100, the ordinary document and what the 21.10 set added to it ----------------------

    /**
     * The 21.10 examples of kind 100 print no request timestamp (the bare body, or a document with a
     * reason beside it), so the timestamp of their goldens is this library's own choice.
     */
    public static final LocalDateTime UNPRINTED_TIMESTAMP = LocalDateTime.parse("2026-10-21T10:00:00");

    /** PDF pages 2-4, "Пример JSON за Општа Фактура". */
    public static Document invoice() {
        return ordinary100("100-2625359277/2026", "2625359277", "2026-10-06").line(testItem("254.24")).build();
    }

    /** PDF pages 4-7, "Пример со повеќе ставки": a DDV-A line of 254.24 and a DDV-B line of 350. */
    public static Document multiItemInvoice() {
        return ordinary100("100-2625359277/2026", "2625359277", "2026-10-06")
                .line(testItem("254.24"))
                .line(Line.builder().sku("222222222222222222222222222222").senderCode("OC-004")
                        .description("TEST 004 OC").unit("litar").quantity("1").unitPrice("350")
                        .standardRate(VatGroup.DDV_B).build())
                .build();
    }

    /**
     * PDF pages 7-9, "Пример со референтен документ": a reference of type SYS to an e-invoice. The PDF
     * prints the name "Фактура" for the type; the code list's name is "Од систем",
     * which is what this library writes.
     */
    public static Document invoiceWithReference() {
        return ordinary100("100-2625359277/2026", "2625359277", "2026-10-06")
                .reference(new Reference(RefDocumentType.SYS, LocalDate.parse("2026-10-21"), EUID_OF_THE_REFERENCED))
                .line(testItem("254.24"))
                .build();
    }

    /**
     * PDF pages 10-12, "Пример со попуст": 10 off the unit price. The PDF also prints
     * docGlobalDiscountAmount 10, which no table documents and the library does not write.
     */
    public static Document invoiceWithDiscount() {
        return ordinary100("100-2625359277/2026", "2625359277", "2026-10-06")
                .line(testItemBuilder("254.24").unitDiscount("10").build())
                .build();
    }

    /** PDF pages 12-14, "Пример со зголемување на износот за плаќање": docAmountIncrease 100. */
    public static Document invoiceWithIncrease() {
        return ordinary100("100-2625359277/2026", "2625359277", "2026-10-06")
                .adjustments(new Adjustments(new BigDecimal("100"), null))
                .line(testItem("254.24"))
                .build();
    }

    /** PDF pages 14-17, "Пример со намалување на износот за плаќање": docAmountDecrease 100. */
    public static Document invoiceWithDecrease() {
        return ordinary100("100-2625359277/2026", "2625359277", "2026-10-06")
                .adjustments(new Adjustments(null, new BigDecimal("100")))
                .line(testItem("254.24"))
                .build();
    }

    /** PDF pages 17-19, "Пример со атрибути на документ". */
    public static Document invoiceWithDocumentAttribute() {
        return ordinary100("100-2625359277/2026", "2625359277", "2026-10-06")
                .attribute(new Attribute("Test Atribut", "123", AttributeType.STR))
                .line(testItem("254.24"))
                .build();
    }

    /** PDF pages 19-22, "Пример со атрибути на ставки". */
    public static Document invoiceWithLineAttribute() {
        return ordinary100("100-2625359277/2026", "2625359277", "2026-10-06")
                .line(testItemBuilder("254.24")
                        .attributes(List.of(new Attribute("Test Atribut", "10", AttributeType.STR))).build())
                .build();
    }

    // ---- Kind 100, storno and correction --------------------------------------------------------

    /** PDF pages 27-29, "Пример за сторно / Прва фактура (оригинална)": the invoice the storno voids. */
    public static Document stornoOriginal() {
        return Document.builder(DocumentKind.INVOICE)
                .header(header("100-2625364731/2026", "2625364731", "2026-10-21", "2026-10-21"))
                .seller(sampleTaxpayer())
                .buyer(sampleTaxpayer())
                .payment(noTypeStated())
                .line(wirelessMouse())
                .build();
    }

    /** PDF pages 29-32, "Пример за сторно / Сторно на првата". */
    public static Document invoiceStorno() {
        return Document.builder(DocumentKind.INVOICE)
                .header(header("100-2625364905/2026", "2625364905", "2026-10-21", "2026-10-21"))
                .mode(new Mode.Storno(VoidReason.S_1, "test"))
                .reference(new Reference(RefDocumentType.ST, LocalDate.parse("2026-10-21"), EUID_OF_THE_STORNOED))
                .seller(sampleTaxpayer())
                .buyer(sampleTaxpayer())
                .payment(noTypeStated())
                .line(wirelessMouse())
                .build();
    }

    /** PDF pages 32-34, "Пример за корекција / Прва фактура": the invoice the correction replaces. */
    public static Document correctionOriginal() {
        return Document.builder(DocumentKind.INVOICE)
                .header(header("100-2625365435/2026", "2625365435", "2026-10-21", "2026-10-21"))
                .seller(sampleTaxpayer())
                .buyer(sampleTaxpayer())
                .payment(cash())
                .line(testItem("254.24"))
                .build();
    }

    /** PDF pages 34-37, "Пример за корекција / Корекција на првата": the quantity is corrected to 2. */
    public static Document invoiceCorrection() {
        return Document.builder(DocumentKind.INVOICE)
                .header(header("100-2625365531/2026", "2625365531", "2026-10-21", "2026-10-21"))
                .mode(new Mode.Correction(CorrectionReason.C02, "test"))
                .reference(new Reference(RefDocumentType.KR, LocalDate.parse("2026-10-21"), EUID_OF_THE_CORRECTED))
                .seller(sampleTaxpayer())
                .buyer(sampleTaxpayer())
                .payment(cash())
                .line(testItemBuilder("254.24").quantity("2").build())
                .build();
    }

    // ---- 110, 120, 130, 310 ---------------------------------------------------------------------

    /**
     * The 110, 120 and 130 examples are the bare body with no requestTimestamp, so the timestamp of
     * their goldens is this library's own choice.
     */
    public static final LocalDateTime BODY_ONLY_TIMESTAMP = LocalDateTime.parse("2026-08-12T10:00:00");

    /** PDF pages 37-39, "Пример за Книжно одобрение". */
    public static Document creditNote() {
        return Document.builder(DocumentKind.CREDIT_NOTE)
                .header(header("110-11644/2026", "11644", "2026-08-12", null))
                .seller(sampleTaxpayer())
                .buyer(sampleTaxpayer())
                .line(testItem("589"))
                .build();
    }

    /** PDF pages 39-41, "Пример за Книжно задолжение". */
    public static Document debitNote() {
        return Document.builder(DocumentKind.DEBIT_NOTE)
                .header(header("120-11730/2026", "11730", "2026-08-12", null))
                .seller(sampleTaxpayer())
                .buyer(sampleTaxpayer())
                .line(testItem("589"))
                .build();
    }

    /** PDF pages 41-43, "Пример за Авансна фактура". */
    public static Document advanceInvoice() {
        return Document.builder(DocumentKind.ADVANCE_INVOICE)
                .header(header("130-11806/2026", "11806", "2026-08-12", null))
                .seller(sampleTaxpayer())
                .buyer(sampleTaxpayer())
                .payment(card())
                .line(testItem("589"))
                .build();
    }

    /** PDF pages 55-57, "Пример за Фактура за регистриран готовински промет". */
    public static final LocalDateTime CASH_TIMESTAMP = LocalDateTime.parse("2026-09-01T12:07:55");

    public static Document cashTurnover() {
        return Document.builder(DocumentKind.CASH_TURNOVER)
                .header(header("310-35256/2026", "35256", "2026-09-01", "2026-09-01"))
                .seller(sampleTaxpayer())
                .buyer(sampleTaxpayer())
                .payment(card())
                .line(testItem("254.24"))
                .build();
    }
}
