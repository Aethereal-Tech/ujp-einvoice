package net.aetherealtech.ujpeinvoice.transport;

import net.aetherealtech.ujpeinvoice.ProvisionalSpec;

import java.net.URI;

/**
 * Every UJP e-Faktura HTTP endpoint this library calls, gathered in one place so a corrected value
 * only ever needs to change here.
 *
 * <p>One host serves two path roots: {@code /einvoice_api} (the services: server time, status, code lists)
 * and {@code /JSONReceiver} (the intake of a signed document). The paths below carry their root in full, so
 * a client needs only the host as its base URL.
 *
 * <p>Sources, as of the pages retrieved 2026-10-19 and the API description fetched 2026-10-20:
 * <ul>
 *   <li><strong>D1</strong>, „API Спецификација" ({@code https://efakturawiki.ujp.gov.mk/тест_апи}), sections
 *       „API Documentation", „Време на сервер", „Праќање на документ", „Статус историја на документ" and
 *       „Тековен статус на излезна фактура";</li>
 *   <li>the <strong>API description</strong> of {@code einvoice_api}
 *       ({@code specification-status/openapi-2026-10-20.md}, committed at
 *       {@code src/test/resources/ujp/einvoice-api-openapi-2026-10-20.json}), which covers the {@code
 *       /einvoice_api} paths only. The JSON Receiver has no published description.</li>
 * </ul>
 * The one constant that no obtained document states is {@link #PRODUCTION_BASE_URL}; it keeps its marker and
 * is used by nothing. See {@code CLAUDE.md}: nothing here may be promoted without citing the section that
 * confirms it.
 */
public final class UjpEndpoints {

    private UjpEndpoints() {
    }

    /**
     * The sandbox host. D1 „API Documentation": {@code https://efakturatest.ujp.gov.mk/einvoice_api/} for the
     * services and {@code https://efakturatest.ujp.gov.mk/JSONReceiver/api/v1/sales-invoices/send} for the
     * intake. The client never defaults to it: pass it to the builder deliberately.
     */
    public static final URI TEST_BASE_URL = URI.create("https://efakturatest.ujp.gov.mk");

    /**
     * Production environment base URL. No obtained document names a production host: every URL in D1 and in the
     * API description is the sandbox. This is the sandbox name with "test" removed, never observed. The client
     * does not use it as a default, and it must be confirmed with УЈП before any use.
     */
    @ProvisionalSpec("Inferred from the sandbox hostname by pattern; no obtained document names a production host.")
    public static final URI PRODUCTION_BASE_URL = URI.create("https://efaktura.ujp.gov.mk");

    /**
     * {@code GET}: the gateway's clock, in the format {@code requestTimestamp} takes. D1 „Време на сервер":
     * {@code GET - /api/v1/server-time}, answering {@code {"timestamp": "2025-11-19T10:45:51"}}; the API
     * description's operation {@code getServerTime} places it under the {@code /einvoice_api} server.
     */
    public static final String SERVER_TIME = "/einvoice_api/api/v1/server-time";

    /**
     * {@code POST}: submit a signed sales invoice. D1 „Праќање на документ":
     * {@code https://efakturatest.ujp.gov.mk/JSONReceiver/api/v1/sales-invoices/send}, body
     * {@code {requestTimestamp, jws}}. The only endpoint under {@code /JSONReceiver}.
     */
    public static final String SALES_INVOICE_SEND = "/JSONReceiver/api/v1/sales-invoices/send";

    /**
     * {@code POST}: a sales invoice's current status. D1 „Тековен статус на излезна фактура":
     * {@code POST /api/v1/documents/sales-invoice/current-status}; the API description's operation
     * {@code getSalesInvoicesCurrentStatus}, body {@code {jws}}, answering a
     * {@code DocumentCurrentStatusResponse}.
     */
    public static final String SALES_INVOICE_CURRENT_STATUS =
            "/einvoice_api/api/v1/documents/sales-invoice/current-status";

    /**
     * {@code POST}: a sales invoice's status history. D1 „Статус историја на документ":
     * {@code POST /api/v1/documents/sales-invoice/status}; the API description's operation
     * {@code getStatusSalesInvoice}, body {@code {jws}}, answering a {@code DocumentStatusChangesResponse}.
     */
    public static final String SALES_INVOICE_STATUS_HISTORY =
            "/einvoice_api/api/v1/documents/sales-invoice/status";
}
