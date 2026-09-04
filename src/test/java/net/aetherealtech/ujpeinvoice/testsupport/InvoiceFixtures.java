package net.aetherealtech.ujpeinvoice.testsupport;

import net.aetherealtech.ujpeinvoice.model.Address;
import net.aetherealtech.ujpeinvoice.model.DocumentReference;
import net.aetherealtech.ujpeinvoice.model.DocumentType;
import net.aetherealtech.ujpeinvoice.model.Invoice;
import net.aetherealtech.ujpeinvoice.model.Party;
import net.aetherealtech.ujpeinvoice.model.VatCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Sample invoice data shared across serializer, client, and golden-file tests. */
public final class InvoiceFixtures {

    private InvoiceFixtures() {
    }

    public static Party seller() {
        return new Party("Aethereal Tech DOOEL", "4030012345678",
                new Address("Bul. Partizanski Odredi 1", "Skopje", "1000", "MK"));
    }

    public static Party buyer() {
        return new Party("Buyer Company DOO", "4057098765432",
                new Address("Ul. Makedonija 10", "Bitola", "7000", "MK"));
    }

    /** A single-category, single-line invoice: one line at the standard 18% rate. */
    public static Invoice simpleInvoice() {
        return Invoice.builder()
                .invoiceNumber("INV-2026-0001")
                .issueDate(LocalDate.of(2026, 8, 28))
                .dueDate(LocalDate.of(2026, 9, 12))
                .currency("MKD")
                .seller(seller())
                .buyer(buyer())
                .addLineItem("Consulting services", new BigDecimal("2"), new BigDecimal("100.00"),
                        VatCategory.STANDARD_18)
                .build();
    }

    /**
     * A credit note against {@link #simpleInvoice()}, crediting one of its two consulting hours.
     * Its amounts are positive: the document type carries the direction.
     */
    public static Invoice creditNote() {
        return Invoice.builder()
                .invoiceNumber("CN-2026-0007")
                .issueDate(LocalDate.of(2026, 9, 4))
                .currency("MKD")
                .seller(seller())
                .buyer(buyer())
                .documentType(DocumentType.CREDIT_NOTE)
                .correctedInvoice(new DocumentReference("INV-2026-0001", LocalDate.of(2026, 8, 28)))
                .addLineItem("Consulting services (partial credit)", new BigDecimal("1"),
                        new BigDecimal("100.00"), VatCategory.STANDARD_18)
                .build();
    }

    /** One line per VAT category, exercising every rate (18/10/5/0/exempt) in one invoice. */
    public static Invoice multiCategoryInvoice() {
        return Invoice.builder()
                .invoiceNumber("INV-2026-0002")
                .issueDate(LocalDate.of(2026, 8, 28))
                .currency("MKD")
                .seller(seller())
                .buyer(buyer())
                .addLineItem("Standard-rated widget", new BigDecimal("3"), new BigDecimal("50.00"),
                        VatCategory.STANDARD_18)
                .addLineItem("Reduced 10% item", new BigDecimal("1"), new BigDecimal("40.00"),
                        VatCategory.REDUCED_10)
                .addLineItem("Basic foodstuff", new BigDecimal("5"), new BigDecimal("20.00"),
                        VatCategory.REDUCED_5)
                .addLineItem("Exported goods", new BigDecimal("1"), new BigDecimal("500.00"),
                        VatCategory.ZERO)
                .addLineItem("Medical service", new BigDecimal("2"), new BigDecimal("75.00"),
                        VatCategory.EXEMPT)
                .build();
    }
}
