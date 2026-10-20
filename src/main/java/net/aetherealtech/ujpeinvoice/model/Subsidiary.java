package net.aetherealtech.ujpeinvoice.model;

/**
 * A branch of a party (UJP03-08 to UJP03-10, UJP05-08 to UJP05-10), for a buyer the place of
 * delivery. Every part is optional, but a subsidiary with none is refused: it would say nothing. The
 * address is one string of up to 100 characters, as the page types it.
 */
public record Subsidiary(String code, String name, String address) {

    public Subsidiary {
        code = Check.optional(code, "UJP03-08", "subsidiary.code", 20);
        name = Check.optional(name, "UJP03-09", "subsidiary.name", 150);
        address = Check.optional(address, "UJP03-10", "subsidiary.address", 100);
        Check.that(code != null || name != null || address != null, "UJP03-08", "subsidiary",
                "needs a code, a name or an address");
    }
}
