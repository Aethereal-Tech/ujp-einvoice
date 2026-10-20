package net.aetherealtech.ujpeinvoice.transport;

import java.time.Instant;

/**
 * The gateway's answer to an accepted submission, as D1 „Праќање на документ" shows it:
 * {@code {euid, message, qr_link, status, e_invoice_user, timestamp}}.
 *
 * <ul>
 *   <li>{@code euid}: the document's unique identifier, to be printed on it;</li>
 *   <li>{@code qrLink}: the verification link the document must show as a QR code ({@code qr_link} on the wire);</li>
 *   <li>{@code status}: an integer, {@code 200} in the example, with D1's gloss „Дали барањето било успешно";
 *       it is <em>not</em> a document status, which {@link SubmissionStatus} carries;</li>
 *   <li>{@code eInvoiceUser}: {@code e_invoice_user}, a boolean D1 does not explain;</li>
 *   <li>{@code timestamp}: the time of receipt, an ISO-8601 instant ({@code 2026-01-05T08:26:07.846Z}).</li>
 * </ul>
 *
 * <p>Every member may be {@code null}: a document the gateway has accepted must never be reported as failed
 * because a member was absent or in a shape this library does not read, so a member that is missing, of another
 * type, or (for {@code timestamp}) not an ISO instant reads as {@code null}. {@code responseBody} is the body
 * exactly as received, and holds whatever the typed members did not.
 */
public record SubmissionResult(String euid, String message, String qrLink, Integer status, Boolean eInvoiceUser,
        Instant timestamp, String responseBody) {
}
