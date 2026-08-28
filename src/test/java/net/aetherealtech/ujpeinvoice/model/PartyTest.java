package net.aetherealtech.ujpeinvoice.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartyTest {

    private static final Address ADDRESS = new Address("Main St 1", "Skopje", "1000", "MK");

    @Test
    void acceptsValidParty() {
        Party party = new Party("Acme DOOEL", "4030012345678", ADDRESS);
        assertThat(party.name()).isEqualTo("Acme DOOEL");
        assertThat(party.taxId()).isEqualTo("4030012345678");
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> new Party("  ", "4030012345678", ADDRESS))
                .isInstanceOf(InvoiceValidationException.class);
    }

    @Test
    void rejectsBlankTaxId() {
        assertThatThrownBy(() -> new Party("Acme DOOEL", "", ADDRESS))
                .isInstanceOf(InvoiceValidationException.class);
    }

    @Test
    void rejectsNullAddress() {
        assertThatThrownBy(() -> new Party("Acme DOOEL", "4030012345678", null))
                .isInstanceOf(InvoiceValidationException.class);
    }
}
