package net.aetherealtech.ujpeinvoice.model;

/**
 * A seller or buyer on an invoice.
 *
 * <p>{@code taxId} is the ЕДБ (Единствен Даночен Број / unique tax number) — a public, well-known
 * identifier format in North Macedonia, not part of the unverified UJP wire spec. This library only
 * requires it to be present; it does not enforce the 13-digit format so that legitimate edge cases
 * (foreign counterparties invoiced cross-border, sandbox test fixtures) are not rejected by a rule
 * this library cannot fully confirm.
 */
public record Party(String name, String taxId, Address address) {

    public Party {
        if (name == null || name.isBlank()) {
            throw new InvoiceValidationException("Party.name must not be blank");
        }
        if (taxId == null || taxId.isBlank()) {
            throw new InvoiceValidationException("Party.taxId must not be blank");
        }
        if (address == null) {
            throw new InvoiceValidationException("Party.address must not be null");
        }
    }
}
