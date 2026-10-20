package net.aetherealtech.ujpeinvoice.model;

/**
 * A complete address (UJP03-07, UJP05-07): street, number, postal code and city. All four parts are
 * mandatory on every scoped kind, so a missing part is refused here by name. The lengths differ by
 * kind (310 is shorter) and are checked when the address joins a document.
 */
public record Address(String street, String number, String postalCode, String city) {

    public Address {
        street = Check.required(street, "UJP03-07-01", "address.street", 80);
        number = Check.required(number, "UJP03-07-02", "address.number", 20);
        postalCode = Check.required(postalCode, "UJP03-07-03", "address.postalCode", 10);
        city = Check.required(city, "UJP03-07-04", "address.city", 60);
    }
}
