package net.aetherealtech.ujpeinvoice.serialization;

import net.aetherealtech.ujpeinvoice.model.Address;
import net.aetherealtech.ujpeinvoice.model.DocumentReference;
import net.aetherealtech.ujpeinvoice.model.DocumentType;
import net.aetherealtech.ujpeinvoice.model.Invoice;
import net.aetherealtech.ujpeinvoice.model.Party;
import net.aetherealtech.ujpeinvoice.model.VatCategory;
import net.aetherealtech.ujpeinvoice.testsupport.InvoiceFixtures;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Golden-file tests: the serializer's output for a fixed input must match byte-for-byte, so any
 * accidental change to field names, ordering, or number formatting is caught here rather than
 * discovered against a live sandbox. See src/test/resources/golden — regenerate deliberately (not
 * by hand-editing) if a field genuinely needs to change, and update
 * {@code openspec/specs/specification-status/spec.md} in the same change.
 */
class UjpJsonSerializerTest {

    private final UjpJsonSerializer serializer = new UjpJsonSerializer();

    @Test
    void matchesGoldenFileForASingleCategoryInvoice() {
        assertThat(serialize(InvoiceFixtures.simpleInvoice())).isEqualTo(golden("simple-invoice.json"));
    }

    @Test
    void matchesGoldenFileForAMultiCategoryInvoice() {
        assertThat(serialize(InvoiceFixtures.multiCategoryInvoice())).isEqualTo(golden("multi-category-invoice.json"));
    }

    @Test
    void matchesGoldenFileForACreditNote() {
        assertThat(serialize(InvoiceFixtures.creditNote())).isEqualTo(golden("credit-note.json"));
    }

    @Test
    void anInvoiceWritesNoDocumentTypeAtAll() {
        // The pin behind "a 0.1.0 invoice serializes byte for byte as it did": an invoice's type is
        // expressed by absence, so introducing notes could not disturb the shape already in the field.
        String json = serialize(InvoiceFixtures.simpleInvoice());

        assertThat(json).doesNotContain("documentType");
        assertThat(json).doesNotContain("correctedInvoice");
    }

    @Test
    void aDebitNoteWritesItsOwnTypeAndReference() {
        Invoice note = Invoice.builder()
                .invoiceNumber("DN-2026-0003")
                .issueDate(LocalDate.of(2026, 9, 4))
                .currency("MKD")
                .seller(InvoiceFixtures.seller())
                .buyer(InvoiceFixtures.buyer())
                .documentType(DocumentType.DEBIT_NOTE)
                .correctedInvoice(new DocumentReference("INV-2026-0002", LocalDate.of(2026, 8, 28)))
                .addLineItem("Understated delivery charge", new BigDecimal("1"), new BigDecimal("250.00"),
                        VatCategory.STANDARD_18)
                .build();

        assertThat(serialize(note)).startsWith("{\"documentType\":\"DEBIT_NOTE\","
                + "\"invoiceNumber\":\"DN-2026-0003\",\"issueDate\":\"2026-09-04\","
                + "\"correctedInvoice\":{\"number\":\"INV-2026-0002\",\"issueDate\":\"2026-08-28\"},"
                + "\"currency\":\"MKD\",");
    }

    @Test
    void matchesGoldenFileForAConsumerInvoice() {
        assertThat(serialize(InvoiceFixtures.consumerInvoice())).isEqualTo(golden("consumer-invoice.json"));
    }

    @Test
    void aNaturalPersonBuyerWritesOnlyWhatIsKnownAboutThem() {
        String json = serialize(InvoiceFixtures.consumerInvoice());

        assertThat(json).contains("\"buyer\":{\"name\":\"Ана Ангеловска\"},");
        assertThat(json).doesNotContain("\"taxId\":\"\"");
        assertThat(json).doesNotContain("\"address\":{}");
    }

    @Test
    void aPartialAddressWritesOnlyThePartsItHas() {
        Invoice invoice = Invoice.builder()
                .invoiceNumber("INV-2026-0004")
                .issueDate(LocalDate.of(2026, 9, 4))
                .currency("MKD")
                .seller(InvoiceFixtures.seller())
                .buyer(Party.naturalPerson("Ана Ангеловска", new Address(null, "Битола", null, null)))
                .addLineItem("Услуга", BigDecimal.ONE, new BigDecimal("500.00"), VatCategory.STANDARD_18)
                .build();

        assertThat(serialize(invoice))
                .contains("\"buyer\":{\"name\":\"Ана Ангеловска\",\"address\":{\"city\":\"Битола\",\"country\":\"MK\"}},");
    }

    @Test
    void aLineWithoutAUnitWritesNoUnitField() {
        assertThat(serialize(InvoiceFixtures.simpleInvoice())).doesNotContain("\"unit\":");
    }

    @Test
    void contentTypeIsApplicationJson() {
        assertThat(serializer.contentType()).isEqualTo("application/json");
    }

    @Test
    void outputIsValidUtf8Json() {
        byte[] bytes = serializer.serialize(InvoiceFixtures.simpleInvoice());
        Object parsed = net.aetherealtech.ujpeinvoice.internal.json.JsonParser.parse(
                new String(bytes, StandardCharsets.UTF_8));
        assertThat(parsed).isInstanceOf(net.aetherealtech.ujpeinvoice.internal.json.JsonObject.class);
    }

    @Test
    void omitsDueDateFieldWhenAbsent() {
        String json = serialize(InvoiceFixtures.multiCategoryInvoice());
        assertThat(json).doesNotContain("dueDate");
    }

    private String serialize(Invoice invoice) {
        return new String(serializer.serialize(invoice), StandardCharsets.UTF_8);
    }

    private String golden(String resourceName) {
        try (InputStream in = getClass().getResourceAsStream("/golden/" + resourceName)) {
            if (in == null) {
                throw new IllegalStateException("Missing golden file: " + resourceName);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
