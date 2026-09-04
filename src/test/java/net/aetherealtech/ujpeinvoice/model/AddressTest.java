package net.aetherealtech.ujpeinvoice.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AddressTest {

    @Test
    void acceptsAllFieldsPresent() {
        Address address = new Address("Main St 1", "Skopje", "1000", "MK");

        assertThat(address.street()).isEqualTo("Main St 1");
        assertThat(address.city()).isEqualTo("Skopje");
        assertThat(address.postalCode()).isEqualTo("1000");
        assertThat(address.country()).isEqualTo("MK");
    }

    @Test
    void acceptsAnAddressWithNothingButACountry() {
        Address address = new Address(null, null, null, "MK");

        assertThat(address.street()).isNull();
        assertThat(address.city()).isNull();
        assertThat(address.postalCode()).isNull();
    }

    @Test
    void defaultsAnAbsentCountryToNorthMacedonia() {
        assertThat(new Address("Main St 1", "Skopje", "1000", null).country())
                .isEqualTo(Address.DEFAULT_COUNTRY);
        assertThat(new Address(null, null, null, null).country()).isEqualTo("MK");
    }

    @Test
    void keepsAForeignCountryAsGiven() {
        assertThat(new Address("Knez Mihailova 1", "Belgrade", "11000", "RS").country()).isEqualTo("RS");
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = "   ")
    void rejectsABlankStreet(String street) {
        assertThatThrownBy(() -> new Address(street, "Skopje", "1000", "MK"))
                .isInstanceOf(InvoiceValidationException.class)
                .hasMessageContaining("Address.street");
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = "   ")
    void rejectsABlankCity(String city) {
        assertThatThrownBy(() -> new Address("Main St 1", city, "1000", "MK"))
                .isInstanceOf(InvoiceValidationException.class)
                .hasMessageContaining("Address.city");
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = "   ")
    void rejectsABlankPostalCode(String postalCode) {
        assertThatThrownBy(() -> new Address("Main St 1", "Skopje", postalCode, "MK"))
                .isInstanceOf(InvoiceValidationException.class)
                .hasMessageContaining("Address.postalCode");
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = "   ")
    void rejectsABlankCountry(String country) {
        // Blank is not absent: absent means MK, blank means someone wrote nothing into a field they
        // meant to fill.
        assertThatThrownBy(() -> new Address("Main St 1", "Skopje", "1000", country))
                .isInstanceOf(InvoiceValidationException.class)
                .hasMessageContaining("Address.country");
    }

    @Test
    void namesTheFirstFieldABusinessAddressIsMissing() {
        assertThat(new Address(null, "Skopje", "1000", "MK").missingCompanyField()).isEqualTo("address.street");
        assertThat(new Address("Main St 1", "Skopje", null, "MK").missingCompanyField())
                .isEqualTo("address.postalCode");
        assertThat(new Address("Main St 1", null, "1000", "MK").missingCompanyField()).isEqualTo("address.city");
        assertThat(new Address("Main St 1", "Skopje", "1000", "MK").missingCompanyField()).isNull();
    }
}
