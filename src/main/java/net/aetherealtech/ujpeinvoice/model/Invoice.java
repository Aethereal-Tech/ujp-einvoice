package net.aetherealtech.ujpeinvoice.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

/**
 * A complete sales document: header fields, the two parties, its line items, and totals that are
 * guaranteed to reconcile with those line items. An invoice by default, or — with a
 * {@link DocumentType} that {@linkplain DocumentType#corrects corrects} — a credit or debit note
 * naming the invoice it corrects.
 *
 * <p>This type is serialization-agnostic — it knows nothing about JSON, JWS, or the UJP gateway.
 * That separation is deliberate: see {@code net.aetherealtech.ujpeinvoice.serialization.Serializer}
 * for turning an {@code Invoice} into wire bytes, and
 * {@code openspec/specs/specification-status/spec.md} for what is and is not verified about that
 * wire shape.
 */
public record Invoice(String invoiceNumber, LocalDate issueDate, LocalDate dueDate, Currency currency,
                       Party seller, Party buyer, List<LineItem> lineItems, Totals totals,
                       DocumentType documentType, DocumentReference correctedInvoice) {

    public Invoice {
        if (invoiceNumber == null || invoiceNumber.isBlank()) {
            throw new InvoiceValidationException("Invoice.invoiceNumber must not be blank");
        }
        if (issueDate == null) {
            throw new InvoiceValidationException("Invoice.issueDate must not be null");
        }
        if (dueDate != null && dueDate.isBefore(issueDate)) {
            throw new InvoiceValidationException("Invoice.dueDate must not precede issueDate");
        }
        if (currency == null) {
            throw new InvoiceValidationException("Invoice.currency must not be null");
        }
        if (seller == null) {
            throw new InvoiceValidationException("Invoice.seller must not be null");
        }
        // The buyer may be a natural person carrying nothing but a name; the seller never can. It is
        // the taxpayer issuing the document, and a document issued by an unidentified one is not an
        // invoice. See Party.company.
        String missing = seller.missingCompanyField();
        if (missing != null) {
            throw new InvoiceValidationException("Invoice.seller." + missing
                    + " must be present: a seller is always an identified business");
        }
        if (buyer == null) {
            throw new InvoiceValidationException("Invoice.buyer must not be null");
        }
        if (lineItems == null || lineItems.isEmpty()) {
            throw new InvoiceValidationException("Invoice.lineItems must contain at least one item");
        }
        lineItems = List.copyOf(lineItems);
        if (totals == null) {
            throw new InvoiceValidationException("Invoice.totals must not be null");
        }
        // Re-derived and checked here, not trusted from the caller: this constructor is public, and a
        // Totals built by hand (or one carried in from a deserialized payload — see the serializer
        // package) has had no chance to go through Totals.compute yet. Builder.build() below always
        // passes a Totals that reconciles by construction, so this check costs it nothing extra.
        if (!totals.reconciles(lineItems)) {
            throw new InvoiceValidationException(
                    "Invoice.totals does not reconcile with lineItems: expected " + Totals.compute(lineItems)
                            + " but was " + totals);
        }
        if (documentType == null) {
            throw new InvoiceValidationException("Invoice.documentType must not be null");
        }
        if (documentType.corrects() && correctedInvoice == null) {
            throw new InvoiceValidationException("Invoice.correctedInvoice must be present on a "
                    + documentType + ": a note has to name the invoice it corrects");
        }
        if (!documentType.corrects() && correctedInvoice != null) {
            throw new InvoiceValidationException("Invoice.correctedInvoice belongs to a credit or debit note, "
                    + "but Invoice.documentType is " + documentType);
        }
    }

    /**
     * A plain {@link DocumentType#INVOICE} correcting nothing — the shape this record had before
     * notes existed, kept so a call site written against it still compiles and still means exactly
     * what it did.
     */
    public Invoice(String invoiceNumber, LocalDate issueDate, LocalDate dueDate, Currency currency,
                   Party seller, Party buyer, List<LineItem> lineItems, Totals totals) {
        this(invoiceNumber, issueDate, dueDate, currency, seller, buyer, lineItems, totals,
                DocumentType.INVOICE, null);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Accumulates header fields and line items, then computes {@link Totals} on {@link #build()}. */
    public static final class Builder {
        private String invoiceNumber;
        private LocalDate issueDate;
        private LocalDate dueDate;
        private Currency currency;
        private Party seller;
        private Party buyer;
        private final List<LineItem> lineItems = new ArrayList<>();
        private DocumentType documentType = DocumentType.INVOICE;
        private DocumentReference correctedInvoice;

        private Builder() {
        }

        public Builder invoiceNumber(String invoiceNumber) {
            this.invoiceNumber = invoiceNumber;
            return this;
        }

        public Builder issueDate(LocalDate issueDate) {
            this.issueDate = issueDate;
            return this;
        }

        public Builder dueDate(LocalDate dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public Builder currency(Currency currency) {
            this.currency = currency;
            return this;
        }

        public Builder currency(String isoCurrencyCode) {
            return currency(Currency.getInstance(isoCurrencyCode));
        }

        public Builder seller(Party seller) {
            this.seller = seller;
            return this;
        }

        public Builder buyer(Party buyer) {
            this.buyer = buyer;
            return this;
        }

        public Builder addLineItem(LineItem lineItem) {
            if (lineItem == null) {
                throw new InvoiceValidationException("lineItem must not be null");
            }
            this.lineItems.add(lineItem);
            return this;
        }

        public Builder addLineItem(String description, BigDecimal quantity, BigDecimal unitPrice,
                                    VatCategory vatCategory) {
            return addLineItem(new LineItem(description, quantity, unitPrice, vatCategory));
        }

        /** As above, stating what the quantity is counted in — see {@link LineItem#unit()}. */
        public Builder addLineItem(String description, BigDecimal quantity, BigDecimal unitPrice,
                                    VatCategory vatCategory, String unit) {
            return addLineItem(new LineItem(description, quantity, unitPrice, vatCategory, unit));
        }

        /** Defaults to {@link DocumentType#INVOICE}. */
        public Builder documentType(DocumentType documentType) {
            this.documentType = documentType;
            return this;
        }

        /** The invoice this credit or debit note corrects. Required on a note, refused on an invoice. */
        public Builder correctedInvoice(DocumentReference correctedInvoice) {
            this.correctedInvoice = correctedInvoice;
            return this;
        }

        /** Builds the document, computing {@link Totals} from the accumulated line items. */
        public Invoice build() {
            Totals totals = Totals.compute(lineItems);
            return new Invoice(invoiceNumber, issueDate, dueDate, currency, seller, buyer, lineItems, totals,
                    documentType, correctedInvoice);
        }
    }
}
