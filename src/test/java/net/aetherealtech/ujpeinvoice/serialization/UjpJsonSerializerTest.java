package net.aetherealtech.ujpeinvoice.serialization;

import net.aetherealtech.ujpeinvoice.model.Invoice;
import net.aetherealtech.ujpeinvoice.testsupport.InvoiceFixtures;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Golden-file tests: the serializer's output for a fixed input must match byte-for-byte, so any
 * accidental change to field names, ordering, or number formatting is caught here rather than
 * discovered against a live sandbox. See src/test/resources/golden — regenerate deliberately (not
 * by hand-editing) if a field genuinely needs to change, and update the README's specification
 * inventory in the same change.
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
