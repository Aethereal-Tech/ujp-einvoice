package net.aetherealtech.ujpeinvoice.transport;

/**
 * The gateway could not be reached, or its response could not be understood at all (network
 * failure, timeout, non-JSON body, unexpected shape). Distinct from the base {@link UjpException}
 * case, where the gateway was reached and answered with a well-formed rejection — this is for
 * everything short of that.
 */
public final class UjpTransportException extends UjpException {

    public UjpTransportException(String message, Throwable cause) {
        super(-1, null, message, cause);
    }
}
