package net.aetherealtech.ujpeinvoice.transport;

import net.aetherealtech.ujpeinvoice.ProvisionalSpec;

/**
 * The UJP gateway rejected a request, or the request could not be completed against it at all (see
 * {@link UjpTransportException} for the latter). {@link #errorCode()} is {@code null} when the
 * gateway did not return one recognizable (or the failure predates a response, e.g. a connection
 * failure).
 *
 * <p>The named constants below are error codes observed in hands-on Macedonian integrator accounts.
 * Only {@link #E1012_CERTIFICATE_NOT_REGISTERED} has a documented meaning behind it; the others are
 * codes that were seen, without a confirmed explanation of what triggers them. Treat their names as
 * this library's placeholder, not a confirmed description.
 */
public class UjpException extends RuntimeException {

    @ProvisionalSpec("Code and meaning reported by integrators: the signing certificate was not "
            + "pre-registered at eujptest.ujp.gov.mk/ureg before submission.")
    public static final String E1012_CERTIFICATE_NOT_REGISTERED = "E1012";

    @ProvisionalSpec("Code observed in integrator reports; no confirmed explanation of its meaning found.")
    public static final String E5004 = "E5004";

    @ProvisionalSpec("Code observed in integrator reports; no confirmed explanation of its meaning found.")
    public static final String E10001 = "E10001";

    @ProvisionalSpec("Code observed in integrator reports; no confirmed explanation of its meaning found.")
    public static final String E10002 = "E10002";

    @ProvisionalSpec("Code observed in integrator reports; no confirmed explanation of its meaning found.")
    public static final String E10003 = "E10003";

    private final int httpStatus;
    private final String errorCode;

    public UjpException(int httpStatus, String errorCode, String message) {
        this(httpStatus, errorCode, message, null);
    }

    public UjpException(int httpStatus, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    /** The HTTP status code, or -1 if the failure occurred before a response was received. */
    public int httpStatus() {
        return httpStatus;
    }

    /** The gateway's own error code from the response body, or {@code null} if none was present. */
    public String errorCode() {
        return errorCode;
    }
}
