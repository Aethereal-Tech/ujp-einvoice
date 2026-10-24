package net.aetherealtech.ujpeinvoice.serialization;

import net.aetherealtech.ujpeinvoice.internal.json.JsonBoolean;
import net.aetherealtech.ujpeinvoice.internal.json.JsonNull;
import net.aetherealtech.ujpeinvoice.internal.json.JsonObject;
import net.aetherealtech.ujpeinvoice.internal.json.JsonParser;
import net.aetherealtech.ujpeinvoice.model.Document;
import net.aetherealtech.ujpeinvoice.model.DocumentKind;
import net.aetherealtech.ujpeinvoice.model.DocumentTotals;
import net.aetherealtech.ujpeinvoice.model.Header;
import net.aetherealtech.ujpeinvoice.model.Line;
import net.aetherealtech.ujpeinvoice.model.TaxIndicators;
import net.aetherealtech.ujpeinvoice.model.VatGroup;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The worked examples of UJP's primer_za_json_21.10.pdf, rebuilt from their values through this
 * library's model. Each example has two assertions: every amount the model derives equals the number
 * the PDF prints for it, and the serialized document equals a golden file this library produced.
 * Page numbers are the PDF's physical pages (87 pages, Last-Modified 2026-10-22).
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

    /** What the PDF prints in docTotals; an increase or decrease the example does not carry is 0. */
    private record PrintedTotals(String net, String discount, String netAfterDiscount, String vat, String gross,
                                 String grossRounded, String advance, String increase, String decrease,
                                 String finalAmount) {
    }

    /** What the PDF prints in one vatTotals entry. */
    private record PrintedVat(String indicator, String percent, String taxable, String vat, String total) {
    }

    @Test
    void invoiceOrdinary() {
        // PDF pages 2-4.
        verify(UjpExamples.invoice(), UjpExamples.UNPRINTED_TIMESTAMP, "invoice-ordinary.json",
                List.of(new PrintedLine("254.24", "45.7632", "254.24", "254.24", "45.7632", "300.0032")),
                new PrintedTotals("254.24", "0", "254.24", "45.76", "300", "300", "0", "0", "0", "300"),
                List.of(new PrintedVat("DDV-A", "18", "254.24", "45.76", "300")));
    }

    @Test
    void multiItemInvoice() {
        // PDF pages 4-7: two groups. The gross is printed 667.5 from an unrounded 667.5032, and 668 is the
        // rounded gross under half-up and under banking rounding alike, so it settles nothing.
        verify(UjpExamples.multiItemInvoice(), UjpExamples.UNPRINTED_TIMESTAMP, "invoice-multi-item.json",
                List.of(new PrintedLine("254.24", "45.7632", "254.24", "254.24", "45.7632", "300.0032"),
                        new PrintedLine("350", "17.5", "350", "350", "17.5", "367.5")),
                new PrintedTotals("604.24", "0", "604.24", "63.26", "667.5", "668", "0", "0", "0", "668"),
                List.of(new PrintedVat("DDV-A", "18", "254.24", "45.76", "300"),
                        new PrintedVat("DDV-B", "5", "350", "17.5", "367.5")));
    }

    @Test
    void invoiceWithReference() {
        // PDF pages 7-9: one reference of type SYS; the amounts are those of the ordinary invoice.
        verify(UjpExamples.invoiceWithReference(), UjpExamples.UNPRINTED_TIMESTAMP, "invoice-reference.json",
                List.of(new PrintedLine("254.24", "45.7632", "254.24", "254.24", "45.7632", "300.0032")),
                new PrintedTotals("254.24", "0", "254.24", "45.76", "300", "300", "0", "0", "0", "300"),
                List.of(new PrintedVat("DDV-A", "18", "254.24", "45.76", "300")));
    }

    @Test
    void invoiceWithDiscount() {
        // PDF pages 10-12: 10 off the unit price; the net is taken before it, the VAT after it.
        verify(UjpExamples.invoiceWithDiscount(), UjpExamples.UNPRINTED_TIMESTAMP, "invoice-discount.json",
                List.of(new PrintedLine("244.24", "43.9632", "254.24", "244.24", "43.9632", "288.2032")),
                new PrintedTotals("254.24", "10", "244.24", "43.96", "288.2", "288", "0", "0", "0", "288"),
                List.of(new PrintedVat("DDV-A", "18", "244.24", "43.96", "288.2")));
    }

    @Test
    void invoiceWithIncreaseOfTheAmountToPay() {
        // PDF pages 12-14: 300 + 100 = 400, and the VAT base does not move.
        verify(UjpExamples.invoiceWithIncrease(), UjpExamples.UNPRINTED_TIMESTAMP, "invoice-increase.json",
                List.of(new PrintedLine("254.24", "45.7632", "254.24", "254.24", "45.7632", "300.0032")),
                new PrintedTotals("254.24", "0", "254.24", "45.76", "300", "300", "0", "100", "0", "400"),
                List.of(new PrintedVat("DDV-A", "18", "254.24", "45.76", "300")));
    }

    @Test
    void invoiceWithDecreaseOfTheAmountToPay() {
        // PDF pages 14-17: 300 - 100 = 200.
        verify(UjpExamples.invoiceWithDecrease(), UjpExamples.UNPRINTED_TIMESTAMP, "invoice-decrease.json",
                List.of(new PrintedLine("254.24", "45.7632", "254.24", "254.24", "45.7632", "300.0032")),
                new PrintedTotals("254.24", "0", "254.24", "45.76", "300", "300", "0", "0", "100", "200"),
                List.of(new PrintedVat("DDV-A", "18", "254.24", "45.76", "300")));
    }

    @Test
    void invoiceWithADocumentAttribute() {
        // PDF pages 17-19: one STR attribute; the amounts are those of the ordinary invoice.
        verify(UjpExamples.invoiceWithDocumentAttribute(), UjpExamples.UNPRINTED_TIMESTAMP,
                "invoice-document-attribute.json",
                List.of(new PrintedLine("254.24", "45.7632", "254.24", "254.24", "45.7632", "300.0032")),
                new PrintedTotals("254.24", "0", "254.24", "45.76", "300", "300", "0", "0", "0", "300"),
                List.of(new PrintedVat("DDV-A", "18", "254.24", "45.76", "300")));
    }

    @Test
    void invoiceWithALineAttribute() {
        // PDF pages 19-22: one STR attribute on the line.
        verify(UjpExamples.invoiceWithLineAttribute(), UjpExamples.UNPRINTED_TIMESTAMP,
                "invoice-line-attribute.json",
                List.of(new PrintedLine("254.24", "45.7632", "254.24", "254.24", "45.7632", "300.0032")),
                new PrintedTotals("254.24", "0", "254.24", "45.76", "300", "300", "0", "0", "0", "300"),
                List.of(new PrintedVat("DDV-A", "18", "254.24", "45.76", "300")));
    }

    @Test
    void stornoOriginal() {
        // PDF pages 27-29: 35.3882 on the line, 35.39 in the gross, 35 rounded.
        verify(UjpExamples.stornoOriginal(), UjpExamples.UNPRINTED_TIMESTAMP, "invoice-storno-original.json",
                List.of(new PrintedLine("29.99", "5.3982", "29.99", "29.99", "5.3982", "35.3882")),
                new PrintedTotals("29.99", "0", "29.99", "5.4", "35.39", "35", "0", "0", "0", "35"),
                List.of(new PrintedVat("DDV-A", "18", "29.99", "5.4", "35.39")));
    }

    @Test
    void invoiceStorno() {
        // PDF pages 29-32: the quantity is -1, the unit values stay positive, every total is negative.
        verify(UjpExamples.invoiceStorno(), UjpExamples.UNPRINTED_TIMESTAMP, "invoice-storno.json",
                List.of(new PrintedLine("29.99", "5.3982", "-29.99", "-29.99", "-5.3982", "-35.3882")),
                new PrintedTotals("-29.99", "0", "-29.99", "-5.4", "-35.39", "-35", "0", "0", "0", "-35"),
                List.of(new PrintedVat("DDV-A", "18", "-29.99", "-5.4", "-35.39")));
    }

    @Test
    void correctionOriginal() {
        // PDF pages 32-34.
        verify(UjpExamples.correctionOriginal(), UjpExamples.UNPRINTED_TIMESTAMP, "invoice-correction-original.json",
                List.of(new PrintedLine("254.24", "45.7632", "254.24", "254.24", "45.7632", "300.0032")),
                new PrintedTotals("254.24", "0", "254.24", "45.76", "300", "300", "0", "0", "0", "300"),
                List.of(new PrintedVat("DDV-A", "18", "254.24", "45.76", "300")));
    }

    @Test
    void invoiceCorrection() {
        // PDF pages 34-37: a replacement document, the quantity corrected to 2. 91.5264 on the line is 91.53
        // in the totals and 508.48 + 91.53 = 600.01 in the gross, which is rounded to 600.
        verify(UjpExamples.invoiceCorrection(), UjpExamples.UNPRINTED_TIMESTAMP, "invoice-correction.json",
                List.of(new PrintedLine("254.24", "45.7632", "508.48", "508.48", "91.5264", "600.0064")),
                new PrintedTotals("508.48", "0", "508.48", "91.53", "600.01", "600", "0", "0", "0", "600"),
                List.of(new PrintedVat("DDV-A", "18", "508.48", "91.53", "600.01")));
    }

    @Test
    void creditNote() {
        // PDF pages 37-39: identical to the 08.10 example (pages 23-25).
        verify(UjpExamples.creditNote(), UjpExamples.BODY_ONLY_TIMESTAMP, "credit-note.json",
                List.of(new PrintedLine("589", "106.02", "589", "589", "106.02", "695.02")),
                new PrintedTotals("589", "0", "589", "106.02", "695.02", "695", "0", "0", "0", "695"),
                List.of(new PrintedVat("DDV-A", "18", "589", "106.02", "695.02")));
    }

    @Test
    void debitNote() {
        // PDF pages 39-41: identical to the 08.10 example (pages 25-27).
        verify(UjpExamples.debitNote(), UjpExamples.BODY_ONLY_TIMESTAMP, "debit-note.json",
                List.of(new PrintedLine("589", "106.02", "589", "589", "106.02", "695.02")),
                new PrintedTotals("589", "0", "589", "106.02", "695.02", "695", "0", "0", "0", "695"),
                List.of(new PrintedVat("DDV-A", "18", "589", "106.02", "695.02")));
    }

    @Test
    void advanceInvoice() {
        // PDF pages 41-43: identical to the 08.10 example (pages 27-29).
        verify(UjpExamples.advanceInvoice(), UjpExamples.BODY_ONLY_TIMESTAMP, "advance-invoice.json",
                List.of(new PrintedLine("589", "106.02", "589", "589", "106.02", "695.02")),
                new PrintedTotals("589", "0", "589", "106.02", "695.02", "695", "0", "0", "0", "695"),
                List.of(new PrintedVat("DDV-A", "18", "589", "106.02", "695.02")));
    }

    @Test
    void cashTurnover() {
        // PDF pages 55-57: identical to the 08.10 example (pages 41-43). 300.0032 is printed on the line and
        // 300 in the totals.
        verify(UjpExamples.cashTurnover(), UjpExamples.CASH_TIMESTAMP, "cash-turnover.json",
                List.of(new PrintedLine("254.24", "45.7632", "254.24", "254.24", "45.7632", "300.0032")),
                new PrintedTotals("254.24", "0", "254.24", "45.76", "300", "300", "0", "0", "0", "300"),
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
        assertThat(actual.increase()).as("docAmountIncrease").isEqualByComparingTo(totals.increase());
        assertThat(actual.decrease()).as("docAmountDecrease").isEqualByComparingTo(totals.decrease());
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

        private final List<Document> everyExample = List.of(UjpExamples.invoice(), UjpExamples.multiItemInvoice(),
                UjpExamples.invoiceWithReference(), UjpExamples.invoiceWithDiscount(),
                UjpExamples.invoiceWithIncrease(), UjpExamples.invoiceWithDecrease(),
                UjpExamples.invoiceWithDocumentAttribute(), UjpExamples.invoiceWithLineAttribute(),
                UjpExamples.stornoOriginal(), UjpExamples.invoiceStorno(), UjpExamples.correctionOriginal(),
                UjpExamples.invoiceCorrection(), UjpExamples.creditNote(), UjpExamples.debitNote(),
                UjpExamples.advanceInvoice(), UjpExamples.cashTurnover());

        private JsonObject root(Document document, LocalDateTime timestamp) {
            return (JsonObject) JsonParser.parse(serialize(document, timestamp));
        }

        private JsonObject document(Document document, LocalDateTime timestamp) {
            return root(document, timestamp).getObject("document");
        }

        private JsonObject firstItem(Document document) {
            return (JsonObject) document(document, UjpExamples.UNPRINTED_TIMESTAMP).getArray("docItems").items()
                    .getFirst();
        }

        @Test
        void nothingUndocumentedIsWritten() {
            // Decision 6: source, a trailing requestTimestamp, the *EinvoiceUser pair, the *EntityType pair (new
            // in the 21.10 correction), docGlobalDiscountAmount and docItemUnitItemDiscountAmount appear in some
            // examples and in no table; docItemHs appears in most examples and in no table.
            for (Document example : everyExample) {
                String json = serialize(example, UjpExamples.UNPRINTED_TIMESTAMP);
                assertThat(json).doesNotContain("\"source\"", "EinvoiceUser", "EntityType",
                        "docGlobalDiscountAmount", "docItemUnitItemDiscountAmount", "docItemHs");
                assertThat(json.indexOf("requestTimestamp")).isEqualTo(json.lastIndexOf("requestTimestamp"));
            }
        }

        @Test
        void theEnvelopeIsWrittenForEveryKindWhateverTheExampleOmits() {
            // The 21.10 examples print the bare body on 110, 120, 130 and the ordinary 100s, a document with its
            // reason and no timestamp on the storno and the correction, and the full envelope on 310 alone. The
            // signed payload nests under document and carries the timestamp (D1), so every kind is enveloped.
            for (Document example : everyExample) {
                LocalDateTime at = UjpExamples.UNPRINTED_TIMESTAMP;
                assertThat(root(example, at).members().keySet()).startsWith("requestTimestamp", "document");
                assertThat(root(example, at).getString("requestTimestamp")).isEqualTo("2026-10-21T10:00:00");
            }
        }

        @Test
        void theReasonObjectIsABrotherOfDocumentAndOnlyTheOneThatApplies() {
            // PDF pp. 29-32 and 34-37: voidReason on a storno, correctionReason on a correction. The 310
            // example writes both as null on an ordinary document; the 100 examples write neither.
            assertThat(root(UjpExamples.invoiceStorno(), UjpExamples.UNPRINTED_TIMESTAMP).members().keySet())
                    .containsExactly("requestTimestamp", "document", "voidReason");
            assertThat(root(UjpExamples.invoiceCorrection(), UjpExamples.UNPRINTED_TIMESTAMP).members().keySet())
                    .containsExactly("requestTimestamp", "document", "correctionReason");
            assertThat(root(UjpExamples.cashTurnover(), UjpExamples.CASH_TIMESTAMP).members().keySet())
                    .containsExactly("requestTimestamp", "document");
        }

        @Test
        void aStornoReferencesItsOriginalWithSTAndACorrectionWithKRAsThe21_10ExamplesPrintThem() {
            // PDF pp. 29-32 and 34-37 print docTypeRef ST / docNameRef "Сторно" and KR / "Корекција", each with
            // a UUID-shaped EUID and the date of the document.
            JsonObject storno = (JsonObject) document(UjpExamples.invoiceStorno(), UjpExamples.UNPRINTED_TIMESTAMP)
                    .getArray("docReferences").items().getFirst();
            JsonObject correction = (JsonObject) document(UjpExamples.invoiceCorrection(),
                    UjpExamples.UNPRINTED_TIMESTAMP).getArray("docReferences").items().getFirst();

            assertThat(storno.getString("docTypeRef")).isEqualTo("ST");
            assertThat(storno.getString("docNameRef")).isEqualTo("Сторно");
            assertThat(storno.getString("docNumberRef")).isEqualTo("01a15e7f-5e4e-7dcf-849e-ccbf9f65386d");
            assertThat(storno.getString("docDateRef")).isEqualTo("2026-10-21");
            assertThat(correction.getString("docTypeRef")).isEqualTo("KR");
            assertThat(correction.getString("docNameRef")).isEqualTo("Корекција");
            assertThat(correction.getString("docNumberRef")).isEqualTo("01a15e8a-8136-7d08-9703-e85ce5a206d5");
        }

        @Test
        void theReferenceNameIsTheCodeListsNotTheExamplesFreeWording() {
            // PDF pp. 7-9 print docNameRef "Фактура" beside docTypeRef SYS; the code list names SYS "Од систем"
            // (D1, /reference-types). The table says the name is the type's according to the list, so the
            // library writes the list's, and the conflict stays open in specification-status.
            JsonObject reference = (JsonObject) document(UjpExamples.invoiceWithReference(),
                    UjpExamples.UNPRINTED_TIMESTAMP).getArray("docReferences").items().getFirst();

            assertThat(reference.getString("docTypeRef")).isEqualTo("SYS");
            assertThat(reference.getString("docNameRef")).isEqualTo("Од систем").isNotEqualTo("Фактура");
            assertThat(reference.getString("docNumberRef")).isEqualTo("01a15da2-21b5-70ae-8f26-a85dd44fa74f");
        }

        @Test
        void aDiscountIsWrittenPerLineAndInTheDocumentDiscountNotAsAGlobalDiscount() {
            // PDF pp. 10-12 also print docGlobalDiscountAmount 10 and docItemUnitItemDiscountAmount 0, which
            // no table documents. The 15.10 changelog spreads an invoice-level discount over the lines and
            // sends it per line, so the 10 is in docItemUnitDiscountAmount and docDiscountAmount.
            Document discount = UjpExamples.invoiceWithDiscount();

            assertThat(firstItem(discount).getNumber("docItemUnitDiscountAmount")).isEqualByComparingTo("10");
            assertThat(document(discount, UjpExamples.UNPRINTED_TIMESTAMP).getObject("docTotals")
                    .getNumber("docDiscountAmount")).isEqualByComparingTo("10");
            assertThat(serialize(discount, UjpExamples.UNPRINTED_TIMESTAMP)).doesNotContain("docGlobalDiscountAmount");
        }

        @Test
        void lineAttributesAreWrittenOnlyOnTheLineThatHasThemAsThe21_10ExamplesDo() {
            // PDF pp. 2-22: the attributes member is on the one line of the line-attributes example and on no
            // other line, while docAttributes is [] on every 100.
            assertThat(firstItem(UjpExamples.invoice()).members()).doesNotContainKey("attributes");
            assertThat(firstItem(UjpExamples.invoiceWithDocumentAttribute()).members()).doesNotContainKey("attributes");
            JsonObject attribute = (JsonObject) firstItem(UjpExamples.invoiceWithLineAttribute())
                    .getArray("attributes").items().getFirst();
            assertThat(attribute.getString("name")).isEqualTo("Test Atribut");
            assertThat(attribute.getString("value")).isEqualTo("10");
            assertThat(attribute.getString("type")).isEqualTo("STR");
            assertThat(document(UjpExamples.invoice(), UjpExamples.UNPRINTED_TIMESTAMP).getArray("docAttributes").items())
                    .isEmpty();
            JsonObject docAttribute = (JsonObject) document(UjpExamples.invoiceWithDocumentAttribute(),
                    UjpExamples.UNPRINTED_TIMESTAMP).getArray("docAttributes").items().getFirst();
            assertThat(docAttribute.getString("value")).isEqualTo("123");
        }

        @Test
        void kind100WritesNoCurrencyDateOrRateAndBothCurrencyMembers() {
            // The 08.10 examples carried the date and the rate, the 15.10 changelog removed them from 100 and the
            // 21.10 examples no longer print them. They also print docCurrency alone, where the table lists
            // docCurrencyCode (UJP10-09) too, which is written.
            JsonObject payment = document(UjpExamples.invoice(), UjpExamples.UNPRINTED_TIMESTAMP).getObject("docPayment");
            assertThat(payment.members()).doesNotContainKeys("docCurrencyDate", "docCurrencyExchRate");
            assertThat(payment.getString("docCurrency")).isEqualTo("MKD");
            assertThat(payment.getString("docCurrencyCode")).isEqualTo("MKD");
        }

        @Test
        void aPaymentWithoutATypeIsWrittenAsNulls() {
            // PDF pp. 27-32: the storno and its original print docPaymentTypeCode and docPaymentTypeDesc as null.
            JsonObject payment = document(UjpExamples.invoiceStorno(), UjpExamples.UNPRINTED_TIMESTAMP)
                    .getObject("docPayment");
            assertThat(payment.get("docPaymentTypeCode")).contains(JsonNull.INSTANCE);
            assertThat(payment.get("docPaymentTypeDesc")).contains(JsonNull.INSTANCE);
            assertThat(payment.getString("docCurrency")).isEqualTo("MKD");
        }

        @Test
        void kinds130And310WriteTheDocumentDateAndARateOfOne() {
            // PDF pp. 41-43 and 55-57 print both, and the tables of 130 and 310 list them.
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
            // The tables mark it mandatory on all five kinds; the 21.10 examples of 100 all carry it, as false,
            // and the 110, 120, 130 and 310 examples omit it.
            for (Document example : List.of(UjpExamples.invoice(), UjpExamples.creditNote(),
                    UjpExamples.cashTurnover())) {
                JsonObject header = document(example, UjpExamples.UNPRINTED_TIMESTAMP).getObject("header");
                assertThat(header.get("docAlt")).contains(JsonBoolean.of(false));
            }
        }

        @Test
        void theNoteOfAStandardIndicatorIsNull() {
            // The 21.10 examples print null on every kind; the 08.10 examples of 100 printed "".
            JsonObject vat = (JsonObject) document(UjpExamples.invoice(), UjpExamples.UNPRINTED_TIMESTAMP)
                    .getArray("vatTotals").items().getFirst();
            assertThat(vat.get("vatTaxIndicatorNote")).contains(JsonNull.INSTANCE);
        }

        @Test
        void aNonStandardIndicatorsNoteComesFromTheCatalogueNotFromFreeText() {
            // The 08.10 multi-item example (pp. 4-8, superseded: the 21.10 one has no DDV-G line) printed "не е
            // ДДВ обврзник" for its DDV-G line. Decision 4: the note is the snapshot's, and a caller who wants
            // other words constructs the TaxIndicator.
            Document zeroRated = Document.builder(DocumentKind.INVOICE)
                    .header(Header.of("100-1/2026", "1", LocalDate.parse("2026-10-21"))
                            .withTurnoverDate(LocalDate.parse("2026-10-21")))
                    .seller(UjpExamples.invoice().seller())
                    .buyer(UjpExamples.invoice().buyer())
                    .line(Line.builder().description("x").quantity("1").unitPrice("90")
                            .tax(VatGroup.DDV_G, TaxIndicators.SNAPSHOT_2026_08_27.get("DDV-G")).build())
                    .build();
            JsonObject exempt = (JsonObject) document(zeroRated, UjpExamples.UNPRINTED_TIMESTAMP)
                    .getArray("vatTotals").items().getFirst();

            assertThat(exempt.getString("vatTaxIndicator")).isEqualTo("DDV-G");
            assertThat(exempt.getString("vatTaxIndicatorNote"))
                    .isEqualTo("ДДВ не е пресметан согласно член 51 став 3 од ЗДДВ");
        }

        @Test
        void numbersCarryNoTrailingZeros() {
            // The examples print 668, 667.5 and 288.2, never 668.00 (changelog 19.10.2026 says ",00" for an MKD
            // sum, which is how it is shown, not how the JSON prints it); the model's scale-4 values would print
            // 100.0000.
            JsonObject totals = document(UjpExamples.multiItemInvoice(), UjpExamples.UNPRINTED_TIMESTAMP)
                    .getObject("docTotals");
            assertThat(serialize(UjpExamples.multiItemInvoice(), UjpExamples.UNPRINTED_TIMESTAMP))
                    .contains("\"docGrossAmount\":667.5,", "\"docGrossAmountR\":668,", "\"docFinalAmount\":668}",
                            "\"docItemQty\":1,")
                    .doesNotContain("668.0", "667.50", "350.0000");
            assertThat(serialize(UjpExamples.invoiceWithDiscount(), UjpExamples.UNPRINTED_TIMESTAMP))
                    .contains("\"docGrossAmount\":288.2,", "\"docGrossAmountR\":288,");
            assertThat(totals.getNumber("docNetAmount")).isEqualByComparingTo(new BigDecimal("604.24"));
        }
    }
}
