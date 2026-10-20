package net.aetherealtech.ujpeinvoice.transport;

import java.util.Optional;

/**
 * The UJP gateway rejected a request, or the request could not be completed against it at all (see
 * {@link UjpTransportException} for the latter). {@link #errorCode()} is {@code null} when the
 * gateway did not return one recognizable (or the failure predates a response, e.g. a connection
 * failure).
 *
 * <p>A code means something only within the interface that raised it, so {@link #namespace()} travels with
 * {@link #errorCode()} and {@link #known()} resolves the pair against D5, the official error catalogue
 * (efakturawiki.ujp.gov.mk, {@code /error-codes}, updated 2026-10-05, retrieved 2026-10-19). Where the
 * interface is not known, the code is not resolved at all: matching on the string alone would pick a
 * meaning for it, and the same string means different things in different catalogues.
 *
 * <p>The raw response body is kept, when there was one, so that nothing the gateway said is lost to what
 * this library knows how to read.
 */
public class UjpException extends RuntimeException {

    private final int httpStatus;
    private final ErrorNamespace namespace;
    private final String errorCode;
    private final String responseBody;

    public UjpException(int httpStatus, String errorCode, String message) {
        this(httpStatus, errorCode, message, null);
    }

    public UjpException(int httpStatus, String errorCode, String message, Throwable cause) {
        this(httpStatus, null, errorCode, null, message, cause);
    }

    public UjpException(int httpStatus, ErrorNamespace namespace, String errorCode, String responseBody,
            String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.namespace = namespace;
        this.errorCode = errorCode;
        this.responseBody = responseBody;
    }

    /** The HTTP status code, or -1 if the failure occurred before a response was received. */
    public int httpStatus() {
        return httpStatus;
    }

    /** The gateway's own error code from the response body, or {@code null} if none was present. */
    public String errorCode() {
        return errorCode;
    }

    /** The interface that raised {@link #errorCode()}, or {@code null} when it is not known. */
    public ErrorNamespace namespace() {
        return namespace;
    }

    /** The response body exactly as received, or {@code null} when there was none. */
    public String responseBody() {
        return responseBody;
    }

    /**
     * The catalogue entry for {@link #errorCode()} in {@link #namespace()}, or empty when either is
     * absent or D5 lists no such code.
     */
    public Optional<UjpErrorCode> known() {
        return UjpErrorCodes.lookup(namespace, errorCode);
    }
}
