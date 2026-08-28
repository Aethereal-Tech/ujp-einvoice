package net.aetherealtech.ujpeinvoice.transport;

/**
 * The gateway's response to a submission or a status poll: the EUID (the gateway's identifier for
 * the invoice), a QR link (for the human-readable verification page), the current status, and an
 * optional human-readable message. {@code euid} and {@code qrLink} may be {@code null} while a
 * submission is still {@link SubmissionStatus#PENDING}.
 */
public record SubmissionResult(String euid, String qrLink, SubmissionStatus status, String message) {
}
