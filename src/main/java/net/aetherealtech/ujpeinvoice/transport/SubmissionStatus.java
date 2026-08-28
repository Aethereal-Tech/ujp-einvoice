package net.aetherealtech.ujpeinvoice.transport;

import net.aetherealtech.ujpeinvoice.ProvisionalSpec;

import java.util.Locale;

/**
 * The lifecycle state of a submitted invoice. PROVISIONAL in its entirety: the status vocabulary
 * itself (as opposed to any specific wire spelling of it) is this library's own guess at what a
 * gateway of this kind is likely to report, not something confirmed for UJP specifically.
 *
 * <p>{@link #fromWireValue(String)} maps a handful of plausible wire spellings onto these four
 * states and falls back to {@link #UNKNOWN} for anything else, rather than throwing — a status this
 * library has not seen before should surface as "figure this out", not crash a caller's submission
 * pipeline.
 */
@ProvisionalSpec("Status vocabulary and wire spellings are this library's own guess, not confirmed against the official spec.")
public enum SubmissionStatus {

    /** Received and awaiting processing/validation. */
    PENDING,

    /** Processed and accepted; an EUID and QR link should be present. */
    ACCEPTED,

    /** Processed and rejected; a message (and, on the exception path, an error code) should explain why. */
    REJECTED,

    /** A status value this library does not recognize. See {@link #fromWireValue(String)}. */
    UNKNOWN;

    public static SubmissionStatus fromWireValue(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        return switch (value.toUpperCase(Locale.ROOT)) {
            case "PENDING", "IN_PROGRESS", "PROCESSING" -> PENDING;
            case "ACCEPTED", "SUCCESS", "OK", "SENT" -> ACCEPTED;
            case "REJECTED", "FAILED", "ERROR" -> REJECTED;
            default -> UNKNOWN;
        };
    }
}
