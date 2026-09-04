package net.aetherealtech.ujpeinvoice.model;

/**
 * A postal address for a {@link Party}. {@code country} is an ISO 3166-1 alpha-2 code
 * (e.g. {@code "MK"}); this library does not validate it against the ISO list, and defaults it to
 * {@link #DEFAULT_COUNTRY} when absent, this being a North Macedonian e-invoicing library.
 *
 * <h2>Absent versus blank</h2>
 * <p>Street, city and postal code may be {@code null}, because a natural-person buyer often has
 * none of them on record and this library will not invent a requirement the spec has not been read
 * to confirm (see {@link Party#naturalPerson}). A <em>blank</em> string is still refused: it is a
 * missing value pretending to be present, and the serializer would have to choose between writing
 * an empty string to the gateway and silently dropping it. Absent fields are omitted from the wire.
 *
 * <p>A seller's address is a different matter — {@link Invoice} requires the seller to carry a
 * complete one, and names the field that is missing when it does not.
 */
public record Address(String street, String city, String postalCode, String country) {

    /** Applied when {@code country} is absent: this library serves one jurisdiction. */
    public static final String DEFAULT_COUNTRY = "MK";

    public Address {
        refuseBlank(street, "Address.street");
        refuseBlank(city, "Address.city");
        refuseBlank(postalCode, "Address.postalCode");
        refuseBlank(country, "Address.country");
        if (country == null) {
            country = DEFAULT_COUNTRY;
        }
    }

    /** The first field a complete business address is missing, qualified for a message, or null. */
    String missingCompanyField() {
        if (street == null) {
            return "address.street";
        }
        if (postalCode == null) {
            return "address.postalCode";
        }
        if (city == null) {
            return "address.city";
        }
        return null;
    }

    private static void refuseBlank(String value, String field) {
        if (value != null && value.isBlank()) {
            throw new InvoiceValidationException(field + " must not be blank; omit it instead");
        }
    }
}
