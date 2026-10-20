package net.aetherealtech.ujpeinvoice.model;

/**
 * The seller or the buyer. The seller is always an identified business. A buyer is a business with
 * a ЕДБ, or a non-resident with a foreign tax number; a private individual (kind 170) is out of
 * scope, so {@link #naturalPerson} exists to be refused by name where a business is required.
 *
 * <p>The ЕДБ is required and not format-checked: a 13-digit rule would reject legitimate
 * cross-border counterparties and fixture cases the library cannot confirm.
 */
public record Party(Country country, String tin, String foreignTin, String vatNumber, String name, Address address,
                    Subsidiary subsidiary, String contact, String email) {

    public Party {
        country = country == null ? Country.MK : country;
        tin = Check.optional(tin, "UJP03-03", "tin", 30);
        foreignTin = Check.optional(foreignTin, "UJP05-04", "foreignTin", 30);
        vatNumber = Check.optional(vatNumber, "UJP03-05", "vatNumber", 30);
        name = Check.required(name, "UJP03-06", "name", 150);
        contact = Check.optional(contact, "UJP03-11", "contact", 200);
        email = Check.optional(email, "UJP03-12", "email", 60);
    }

    /** A business known by its ЕДБ, with a complete address. */
    public static Party company(String name, String tin, Address address) {
        Check.required(tin, "UJP03-03", "tin", 30);
        Check.present(address, "UJP03-07", "address");
        return new Party(Country.MK, tin, null, null, name, address, null, null, null);
    }

    /** A non-resident business: a foreign tax number (UJP05-04) in place of a ЕДБ. */
    public static Party foreignCompany(Country country, String foreignTin, String name, Address address) {
        Check.required(foreignTin, "UJP05-04", "foreignTin", 30);
        Check.present(address, "UJP05-07", "address");
        return new Party(country, null, foreignTin, null, name, address, null, null, null);
    }

    /** A private individual known by name; refused wherever a business is required. */
    public static Party naturalPerson(String name) {
        return new Party(Country.MK, null, null, null, name, null, null, null, null);
    }

    /** A private individual known by name and whatever address is on record. */
    public static Party naturalPerson(String name, Address address) {
        return new Party(Country.MK, null, null, null, name, address, null, null, null);
    }

    /** True exactly when the party carries no tax number of either kind. */
    public boolean isNaturalPerson() {
        return tin == null && foreignTin == null;
    }

    public Party withCountry(Country value) {
        return new Party(value, tin, foreignTin, vatNumber, name, address, subsidiary, contact, email);
    }

    public Party withForeignTin(String value) {
        return new Party(country, tin, value, vatNumber, name, address, subsidiary, contact, email);
    }

    public Party withVatNumber(String value) {
        return new Party(country, tin, foreignTin, value, name, address, subsidiary, contact, email);
    }

    public Party withSubsidiary(Subsidiary value) {
        return new Party(country, tin, foreignTin, vatNumber, name, address, value, contact, email);
    }

    public Party withContact(String value) {
        return new Party(country, tin, foreignTin, vatNumber, name, address, subsidiary, value, email);
    }

    public Party withEmail(String value) {
        return new Party(country, tin, foreignTin, vatNumber, name, address, subsidiary, contact, value);
    }
}
