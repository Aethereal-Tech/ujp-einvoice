package net.aetherealtech.ujpeinvoice.model;

/**
 * How a tax indicator affects the VAT calculation, the axis the gateway's own validation branches
 * on (the API description's "Валидација на даночни индикатори", /тест_апи). The first four are
 * described there; {@link #MARGINALNA} appears only in the gateway's {@code TaxIndicatorDto}
 * enumeration (openapi-2026-10-20) with no description of its arithmetic.
 */
public enum VatImpact {

    /** VAT is calculated at the group's rate and shown. */
    STANDARD,

    /** A 0% item: no VAT is calculated and 0 is shown. */
    NULA,

    /** Turnover exempt from VAT: no VAT is calculated or shown, 0 is sent. */
    OSLOBODEN,

    /**
     * Transferred tax liability under article 32: no VAT on the items or the document totals, but
     * it is shown in the totals per tax indicator, with a note.
     */
    PRENESEN,

    /** Margin scheme. How the gateway computes it is not described by any obtained document. */
    MARGINALNA
}
