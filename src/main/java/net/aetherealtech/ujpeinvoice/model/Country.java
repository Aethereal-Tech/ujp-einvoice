package net.aetherealtech.ujpeinvoice.model;

/**
 * A party's country as the code list gives it (UJP03-01/-02, UJP05-01/-02): an ISO code and the
 * country's name. The code list is named by the gateway, not closed here.
 */
public record Country(String code, String name) {

    /** The default for a party: the examples all use the Latin letters MK. */
    public static final Country MK = new Country("MK", "Северна Македонија");

    public Country {
        code = Check.required(code, "UJP03-01", "country.code", 3);
        name = Check.required(name, "UJP03-02", "country.name", 50);
    }
}
