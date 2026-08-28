package net.aetherealtech.ujpeinvoice.model;

/**
 * A postal address for a {@link Party}. {@code country} is an ISO 3166-1 alpha-2 code
 * (e.g. {@code "MK"}); this library does not validate it against the ISO list.
 */
public record Address(String street, String city, String postalCode, String country) {

    public Address {
        if (street == null || street.isBlank()) {
            throw new InvoiceValidationException("Address.street must not be blank");
        }
        if (city == null || city.isBlank()) {
            throw new InvoiceValidationException("Address.city must not be blank");
        }
        if (postalCode == null || postalCode.isBlank()) {
            throw new InvoiceValidationException("Address.postalCode must not be blank");
        }
        if (country == null || country.isBlank()) {
            throw new InvoiceValidationException("Address.country must not be blank");
        }
    }
}
