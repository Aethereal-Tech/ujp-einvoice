package net.aetherealtech.ujpeinvoice.model;

/**
 * A seller or buyer on an invoice, in one of two shapes.
 *
 * <p>A <strong>business</strong> ({@link #company}) carries everything: name, ЕДБ, and a complete
 * address. That is what a seller always is — {@link Invoice} requires it — and what a B2B buyer
 * normally is.
 *
 * <p>A <strong>natural person</strong> ({@link #naturalPerson}) carries a name and nothing else
 * that is required: no tax id, and an address that may be partial or absent entirely. Private
 * individuals buy things, and this library will not demand a city or a street from them ahead of a
 * specification nobody has read. If the eventual spec turns out to require more, that is a refusal
 * for the consumer to raise, by name, before it ever calls this library — not a rule to guess at
 * here, where guessing wrong means refusing invoices that are perfectly legal.
 *
 * <p>{@code name} is one string either way: an organization's registered name, or a person's name
 * composed however the consumer composes it (given plus family, or a single display name).
 *
 * <p>{@code taxId} is the ЕДБ (Единствен Даночен Број / unique tax number) — a public, well-known
 * identifier format in North Macedonia, not part of the unverified UJP wire spec. This library
 * requires it on a business and does not enforce the 13-digit format, so that legitimate edge cases
 * (foreign counterparties invoiced cross-border, sandbox test fixtures) are not rejected by a rule
 * this library cannot fully confirm.
 */
public record Party(String name, String taxId, Address address) {

    public Party {
        if (name == null || name.isBlank()) {
            throw new InvoiceValidationException("Party.name must not be blank");
        }
        if (taxId != null && taxId.isBlank()) {
            throw new InvoiceValidationException("Party.taxId must not be blank; omit it instead");
        }
    }

    /**
     * A business: name, ЕДБ and a complete address, each refused by name if missing. This is the
     * only shape a seller may take.
     */
    public static Party company(String name, String taxId, Address address) {
        Party party = new Party(name, taxId, address);
        String missing = party.missingCompanyField();
        if (missing != null) {
            throw new InvoiceValidationException("Party.company requires " + missing
                    + "; use Party.naturalPerson for a private individual");
        }
        return party;
    }

    /** A private individual, known by name alone. */
    public static Party naturalPerson(String name) {
        return new Party(name, null, null);
    }

    /**
     * A private individual with whatever address is on record — any part of it may be absent, and
     * an absent country is read as {@link Address#DEFAULT_COUNTRY}.
     */
    public static Party naturalPerson(String name, Address address) {
        return new Party(name, null, address);
    }

    /** Whether this party is a private individual: a party without a tax id is one by definition. */
    public boolean isNaturalPerson() {
        return taxId == null;
    }

    /** The first field a business is missing, qualified for a message, or null when complete. */
    String missingCompanyField() {
        if (taxId == null) {
            return "taxId";
        }
        if (address == null) {
            return "address";
        }
        return address.missingCompanyField();
    }
}
