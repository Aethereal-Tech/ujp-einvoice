package net.aetherealtech.ujpeinvoice.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AddressTest {

    @Test
    void acceptsAllFieldsPresent() {
        Address address = new Address("Main St 1", "Skopje", "1000", "MK");
        assertThat(address.street()).isEqualTo("Main St 1");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void rejectsBlankStreet(String street) {
        assertThatThrownBy(() -> new Address(street, "Skopje", "1000", "MK"))
                .isInstanceOf(InvoiceValidationException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void rejectsBlankCity(String city) {
        assertThatThrownBy(() -> new Address("Main St 1", city, "1000", "MK"))
                .isInstanceOf(InvoiceValidationException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void rejectsBlankPostalCode(String postalCode) {
        assertThatThrownBy(() -> new Address("Main St 1", "Skopje", postalCode, "MK"))
                .isInstanceOf(InvoiceValidationException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void rejectsBlankCountry(String country) {
        assertThatThrownBy(() -> new Address("Main St 1", "Skopje", "1000", country))
                .isInstanceOf(InvoiceValidationException.class);
    }
}
