package net.aetherealtech.ujpeinvoice.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvoiceTest {

    private static final Address ADDRESS = new Address("Main St 1", "Skopje", "1000", "MK");
    private static final Party SELLER = new Party("Seller DOOEL", "4030012345678", ADDRESS);
    private static final Party BUYER = new Party("Buyer DOO", "4057098765432", ADDRESS);

    @Test
    void builderComputesReconcilingTotals() {
        Invoice invoice = Invoice.builder()
                .invoiceNumber("INV-1")
                .issueDate(LocalDate.of(2026, 1, 1))
                .currency("MKD")
                .seller(SELLER)
                .buyer(BUYER)
                .addLineItem("Item", new BigDecimal("2"), new BigDecimal("100.00"), VatCategory.STANDARD_18)
                .build();

        assertThat(invoice.totals().netTotal()).isEqualByComparingTo("200.00");
        assertThat(invoice.totals().reconciles(invoice.lineItems())).isTrue();
        assertThat(invoice.currency()).isEqualTo(Currency.getInstance("MKD"));
    }

    @Test
    void builderAcceptsMultipleLineItems() {
        Invoice invoice = Invoice.builder()
                .invoiceNumber("INV-2")
                .issueDate(LocalDate.of(2026, 1, 1))
                .currency("MKD")
                .seller(SELLER)
                .buyer(BUYER)
                .addLineItem("A", BigDecimal.ONE, new BigDecimal("10.00"), VatCategory.STANDARD_18)
                .addLineItem("B", BigDecimal.ONE, new BigDecimal("20.00"), VatCategory.EXEMPT)
                .build();

        assertThat(invoice.lineItems()).hasSize(2);
    }

    @Test
    void canonicalConstructorRejectsTotalsThatDoNotReconcile() {
        List<LineItem> items = List.of(
                new LineItem("A", new BigDecimal("2"), new BigDecimal("100.00"), VatCategory.STANDARD_18));
        Totals wrongTotals = Totals.compute(List.of(
                new LineItem("A", new BigDecimal("3"), new BigDecimal("100.00"), VatCategory.STANDARD_18)));

        assertThatThrownBy(() -> new Invoice("INV-3", LocalDate.of(2026, 1, 1), null,
                Currency.getInstance("MKD"), SELLER, BUYER, items, wrongTotals))
                .isInstanceOf(InvoiceValidationException.class)
                .hasMessageContaining("does not reconcile");
    }

    @Test
    void canonicalConstructorRejectsNullTotals() {
        List<LineItem> items = List.of(
                new LineItem("A", BigDecimal.ONE, BigDecimal.TEN, VatCategory.STANDARD_18));

        assertThatThrownBy(() -> new Invoice("INV-N", LocalDate.of(2026, 1, 1), null,
                Currency.getInstance("MKD"), SELLER, BUYER, items, null))
                .isInstanceOf(InvoiceValidationException.class);
    }

    @Test
    void rejectsBlankInvoiceNumber() {
        assertThatThrownBy(() -> Invoice.builder()
                .invoiceNumber("  ")
                .issueDate(LocalDate.of(2026, 1, 1))
                .currency("MKD")
                .seller(SELLER)
                .buyer(BUYER)
                .addLineItem("A", BigDecimal.ONE, BigDecimal.TEN, VatCategory.STANDARD_18)
                .build())
                .isInstanceOf(InvoiceValidationException.class);
    }

    @Test
    void rejectsMissingIssueDate() {
        assertThatThrownBy(() -> Invoice.builder()
                .invoiceNumber("INV-4")
                .currency("MKD")
                .seller(SELLER)
                .buyer(BUYER)
                .addLineItem("A", BigDecimal.ONE, BigDecimal.TEN, VatCategory.STANDARD_18)
                .build())
                .isInstanceOf(InvoiceValidationException.class);
    }

    @Test
    void rejectsDueDateBeforeIssueDate() {
        assertThatThrownBy(() -> Invoice.builder()
                .invoiceNumber("INV-5")
                .issueDate(LocalDate.of(2026, 1, 10))
                .dueDate(LocalDate.of(2026, 1, 1))
                .currency("MKD")
                .seller(SELLER)
                .buyer(BUYER)
                .addLineItem("A", BigDecimal.ONE, BigDecimal.TEN, VatCategory.STANDARD_18)
                .build())
                .isInstanceOf(InvoiceValidationException.class);
    }

    @Test
    void allowsDueDateEqualToIssueDate() {
        Invoice invoice = Invoice.builder()
                .invoiceNumber("INV-6")
                .issueDate(LocalDate.of(2026, 1, 10))
                .dueDate(LocalDate.of(2026, 1, 10))
                .currency("MKD")
                .seller(SELLER)
                .buyer(BUYER)
                .addLineItem("A", BigDecimal.ONE, BigDecimal.TEN, VatCategory.STANDARD_18)
                .build();

        assertThat(invoice.dueDate()).isEqualTo(invoice.issueDate());
    }

    @Test
    void rejectsMissingCurrency() {
        assertThatThrownBy(() -> Invoice.builder()
                .invoiceNumber("INV-7")
                .issueDate(LocalDate.of(2026, 1, 1))
                .seller(SELLER)
                .buyer(BUYER)
                .addLineItem("A", BigDecimal.ONE, BigDecimal.TEN, VatCategory.STANDARD_18)
                .build())
                .isInstanceOf(InvoiceValidationException.class);
    }

    @Test
    void rejectsMissingSeller() {
        assertThatThrownBy(() -> Invoice.builder()
                .invoiceNumber("INV-8")
                .issueDate(LocalDate.of(2026, 1, 1))
                .currency("MKD")
                .buyer(BUYER)
                .addLineItem("A", BigDecimal.ONE, BigDecimal.TEN, VatCategory.STANDARD_18)
                .build())
                .isInstanceOf(InvoiceValidationException.class);
    }

    @Test
    void rejectsMissingBuyer() {
        assertThatThrownBy(() -> Invoice.builder()
                .invoiceNumber("INV-9")
                .issueDate(LocalDate.of(2026, 1, 1))
                .currency("MKD")
                .seller(SELLER)
                .addLineItem("A", BigDecimal.ONE, BigDecimal.TEN, VatCategory.STANDARD_18)
                .build())
                .isInstanceOf(InvoiceValidationException.class);
    }

    @Test
    void rejectsNoLineItems() {
        assertThatThrownBy(() -> Invoice.builder()
                .invoiceNumber("INV-10")
                .issueDate(LocalDate.of(2026, 1, 1))
                .currency("MKD")
                .seller(SELLER)
                .buyer(BUYER)
                .build())
                .isInstanceOf(InvoiceValidationException.class);
    }

    @Test
    void rejectsNullLineItem() {
        assertThatThrownBy(() -> Invoice.builder().addLineItem((LineItem) null))
                .isInstanceOf(InvoiceValidationException.class);
    }

    @Test
    void defaultsToAPlainInvoiceCorrectingNothing() {
        Invoice invoice = invoiceBuilder("INV-12").build();

        assertThat(invoice.documentType()).isEqualTo(DocumentType.INVOICE);
        assertThat(invoice.correctedInvoice()).isNull();
    }

    @Test
    void theEightArgumentConstructorStillBuildsAPlainInvoice() {
        List<LineItem> items = List.of(
                new LineItem("A", BigDecimal.ONE, BigDecimal.TEN, VatCategory.STANDARD_18));

        Invoice invoice = new Invoice("INV-13", LocalDate.of(2026, 1, 1), null, Currency.getInstance("MKD"),
                SELLER, BUYER, items, Totals.compute(items));

        assertThat(invoice.documentType()).isEqualTo(DocumentType.INVOICE);
        assertThat(invoice.correctedInvoice()).isNull();
    }

    @Test
    void aCreditNoteCarriesTheInvoiceItCorrects() {
        DocumentReference corrected = new DocumentReference("INV-1", LocalDate.of(2026, 1, 1));

        Invoice note = invoiceBuilder("CN-1")
                .documentType(DocumentType.CREDIT_NOTE)
                .correctedInvoice(corrected)
                .build();

        assertThat(note.documentType()).isEqualTo(DocumentType.CREDIT_NOTE);
        assertThat(note.correctedInvoice()).isEqualTo(corrected);
    }

    @Test
    void aNotesAmountsStayPositive() {
        Invoice note = invoiceBuilder("DN-1")
                .documentType(DocumentType.DEBIT_NOTE)
                .correctedInvoice(new DocumentReference("INV-1", LocalDate.of(2026, 1, 1)))
                .build();

        // The type carries the direction; the totals rules are the same ones an invoice uses.
        assertThat(note.totals().grossTotal()).isPositive();
        assertThat(note.totals().reconciles(note.lineItems())).isTrue();
    }

    @Test
    void rejectsANoteWithNoCorrectedInvoice() {
        assertThatThrownBy(() -> invoiceBuilder("CN-2").documentType(DocumentType.CREDIT_NOTE).build())
                .isInstanceOf(InvoiceValidationException.class)
                .hasMessageContaining("Invoice.correctedInvoice")
                .hasMessageContaining("CREDIT_NOTE");
    }

    @Test
    void rejectsAPlainInvoiceThatCorrectsSomething() {
        assertThatThrownBy(() -> invoiceBuilder("INV-14")
                .correctedInvoice(new DocumentReference("INV-1", LocalDate.of(2026, 1, 1)))
                .build())
                .isInstanceOf(InvoiceValidationException.class)
                .hasMessageContaining("Invoice.correctedInvoice");
    }

    @Test
    void rejectsAMissingDocumentType() {
        assertThatThrownBy(() -> invoiceBuilder("INV-15").documentType(null).build())
                .isInstanceOf(InvoiceValidationException.class)
                .hasMessageContaining("Invoice.documentType");
    }

    @Test
    void lineItemsAreDefensivelyCopied() {
        Invoice invoice = Invoice.builder()
                .invoiceNumber("INV-11")
                .issueDate(LocalDate.of(2026, 1, 1))
                .currency("MKD")
                .seller(SELLER)
                .buyer(BUYER)
                .addLineItem("A", BigDecimal.ONE, BigDecimal.TEN, VatCategory.STANDARD_18)
                .build();

        assertThatThrownBy(() -> invoice.lineItems().add(
                new LineItem("B", BigDecimal.ONE, BigDecimal.TEN, VatCategory.STANDARD_18)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static Invoice.Builder invoiceBuilder(String number) {
        return Invoice.builder()
                .invoiceNumber(number)
                .issueDate(LocalDate.of(2026, 1, 1))
                .currency("MKD")
                .seller(SELLER)
                .buyer(BUYER)
                .addLineItem("A", BigDecimal.ONE, BigDecimal.TEN, VatCategory.STANDARD_18);
    }
}
