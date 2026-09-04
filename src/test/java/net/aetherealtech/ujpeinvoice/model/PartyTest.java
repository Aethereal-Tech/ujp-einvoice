package net.aetherealtech.ujpeinvoice.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartyTest {

    private static final Address ADDRESS = new Address("Main St 1", "Skopje", "1000", "MK");

    @Test
    void acceptsValidParty() {
        Party party = new Party("Acme DOOEL", "4030012345678", ADDRESS);

        assertThat(party.name()).isEqualTo("Acme DOOEL");
        assertThat(party.taxId()).isEqualTo("4030012345678");
        assertThat(party.isNaturalPerson()).isFalse();
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> new Party("  ", "4030012345678", ADDRESS))
                .isInstanceOf(InvoiceValidationException.class)
                .hasMessageContaining("Party.name");
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = "   ")
    void rejectsBlankTaxId(String taxId) {
        // Absent is a natural person; blank is a business whose ЕДБ was left empty by mistake.
        assertThatThrownBy(() -> new Party("Acme DOOEL", taxId, ADDRESS))
                .isInstanceOf(InvoiceValidationException.class)
                .hasMessageContaining("Party.taxId");
    }

    @Test
    void aCompanyCarriesEverything() {
        Party company = Party.company("Acme DOOEL", "4030012345678", ADDRESS);

        assertThat(company.isNaturalPerson()).isFalse();
        assertThat(company.address()).isEqualTo(ADDRESS);
    }

    @Test
    void aCompanyWithoutATaxIdIsRefusedByName() {
        assertThatThrownBy(() -> Party.company("Acme DOOEL", null, ADDRESS))
                .isInstanceOf(InvoiceValidationException.class)
                .hasMessageContaining("taxId")
                .hasMessageContaining("naturalPerson");
    }

    @Test
    void aCompanyWithoutAnAddressIsRefusedByName() {
        assertThatThrownBy(() -> Party.company("Acme DOOEL", "4030012345678", null))
                .isInstanceOf(InvoiceValidationException.class)
                .hasMessageContaining("address");
    }

    @Test
    void aCompanyWithAPartialAddressIsRefusedByField() {
        assertThatThrownBy(() -> Party.company("Acme DOOEL", "4030012345678",
                new Address(null, "Skopje", "1000", "MK")))
                .isInstanceOf(InvoiceValidationException.class)
                .hasMessageContaining("address.street");
    }

    @Test
    void aNaturalPersonNeedsNothingButAName() {
        Party buyer = Party.naturalPerson("Ана Ангеловска");

        assertThat(buyer.name()).isEqualTo("Ана Ангеловска");
        assertThat(buyer.taxId()).isNull();
        assertThat(buyer.address()).isNull();
        assertThat(buyer.isNaturalPerson()).isTrue();
    }

    @Test
    void aNaturalPersonMayCarryWhateverAddressIsOnRecord() {
        Party buyer = Party.naturalPerson("Ана Ангеловска", new Address(null, "Битола", null, null));

        assertThat(buyer.address().city()).isEqualTo("Битола");
        assertThat(buyer.address().street()).isNull();
        assertThat(buyer.address().postalCode()).isNull();
        assertThat(buyer.address().country()).isEqualTo("MK");
    }

    @Test
    void aNaturalPersonStillNeedsAName() {
        assertThatThrownBy(() -> Party.naturalPerson("   "))
                .isInstanceOf(InvoiceValidationException.class)
                .hasMessageContaining("Party.name");
    }
}
