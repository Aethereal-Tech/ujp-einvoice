package net.aetherealtech.ujpeinvoice.transport;

/**
 * The gateway could not be reached, or its response could not be understood at all (network
 * failure, timeout, non-JSON body, unexpected shape). Distinct from the base {@link UjpException}
 * case, where the gateway was reached and answered with a well-formed rejection — this is for
 * everything short of that.
 *
 * <p>When a response did arrive but could not be read, {@link #httpStatus()} and {@link #responseBody()} carry
 * it. For a send that is the dangerous case: an unreadable 2xx may mean the document <em>was</em> accepted, so
 * the status of the document must be checked before it is submitted again.
 */
public final class UjpTransportException extends UjpException {

    public UjpTransportException(String message, Throwable cause) {
        super(-1, null, message, cause);
    }

    public UjpTransportException(int httpStatus, ErrorNamespace namespace, String responseBody, String message,
            Throwable cause) {
        super(httpStatus, namespace, null, responseBody, message, cause);
    }
}
