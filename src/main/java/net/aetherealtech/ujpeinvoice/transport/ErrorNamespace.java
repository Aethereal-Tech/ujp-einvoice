package net.aetherealtech.ujpeinvoice.transport;

/**
 * The three error catalogues D5 publishes, one per interface. D5 is the official error catalogue,
 * {@code https://efakturawiki.ujp.gov.mk/error-codes}, page {@code updated-at} 2026-10-05 and retrieved
 * 2026-10-19. A code's identity is the pair (namespace, code): the same string means different things
 * in different catalogues, so a code is never matched on its text alone.
 */
public enum ErrorNamespace {

    /**
     * D5, section "Error Codes - JSON Receiver" (page updated 2026-10-05, retrieved 2026-10-19): the
     * interface that takes the signed document.
     */
    JSON_RECEIVER("JSON Receiver"),

    /**
     * D5, section "Error Codes - eInvoiceApi" (page updated 2026-10-05, retrieved 2026-10-19): the
     * {@code /einvoice_api} services. Its error body is {@code ErrorStatus}, per
     * {@code openapi-2026-10-20.md}.
     */
    EINVOICE_API("eInvoiceApi"),

    /**
     * D5, section "Error Codes - Web App" (page updated 2026-10-05, retrieved 2026-10-19): the
     * browser application's own interface.
     */
    WEB_APP("Web App");

    private final String catalogueName;

    ErrorNamespace(String catalogueName) {
        this.catalogueName = catalogueName;
    }

    /** The name D5 gives this catalogue in its section heading, e.g. {@code "JSON Receiver"}. */
    public String catalogueName() {
        return catalogueName;
    }
}
