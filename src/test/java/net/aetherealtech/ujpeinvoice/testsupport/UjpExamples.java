package net.aetherealtech.ujpeinvoice.testsupport;

import net.aetherealtech.ujpeinvoice.model.Address;
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
import net.aetherealtech.ujpeinvoice.model.Subsidiary;
import net.aetherealtech.ujpeinvoice.model.TaxIndicators;
import net.aetherealtech.ujpeinvoice.model.VatGroup;
import net.aetherealtech.ujpeinvoice.model.VoidReason;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * The values of the worked examples in UJP's primer_za_json_08.10.pdf (Last-Modified 2026-10-08),
 * one document per scoped kind and mode, built through this library's own model. Only the inputs are
 * here: the amounts the model derives are asserted against the numbers the PDF prints, in
 * {@code UjpExamplesTest}. The PDF's JSON is not committed.
 *
 * <p>The parties are the sample taxpayer of UJP's own examples, who is both seller and buyer there.
 */
public final class UjpExamples {

    private static final String EUID_OF_THE_ORIGINAL = "019dd423-bef7-73b7-abe6-f69721682406";

    private UjpExamples() {
    }

    private static Party sampleTaxpayer() {
        return Party.company("АКЦЕНТ Д.О.О.Е.Л. ЕКСПОРТ-ИМПОРТ СКОПЈЕ", "4030995135699",
                new Address("ОРЦЕ НИКОЛОВ", "133/Д3", "1000", "СКОПЈЕ-ЦЕНТАР"))
                .withVatNumber("МК4030995135699");
    }

    private static Line sampleLine(String unitPrice, VatGroup group) {
        return Line.builder()
                .sku("54321")
                .senderCode("54321")
                .description("Opis5432")
                .unit("kg")
                .quantity("1")
                .unitPrice(unitPrice)
                .standardRate(group)
                .build();
    }

    private static Line testItem(String unitPrice) {
        return Line.builder()
                .sku("123566554581851685416841685415")
                .senderCode("OC-001")
                .description("TEST OC 0 0 1")
                .unit("par.")
                .quantity("1")
                .unitPrice(unitPrice)
                .standardRate(VatGroup.DDV_A)
                .domesticProduct(true)
                .build();
    }

    private static Header header(String number, String id, String date, String turnoverDate) {
        Header header = Header.of(number, id, LocalDate.parse(date));
        return turnoverDate == null ? header : header.withTurnoverDate(LocalDate.parse(turnoverDate));
    }

    private static Payment card() {
        return Payment.of(PaymentType.P_11);
    }

    private static Reference theOriginal() {
        return new Reference(RefDocumentType.ST, LocalDate.parse("2025-12-11"), EUID_OF_THE_ORIGINAL);
    }

    // ---- Kind 100 -------------------------------------------------------------------------------

    /** PDF pages 2-4, "Пример JSON за Општа Фактура". */
    public static final LocalDateTime INVOICE_TIMESTAMP = LocalDateTime.parse("2026-04-27T11:24:10");

    public static Document invoice() {
        return Document.builder(DocumentKind.INVOICE)
                .header(header("f52368c0-10a6-4cfd-9", "13184", "2026-04-22", "2026-04-22"))
                .seller(sampleTaxpayer())
                .buyer(sampleTaxpayer())
                .payment(card())
                .line(sampleLine("95.2381", VatGroup.DDV_B))
                .build();
    }

    /**
     * PDF pages 4-8, "Пример со повеќе ставки": four lines in the four groups. Its DDV-G line carries a
     * free-text note in the PDF; the library takes the note from its catalogue.
     */
    public static Document multiItemInvoice() {
        return Document.builder(DocumentKind.INVOICE)
                .header(header("03ccfe89-a1dd-4f06-8", "112808", "2026-04-28", "2026-04-28"))
                .seller(sampleTaxpayer())
                .buyer(sampleTaxpayer())
                .payment(card())
                .line(sampleLine("100", VatGroup.DDV_B))
                .line(Line.builder().sku("708090").senderCode("70809").description("Opis 708090").unit("parce")
                        .quantity("1").unitPrice("1694.9153").standardRate(VatGroup.DDV_A).build())
                .line(Line.builder().sku("Opis12345").senderCode("Opis1").description("12345").unit("parce")
                        .quantity("1").unitPrice("1111").standardRate(VatGroup.DDV_V).build())
                .line(Line.builder().sku("123456").senderCode("12345").description("Opis123456").unit("par.")
                        .quantity("1").unitPrice("90").tax(VatGroup.DDV_G,
                                TaxIndicators.SNAPSHOT_2026_08_27.get("DDV-G"))
                        .build())
                .build();
    }

    /** PDF pages 15-18, "Пример за сторно / Сторно на првата". */
    public static final LocalDateTime STORNO_TIMESTAMP = LocalDateTime.parse("2026-04-28T15:05:10");

    public static Document invoiceStorno() {
        return Document.builder(DocumentKind.INVOICE)
                .header(header("f52368c0-10a6-4cfd-9", "1311", "2026-04-22", "2026-04-22"))
                .mode(new Mode.Storno(VoidReason.S_1, "test"))
                .reference(theOriginal())
                .seller(sampleTaxpayer().withSubsidiary(new Subsidiary(null, null, "Адреса за тестирање1, "))
                        .withContact("02/3133-122").withEmail("daniel.rikaloski@accent.mk"))
                .buyer(sampleTaxpayer().withSubsidiary(new Subsidiary(null, null, "ОРЦЕ НИКОЛОВ 133/Д3, Скопје"))
                        .withContact("02/3133-122").withEmail("daniel.rikaloski@accent.mk"))
                .payment(card())
                .line(sampleLine("95.2381", VatGroup.DDV_B))
                .build();
    }

    /** PDF pages 20-23, "Пример за корекција / Корекција на првата". */
    public static final LocalDateTime CORRECTION_TIMESTAMP = LocalDateTime.parse("2026-04-28T14:54:10");

    public static Document invoiceCorrection() {
        return Document.builder(DocumentKind.INVOICE)
                .header(header("f52368c0-10a6-4cfd-9", "1318-1", "2026-04-22", "2026-04-22"))
                .mode(new Mode.Correction(CorrectionReason.C01, null))
                .reference(theOriginal())
                .seller(sampleTaxpayer())
                .buyer(sampleTaxpayer())
                .payment(card())
                .line(sampleLine("95.2381", VatGroup.DDV_B))
                .build();
    }

    // ---- 110, 120, 130, 310 ---------------------------------------------------------------------

    /**
     * The 110, 120 and 130 examples are the bare body with no requestTimestamp, so the timestamp of
     * their goldens is this library's own choice.
     */
    public static final LocalDateTime BODY_ONLY_TIMESTAMP = LocalDateTime.parse("2026-08-12T10:00:00");

    /** PDF pages 23-25, "Пример за Книжно одобрение". */
    public static Document creditNote() {
        return Document.builder(DocumentKind.CREDIT_NOTE)
                .header(header("110-11644/2026", "11644", "2026-08-12", null))
                .seller(sampleTaxpayer())
                .buyer(sampleTaxpayer())
                .line(testItem("589"))
                .build();
    }

    /** PDF pages 25-27, "Пример за Книжно задолжение". */
    public static Document debitNote() {
        return Document.builder(DocumentKind.DEBIT_NOTE)
                .header(header("120-11730/2026", "11730", "2026-08-12", null))
                .seller(sampleTaxpayer())
                .buyer(sampleTaxpayer())
                .line(testItem("589"))
                .build();
    }

    /** PDF pages 27-29, "Пример за Авансна фактура". */
    public static Document advanceInvoice() {
        return Document.builder(DocumentKind.ADVANCE_INVOICE)
                .header(header("130-11806/2026", "11806", "2026-08-12", null))
                .seller(sampleTaxpayer())
                .buyer(sampleTaxpayer())
                .payment(card())
                .line(testItem("589"))
                .build();
    }

    /** PDF pages 41-43, "Пример за Фактура за регистриран готовински промет". */
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
