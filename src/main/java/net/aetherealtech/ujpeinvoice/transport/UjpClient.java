package net.aetherealtech.ujpeinvoice.transport;

import net.aetherealtech.ujpeinvoice.internal.json.JsonObject;
import net.aetherealtech.ujpeinvoice.internal.json.JsonParser;
import net.aetherealtech.ujpeinvoice.internal.json.JsonValue;
import net.aetherealtech.ujpeinvoice.model.Invoice;
import net.aetherealtech.ujpeinvoice.serialization.Serializer;
import net.aetherealtech.ujpeinvoice.signing.Signer;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * A client for the UJP e-Faktura gateway: serializes an {@link Invoice}, signs it, and posts the
 * result over {@code java.net.http}. Composed from {@link Serializer} and {@link Signer} rather than
 * hard-wiring either, so a corrected schema or a different signing setup is a constructor argument,
 * not a change to this class.
 *
 * <p>See {@link UjpEndpoints} for the base URLs and paths this client calls, and its javadoc (and
 * the README's "Specification status" section) for how much of that is confirmed versus
 * reconstructed. Every wire interaction this class performs is against a reconstructed shape; treat
 * responses gracefully and validate against a real sandbox account before relying on this in
 * production.
 */
public final class UjpClient {

    private final HttpClient httpClient;
    private final URI baseUrl;
    private final Serializer serializer;
    private final Signer signer;

    private UjpClient(Builder builder) {
        this.httpClient = builder.httpClient;
        this.baseUrl = builder.baseUrl;
        this.serializer = builder.serializer;
        this.signer = builder.signer;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Serializes, signs, and submits {@code invoice}. Throws {@link UjpException} on any failure. */
    public SubmissionResult submit(Invoice invoice) {
        byte[] payload = serializer.serialize(invoice);
        String jws = signer.signCompact(payload);
        HttpRequest request = HttpRequest.newBuilder(baseUrl.resolve(UjpEndpoints.SALES_INVOICE_SEND))
                .header("Content-Type", "application/jose")
                .POST(HttpRequest.BodyPublishers.ofString(jws, StandardCharsets.US_ASCII))
                .build();
        return execute(request);
    }

    /** Polls the current status of a previously submitted invoice by its EUID. */
    public SubmissionResult status(String euid) {
        if (euid == null || euid.isBlank()) {
            throw new IllegalArgumentException("euid must not be blank");
        }
        String path = UjpEndpoints.SALES_INVOICE_STATUS_TEMPLATE
                .formatted(URLEncoder.encode(euid, StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(baseUrl.resolve(path))
                .header("Accept", "application/json")
                .GET()
                .build();
        return execute(request);
    }

    private SubmissionResult execute(HttpRequest request) {
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UjpTransportException("I/O failure calling the UJP gateway at " + request.uri(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UjpTransportException("Interrupted while calling the UJP gateway at " + request.uri(), e);
        }
        return parseResponse(response);
    }

    private SubmissionResult parseResponse(HttpResponse<String> response) {
        JsonValue parsed;
        try {
            parsed = JsonParser.parse(response.body());
        } catch (RuntimeException e) {
            throw new UjpTransportException(
                    "UJP gateway returned a non-JSON response (HTTP " + response.statusCode() + ")", e);
        }
        if (!(parsed instanceof JsonObject json)) {
            throw new UjpTransportException(
                    "Expected a JSON object response from the UJP gateway, got: " + parsed, null);
        }

        if (response.statusCode() >= 400) {
            String errorCode = json.getOptionalString("errorCode");
            String errorMessage = json.getOptionalString("errorMessage");
            throw new UjpException(response.statusCode(), errorCode,
                    errorMessage != null ? errorMessage : "UJP gateway returned HTTP " + response.statusCode());
        }

        String euid = json.getOptionalString("euid");
        String qrLink = json.getOptionalString("qrLink");
        SubmissionStatus status = SubmissionStatus.fromWireValue(json.getOptionalString("status"));
        String message = json.getOptionalString("message");
        return new SubmissionResult(euid, qrLink, status, message);
    }

    /** Builds a {@link UjpClient}. {@code serializer} and {@code signer} are required. */
    public static final class Builder {
        private URI baseUrl = UjpEndpoints.TEST_BASE_URL;
        private Serializer serializer;
        private Signer signer;
        private HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        private Builder() {
        }

        /** Defaults to {@link UjpEndpoints#TEST_BASE_URL}. */
        public Builder baseUrl(URI baseUrl) {
            if (baseUrl == null) {
                throw new NullPointerException("baseUrl must not be null");
            }
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder serializer(Serializer serializer) {
            this.serializer = serializer;
            return this;
        }

        public Builder signer(Signer signer) {
            this.signer = signer;
            return this;
        }

        /** Overrides the default {@link HttpClient} (10s connect timeout, otherwise JDK defaults). */
        public Builder httpClient(HttpClient httpClient) {
            if (httpClient == null) {
                throw new NullPointerException("httpClient must not be null");
            }
            this.httpClient = httpClient;
            return this;
        }

        public UjpClient build() {
            if (serializer == null) {
                throw new IllegalStateException("serializer is required");
            }
            if (signer == null) {
                throw new IllegalStateException("signer is required");
            }
            return new UjpClient(this);
        }
    }
}
