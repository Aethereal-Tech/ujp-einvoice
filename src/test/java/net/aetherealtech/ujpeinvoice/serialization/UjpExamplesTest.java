package net.aetherealtech.ujpeinvoice.serialization;

import net.aetherealtech.ujpeinvoice.internal.json.JsonArray;
import net.aetherealtech.ujpeinvoice.internal.json.JsonBoolean;
import net.aetherealtech.ujpeinvoice.internal.json.JsonNull;
import net.aetherealtech.ujpeinvoice.internal.json.JsonObject;
import net.aetherealtech.ujpeinvoice.internal.json.JsonParser;
import net.aetherealtech.ujpeinvoice.model.Document;
import net.aetherealtech.ujpeinvoice.model.DocumentTotals;
import net.aetherealtech.ujpeinvoice.model.Line;
import net.aetherealtech.ujpeinvoice.model.VatTotal;
import net.aetherealtech.ujpeinvoice.testsupport.UjpExamples;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The worked examples of UJP's primer_za_json_08.10.pdf, rebuilt from their values through this
 * library's model. Each example has two assertions: every amount the model derives equals the number
 * the PDF prints for it, and the serialized document equals a golden file this library produced.
 *
 * <p>The goldens are this library's output, not UJP's JSON: they differ from the PDF in the members
 * decision 6 of wire-layer-correction leaves out and in the ones the type pages add (see
 * {@link Differences}). The 0.1.0 byte-for-byte guarantee ended with the model. Regenerate a golden
 * deliberately, after reading the diff: {@code ./mvnw test -Dgolden.regenerate=true}.
 */
class UjpExamplesTest {

    private static final UjpJsonSerializer SERIALIZER = new UjpJsonSerializer();

    /** What the PDF prints for one line: unit price after discount, unit VAT, and the four totals. */
    private record PrintedLine(String unitPriceAfterDiscount, String unitVat, String totalBeforeDiscount,
                               String total, String totalVat, String totalWithVat) {
    }

    /** What the PDF prints in docTotals. */
    private record PrintedTotals(String net, String discount, String netAfterDiscount, String vat, String gross,
                                 String grossRounded, String advance, String finalAmount) {
    }

    /** What the PDF prints in one vatTotals entry. */
    private record PrintedVat(String indicator, String percent, String taxable, String vat, String total) {
    }

    @Test
    void invoiceOrdinary() {
        // PDF pages 2-4.
        verify(UjpExamples.invoice(), UjpExamples.INVOICE_TIMESTAMP, "invoice-ordinary.json",
                List.of(new PrintedLine("95.2381", "4.7619", "95.2381", "95.2381", "4.7619", "100")),
                new PrintedTotals("95.24", "0", "95.24", "4.76", "100", "100", "0", "100"),
                List.of(new PrintedVat("DDV-B", "5", "95.24", "4.76", "100")));
    }

    @Test
    void invoiceStorno() {
        // PDF pages 15-18: the quantity is -1, the unit values stay positive, every total is negative.
        verify(UjpExamples.invoiceStorno(), UjpExamples.STORNO_TIMESTAMP, "invoice-storno.json",
                List.of(new PrintedLine("95.2381", "4.7619", "-95.2381", "-95.2381", "-4.7619", "-100")),
                new PrintedTotals("-95.24", "0", "-95.24", "-4.76", "-100", "-100", "0", "-100"),
                List.of(new PrintedVat("DDV-B", "5", "-95.24", "-4.76", "-100")));
    }

    @Test
    void invoiceCorrection() {
        // PDF pages 20-23: a replacement document with positive amounts.
        verify(UjpExamples.invoiceCorrection(), UjpExamples.CORRECTION_TIMESTAMP, "invoice-correction.json",
                List.of(new PrintedLine("95.2381", "4.7619", "95.2381", "95.2381", "4.7619", "100")),
                new PrintedTotals("95.24", "0", "95.24", "4.76", "100", "100", "0", "100"),
                List.of(new PrintedVat("DDV-B", "5", "95.24", "4.76", "100")));
    }

    @Test
    void multiItemInvoice() {
        // PDF pages 4-8: the four groups in one invoice; 2000.0001 and 3417.1001 are the unrounded
        // sums that make the summed-then-rounded rule visible.
        verify(UjpExamples.multiItemInvoice(), UjpExamples.INVOICE_TIMESTAMP, "invoice-multi-item.json",
                List.of(new PrintedLine("100", "5", "100", "100", "5", "105"),
                        new PrintedLine("1694.9153", "305.0848", "1694.9153", "1694.9153", "305.0848", "2000.0001"),
                        new PrintedLine("1111", "111.1", "1111", "1111", "111.1", "1222.1"),
                        new PrintedLine("90", "0", "90", "90", "0", "90")),
                new PrintedTotals("2995.92", "0", "2995.92", "421.18", "3417.1", "3417", "0", "3417"),
                List.of(new PrintedVat("DDV-B", "5", "100", "5", "105"),
                        new PrintedVat("DDV-A", "18", "1694.92", "305.08", "2000"),
                        new PrintedVat("DDV-V", "10", "1111", "111.1", "1222.1"),
                        new PrintedVat("DDV-G", "0", "90", "0", "90")));
    }

    @Test
    void creditNote() {
        // PDF pages 23-25.
        verify(UjpExamples.creditNote(), UjpExamples.BODY_ONLY_TIMESTAMP, "credit-note.json",
                List.of(new PrintedLine("589", "106.02", "589", "589", "106.02", "695.02")),
                new PrintedTotals("589", "0", "589", "106.02", "695.02", "695", "0", "695"),
                List.of(new PrintedVat("DDV-A", "18", "589", "106.02", "695.02")));
    }

    @Test
    void debitNote() {
        // PDF pages 25-27.
        verify(UjpExamples.debitNote(), UjpExamples.BODY_ONLY_TIMESTAMP, "debit-note.json",
                List.of(new PrintedLine("589", "106.02", "589", "589", "106.02", "695.02")),
                new PrintedTotals("589", "0", "589", "106.02", "695.02", "695", "0", "695"),
                List.of(new PrintedVat("DDV-A", "18", "589", "106.02", "695.02")));
    }

    @Test
    void advanceInvoice() {
        // PDF pages 27-29.
        verify(UjpExamples.advanceInvoice(), UjpExamples.BODY_ONLY_TIMESTAMP, "advance-invoice.json",
                List.of(new PrintedLine("589", "106.02", "589", "589", "106.02", "695.02")),
                new PrintedTotals("589", "0", "589", "106.02", "695.02", "695", "0", "695"),
                List.of(new PrintedVat("DDV-A", "18", "589", "106.02", "695.02")));
    }

    @Test
    void cashTurnover() {
        // PDF pages 41-43: 300.0032 is printed on the line and 300 in the totals.
        verify(UjpExamples.cashTurnover(), UjpExamples.CASH_TIMESTAMP, "cash-turnover.json",
                List.of(new PrintedLine("254.24", "45.7632", "254.24", "254.24", "45.7632", "300.0032")),
                new PrintedTotals("254.24", "0", "254.24", "45.76", "300", "300", "0", "300"),
                List.of(new PrintedVat("DDV-A", "18", "254.24", "45.76", "300")));
    }

    private static void verify(Document document, LocalDateTime timestamp, String golden,
                               List<PrintedLine> lines, PrintedTotals totals, List<PrintedVat> vatTotals) {
        assertDerivedAmounts(document, lines, totals, vatTotals);
        assertThat(serialize(document, timestamp)).isEqualTo(golden(golden, serialize(document, timestamp)));
    }

    private static void assertDerivedAmounts(Document document, List<PrintedLine> lines, PrintedTotals totals,
                                             List<PrintedVat> vatTotals) {
        assertThat(document.effectiveLines()).hasSameSizeAs(lines);
        for (int index = 0; index < lines.size(); index++) {
            Line line = document.effectiveLines().get(index);
            PrintedLine printed = lines.get(index);
            assertThat(line.unitPriceAfterDiscount()).as("unit price after discount").isEqualByComparingTo(printed.unitPriceAfterDiscount());
            assertThat(line.unitVat()).as("unit VAT").isEqualByComparingTo(printed.unitVat());
            assertThat(line.totalBeforeDiscount()).as("total before discount").isEqualByComparingTo(printed.totalBeforeDiscount());
            assertThat(line.total()).as("line total").isEqualByComparingTo(printed.total());
            assertThat(line.totalVat()).as("line VAT").isEqualByComparingTo(printed.totalVat());
            assertThat(line.totalWithVat()).as("line total with VAT").isEqualByComparingTo(printed.totalWithVat());
        }
        DocumentTotals actual = document.totals();
        assertThat(actual.net()).as("docNetAmount").isEqualByComparingTo(totals.net());
        assertThat(actual.discount()).as("docDiscountAmount").isEqualByComparingTo(totals.discount());
        assertThat(actual.netAfterDiscount()).as("docNetAmountDisc").isEqualByComparingTo(totals.netAfterDiscount());
        assertThat(actual.vat()).as("docVatAmount").isEqualByComparingTo(totals.vat());
        assertThat(actual.gross()).as("docGrossAmount").isEqualByComparingTo(totals.gross());
        assertThat(actual.grossRounded()).as("docGrossAmountR").isEqualByComparingTo(totals.grossRounded());
        assertThat(actual.advanceAmount()).as("docAvansAmount").isEqualByComparingTo(totals.advance());
        assertThat(actual.finalAmount()).as("docFinalAmount").isEqualByComparingTo(totals.finalAmount());
        List<VatTotal> actualVat = document.vatTotals();
        assertThat(actualVat).hasSameSizeAs(vatTotals);
        for (int index = 0; index < vatTotals.size(); index++) {
            VatTotal total = actualVat.get(index);
            PrintedVat printed = vatTotals.get(index);
            assertThat(total.indicator().code()).isEqualTo(printed.indicator());
            assertThat(total.group().percent()).isEqualByComparingTo(printed.percent());
            assertThat(total.taxable()).as("vatTaxableAmount").isEqualByComparingTo(printed.taxable());
            assertThat(total.vat()).as("vatAmount").isEqualByComparingTo(printed.vat());
            assertThat(total.total()).as("vatTotalAmount").isEqualByComparingTo(printed.total());
        }
    }

    private static String serialize(Document document, LocalDateTime timestamp) {
        return new String(SERIALIZER.serialize(document, timestamp), StandardCharsets.UTF_8);
    }

    private static String golden(String name, String actual) {
        if (Boolean.getBoolean("golden.regenerate")) {
            try {
                Files.writeString(Path.of("src/test/resources/golden", name), actual + "\n", StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return actual;
        }
        try (InputStream in = UjpExamplesTest.class.getResourceAsStream("/golden/" + name)) {
            if (in == null) {
                throw new IllegalStateException("Missing golden file: golden/" + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).stripTrailing();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Where the output deliberately differs from UJP's own examples. Each assertion states what the
     * PDF prints, so the day a difference stops being deliberate this class says which one.
     */
    @Nested
    class Differences {

        private JsonObject root(Document document, LocalDateTime timestamp) {
            return (JsonObject) JsonParser.parse(serialize(document, timestamp));
        }

        private JsonObject document(Document document, LocalDateTime timestamp) {
            return root(document, timestamp).getObject("document");
        }

        @Test
        void nothingUndocumentedIsWritten() {
            // Decision 6: source, a trailing requestTimestamp, the *EinvoiceUser pair,
            // docGlobalDiscountAmount, docItemUnitItemDiscountAmount appear in some examples and in no
            // table; docItemHs appears in the 110 to 310 examples and in no table.
            for (Document example : List.of(UjpExamples.invoice(), UjpExamples.invoiceStorno(),
                    UjpExamples.invoiceCorrection(), UjpExamples.creditNote(), UjpExamples.debitNote(),
                    UjpExamples.advanceInvoice(), UjpExamples.cashTurnover())) {
                String json = serialize(example, UjpExamples.INVOICE_TIMESTAMP);
                assertThat(json).doesNotContain("\"source\"", "EinvoiceUser", "docGlobalDiscountAmount",
                        "docItemUnitItemDiscountAmount", "docItemHs");
                assertThat(json.indexOf("requestTimestamp")).isEqualTo(json.lastIndexOf("requestTimestamp"));
            }
        }

        @Test
        void theEnvelopeIsWrittenForEveryKind() {
            // The 110, 120 and 130 examples print the bare body; the signed payload nests under document.
            for (Document example : List.of(UjpExamples.creditNote(), UjpExamples.debitNote(),
                    UjpExamples.advanceInvoice())) {
                JsonObject root = root(example, UjpExamples.BODY_ONLY_TIMESTAMP);
                assertThat(root.members().keySet()).containsExactly("requestTimestamp", "document");
                assertThat(root.getString("requestTimestamp")).isEqualTo("2026-08-12T10:00:00");
            }
        }

        @Test
        void theReasonObjectIsABrotherOfDocumentAndOnlyTheOneThatApplies() {
            // PDF pp. 15-18 and 20-23: voidReason on a storno, correctionReason on a correction. The 310
            // example writes both as null on an ordinary document; the 100 examples write neither.
            assertThat(root(UjpExamples.invoiceStorno(), UjpExamples.STORNO_TIMESTAMP).members().keySet())
                    .containsExactly("requestTimestamp", "document", "voidReason");
            assertThat(root(UjpExamples.invoiceCorrection(), UjpExamples.CORRECTION_TIMESTAMP).members().keySet())
                    .containsExactly("requestTimestamp", "document", "correctionReason");
            assertThat(root(UjpExamples.cashTurnover(), UjpExamples.CASH_TIMESTAMP).members().keySet())
                    .containsExactly("requestTimestamp", "document");
        }

        @Test
        void kind100WritesNoCurrencyDateOrRate() {
            // The 08.10 examples carry both; the 15.10 changelog removed them from 100.
            JsonObject payment = document(UjpExamples.invoice(), UjpExamples.INVOICE_TIMESTAMP).getObject("docPayment");
            assertThat(payment.members()).doesNotContainKeys("docCurrencyDate", "docCurrencyExchRate");
            assertThat(payment.getString("docCurrencyCode")).isEqualTo("MKD");
        }

        @Test
        void kinds130And310WriteTheDocumentDateAndARateOfOne() {
            // PDF pp. 27-29 and 41-43 print both, and the tables of 130 and 310 list them.
            JsonObject advance = document(UjpExamples.advanceInvoice(), UjpExamples.BODY_ONLY_TIMESTAMP)
                    .getObject("docPayment");
            assertThat(advance.getString("docCurrencyDate")).isEqualTo("2026-08-12");
            assertThat(advance.getNumber("docCurrencyExchRate")).isEqualByComparingTo("1");
            JsonObject cash = document(UjpExamples.cashTurnover(), UjpExamples.CASH_TIMESTAMP).getObject("docPayment");
            assertThat(cash.getString("docCurrencyDate")).isEqualTo("2026-09-01");
            assertThat(cash.getNumber("docCurrencyExchRate")).isEqualByComparingTo("1");
        }

        @Test
        void kinds110And120WriteNoPaymentGroup() {
            // Neither their tables nor their examples have one.
            assertThat(document(UjpExamples.creditNote(), UjpExamples.BODY_ONLY_TIMESTAMP).members())
                    .doesNotContainKey("docPayment");
            assertThat(document(UjpExamples.debitNote(), UjpExamples.BODY_ONLY_TIMESTAMP).members())
                    .doesNotContainKey("docPayment");
        }

        @Test
        void docAltIsAlwaysWrittenAsFalseUnlessSet() {
            // The tables mark it mandatory on all five kinds; only the 100 exempt example of 07.10 carries
            // it, as false. The other examples omit it.
            for (Document example : List.of(UjpExamples.invoice(), UjpExamples.creditNote(),
                    UjpExamples.cashTurnover())) {
                JsonObject header = document(example, UjpExamples.INVOICE_TIMESTAMP).getObject("header");
                assertThat(header.get("docAlt")).contains(JsonBoolean.of(false));
            }
        }

        @Test
        void theNoteOfAStandardIndicatorIsNullNotAnEmptyString() {
            // The 100 examples print "" for vatTaxIndicatorNote; the 110, 130 and 310 examples print null.
            JsonObject vat = (JsonObject) document(UjpExamples.invoice(), UjpExamples.INVOICE_TIMESTAMP)
                    .getArray("vatTotals").items().getFirst();
            assertThat(vat.get("vatTaxIndicatorNote")).contains(JsonNull.INSTANCE);
        }

        @Test
        void aNonStandardIndicatorsNoteComesFromTheCatalogueNotFromFreeText() {
            // PDF pp. 4-8 print "не е ДДВ обврзник" for the DDV-G line. Decision 4: the note is the
            // snapshot's, and a caller who wants other words constructs the TaxIndicator.
            JsonArray vatTotals = document(UjpExamples.multiItemInvoice(), UjpExamples.INVOICE_TIMESTAMP)
                    .getArray("vatTotals");
            JsonObject exempt = (JsonObject) vatTotals.items().get(3);

            assertThat(exempt.getString("vatTaxIndicator")).isEqualTo("DDV-G");
            assertThat(exempt.getString("vatTaxIndicatorNote"))
                    .isEqualTo("ДДВ не е пресметан согласно член 51 став 3 од ЗДДВ");
        }

        @Test
        void numbersCarryNoTrailingZeros() {
            // The examples print 100, 589 and 3417.1; the model's scale-4 values would print 100.0000.
            JsonObject totals = document(UjpExamples.multiItemInvoice(), UjpExamples.INVOICE_TIMESTAMP)
                    .getObject("docTotals");
            assertThat(serialize(UjpExamples.multiItemInvoice(), UjpExamples.INVOICE_TIMESTAMP))
                    .contains("\"docGrossAmount\":3417.1,", "\"docGrossAmountR\":3417,", "\"docItemQty\":1,")
                    .doesNotContain("3417.10", "100.0000");
            assertThat(totals.getNumber("docNetAmount")).isEqualByComparingTo(new BigDecimal("2995.92"));
        }
    }
}
