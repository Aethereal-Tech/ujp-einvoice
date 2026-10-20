package net.aetherealtech.ujpeinvoice.transport;

import net.aetherealtech.ujpeinvoice.ProvisionalSpec;
import net.aetherealtech.ujpeinvoice.internal.json.JsonArray;
import net.aetherealtech.ujpeinvoice.internal.json.JsonBoolean;
import net.aetherealtech.ujpeinvoice.internal.json.JsonNumber;
import net.aetherealtech.ujpeinvoice.internal.json.JsonObject;
import net.aetherealtech.ujpeinvoice.internal.json.JsonParser;
import net.aetherealtech.ujpeinvoice.internal.json.JsonString;
import net.aetherealtech.ujpeinvoice.internal.json.JsonValue;
import net.aetherealtech.ujpeinvoice.internal.json.JsonWriter;
import net.aetherealtech.ujpeinvoice.model.Document;
import net.aetherealtech.ujpeinvoice.serialization.Serializer;
import net.aetherealtech.ujpeinvoice.signing.Signer;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Function;

/**
 * A client for the UJP e-Faktura gateway: serializes a {@link Document}, signs it, and posts it over
 * {@code java.net.http}; polls a document's status. Composed from {@link Serializer} and {@link Signer} as
 * interfaces, so a different format or a hardware-backed signer is a constructor argument, not a change here.
 *
 * <p><strong>Do not use this to file.</strong> The rule that this client is never called for a real submission
 * until the specification is verified, a qualified certificate is registered and its privileges are granted is
 * unchanged by this wire layer; nothing in this library or its tests contacts a UJP host.
 *
 * <h2>What a call does</h2>
 * Every call first reads the gateway's clock and puts it in the {@code requestTimestamp} of what it signs, and
 * caches nothing across calls (D1 „Валидација на requestTimestamp" allows ±5 minutes, {@code E40122} refuses
 * more; D1 „Време на сервер", {@code GET /api/v1/server-time}). It is therefore two requests, and with the
 * one-per-second limit (D1 „API Documentation") at least a second long.
 *
 * <ul>
 *   <li>{@link #submit}: {@code POST /JSONReceiver/api/v1/sales-invoices/send}, headers {@code X-EUJP-ID},
 *       {@code X-EDB}, {@code X-SERIAL-NUMBER} and {@code X-DOC-TYPE-CODE} (the document kind's code), body
 *       {@code {"requestTimestamp": ..., "jws": ...}} (D1 „Праќање на документ"). The timestamp is in
 *       <em>both</em> places, with one value: D1 shows it in the outer body, and says it belongs inside the
 *       signed payload, where it is part of the signature and defeats replay (D1 „Валидација на
 *       requestTimestamp"); the {@link Serializer} writes the inner one. The request's content type is JSON
 *       whatever {@link Serializer#contentType()} says of the payload it signed.</li>
 *   <li>{@link #status} and {@link #statusHistory}: {@code POST /einvoice_api/api/v1/documents/sales-invoice/...},
 *       headers {@code X-EUJP-ID}, {@code X-EDB} and {@code X-SERIAL-NUMBER}, and a body of the JWS alone,
 *       {@code {"jws": ...}} (the API description's {@code InvoiceSignedRequest}; the 27.05 changelog entry). The
 *       timestamp is inside the signed payload only, with the EUID.</li>
 * </ul>
 * The server-time read carries {@code X-EUJP-ID} and {@code X-EDB}: D1 requires them on all services, though the
 * API description declares no parameters on that operation. It carries no {@code X-SERIAL-NUMBER}, since nothing
 * is signed.
 *
 * <h2>Throttle</h2>
 * Requests are spaced at least one second apart per client instance, counting the server-time reads. D1 states the
 * limit per user, so two instances of one EUJP id are not throttled together; use one instance per user.
 *
 * <h2>Failures</h2>
 * A refusal is a {@link UjpException} with the HTTP status, the code, the namespace of the interface that raised
 * it ({@link ErrorNamespace#JSON_RECEIVER} for a send, {@link ErrorNamespace#EINVOICE_API} for the rest) and the
 * raw body. A request that never completed, or an answer that cannot be read, is a
 * {@link UjpTransportException}.
 */
public final class UjpClient {

    private static final String EUJP_ID_HEADER = "X-EUJP-ID";
    private static final String EDB_HEADER = "X-EDB";
    private static final String SERIAL_NUMBER_HEADER = "X-SERIAL-NUMBER";
    private static final String DOC_TYPE_CODE_HEADER = "X-DOC-TYPE-CODE";
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    @ProvisionalSpec("No obtained document states the send request's Content-Type; the body is JSON. The "
            + "einvoice_api operations declare application/json.")
    private static final String JSON = "application/json";

    private final HttpClient httpClient;
    private final URI baseUrl;
    private final String eujpId;
    private final String edb;
    private final String certificateSerialNumber;
    private final Serializer serializer;
    private final Signer signer;
    private final Throttle throttle;

    private UjpClient(Builder builder) {
        this.httpClient = builder.httpClient;
        this.baseUrl = builder.baseUrl;
        this.eujpId = builder.eujpId;
        this.edb = builder.edb;
        this.certificateSerialNumber = builder.certificateSerialNumber;
        this.serializer = builder.serializer;
        this.signer = builder.signer;
        this.throttle = builder.throttle;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Serializes, signs and submits {@code document}. Returns the gateway's receipt.
     *
     * @throws UjpException on a refusal, and {@link UjpTransportException} when the request failed or the answer
     *                      could not be read; after an unreadable 2xx answer the document may have been accepted,
     *                      so check {@link #status} before submitting it again
     */
    public SubmissionResult submit(Document document) {
        LocalDateTime requestTimestamp = serverTime();
        String jws = signer.signCompact(serializer.serialize(document, requestTimestamp));
        String body = JsonWriter.write(JsonObject.builder()
                .put("requestTimestamp", TIMESTAMP.format(requestTimestamp))
                .put("jws", jws)
                .build());
        HttpRequest request = signedRequest(UjpEndpoints.SALES_INVOICE_SEND, body)
                .header(DOC_TYPE_CODE_HEADER, document.kind().code())
                .build();
        return read(call(request, ErrorNamespace.JSON_RECEIVER), UjpClient::submissionResult);
    }

    /** Reads the current status of the document with {@code euid}. */
    public SubmissionStatus status(String euid) {
        requireEuid(euid);
        JsonObject payload = JsonObject.builder()
                .put("requestTimestamp", TIMESTAMP.format(serverTime()))
                .put("euid", euid)
                .build();
        HttpRequest request = signedRequest(UjpEndpoints.SALES_INVOICE_CURRENT_STATUS, jwsBody(payload)).build();
        return read(call(request, ErrorNamespace.EINVOICE_API), UjpClient::submissionStatus);
    }

    /**
     * Reads the status history of the document with {@code euid}: the changes from {@code dateFrom} on, or all of
     * them when {@code dateFrom} is {@code null}.
     */
    @ProvisionalSpec("dateFrom: a date-time in D1 ('dateFrom': '2025-12-25T13:07:30') but a date in the API "
            + "description's example ('2026-01-01', 'on or after this date, field lastModified'). Written as a "
            + "date, the machine-readable source; omitted when absent, which no document says is allowed.")
    public List<StatusChange> statusHistory(String euid, LocalDate dateFrom) {
        requireEuid(euid);
        JsonObject.Builder payload = JsonObject.builder()
                .put("requestTimestamp", TIMESTAMP.format(serverTime()))
                .put("euid", euid);
        if (dateFrom != null) {
            payload.put("dateFrom", dateFrom.toString());
        }
        HttpRequest request = signedRequest(UjpEndpoints.SALES_INVOICE_STATUS_HISTORY, jwsBody(payload.build()))
                .build();
        return read(call(request, ErrorNamespace.EINVOICE_API), UjpClient::statusChanges);
    }

    private LocalDateTime serverTime() {
        HttpRequest request = identified(UjpEndpoints.SERVER_TIME).GET().build();
        return read(call(request, ErrorNamespace.EINVOICE_API),
                reply -> LocalDateTime.parse(reply.json().getString("timestamp")).truncatedTo(ChronoUnit.SECONDS));
    }

    private String jwsBody(JsonObject payload) {
        String jws = signer.signCompact(JsonWriter.write(payload).getBytes(StandardCharsets.UTF_8));
        return JsonWriter.write(JsonObject.builder().put("jws", jws).build());
    }

    private HttpRequest.Builder identified(String path) {
        return HttpRequest.newBuilder(baseUrl.resolve(path))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", JSON)
                .header(EUJP_ID_HEADER, eujpId)
                .header(EDB_HEADER, edb);
    }

    private HttpRequest.Builder signedRequest(String path, String body) {
        return identified(path)
                .header(SERIAL_NUMBER_HEADER, certificateSerialNumber)
                .header("Content-Type", JSON)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
    }

    private Reply call(HttpRequest request, ErrorNamespace namespace) {
        HttpResponse<String> response;
        try {
            throttle.acquire();
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UjpTransportException("I/O failure calling the UJP gateway at " + request.uri(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UjpTransportException("Interrupted while calling the UJP gateway at " + request.uri(), e);
        }
        String body = response.body();
        int httpStatus = response.statusCode();
        if (httpStatus >= 400) {
            throw refusal(httpStatus, namespace, body);
        }
        if (!(parseOrNull(body) instanceof JsonObject json)) {
            throw new UjpTransportException(httpStatus, namespace, body,
                    "UJP gateway returned a response that is not a JSON object (HTTP " + httpStatus + ")", null);
        }
        if (json.get("errorStatus").orElse(null) instanceof JsonObject) {
            throw refusal(httpStatus, namespace, body);
        }
        return new Reply(httpStatus, namespace, body, json);
    }

    @ProvisionalSpec("The JSON Receiver's error body is unpublished. The two shapes einvoice_api declares (a "
            + "GenericAPIResponse carrying errorStatus, or a bare ErrorStatusBean) are tried for it too, and "
            + "anything else is kept raw. The HTTP status is authoritative.")
    private static UjpException refusal(int httpStatus, ErrorNamespace namespace, String body) {
        ErrorStatus error = ErrorStatus.fromResponseBody(body).orElse(null);
        String code = error == null ? null : error.errorCode();
        String message = error != null && error.errorMessage() != null
                ? error.errorMessage()
                : "UJP gateway returned HTTP " + httpStatus;
        return new UjpException(httpStatus, namespace, code, body, message, null);
    }

    private static <T> T read(Reply reply, Function<Reply, T> reader) {
        try {
            return reader.apply(reply);
        } catch (RuntimeException e) {
            throw new UjpTransportException(reply.httpStatus(), reply.namespace(), reply.body(),
                    "UJP gateway returned a response this library cannot read (HTTP " + reply.httpStatus() + ")", e);
        }
    }

    private static JsonValue parseOrNull(String body) {
        try {
            return JsonParser.parse(body);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static SubmissionResult submissionResult(Reply reply) {
        JsonObject json = reply.json();
        return new SubmissionResult(text(json, "euid"), text(json, "message"), text(json, "qr_link"),
                integer(json, "status"), bool(json, "e_invoice_user"), instant(json, "timestamp"), reply.body());
    }

    private static SubmissionStatus submissionStatus(Reply reply) {
        JsonObject json = reply.json().getObject("docStatus");
        return new SubmissionStatus(text(json, "statusCode"), text(json, "statusName"), text(json, "euid"),
                text(json, "docNumber"), text(json, "docType"), text(json, "docTypeName"),
                date(json, "docDate"), date(json, "docDeliveryDate"),
                decimal(json, "docNetAmount"), decimal(json, "docVatAmount"), decimal(json, "docGrossAmountR"),
                decimal(json, "docAvansAmount"), decimal(json, "docFinalAmount"),
                text(json, "sellerTin"), text(json, "buyerTin"), text(json, "senderTin"), text(json, "receiverTin"),
                text(json, "comment"), text(json, "cancelComment"),
                reason(text(json, "voidReasonCode"), text(json, "voidReason"), text(json, "voidComment")),
                reason(text(json, "rejectReasonCode"), text(json, "rejectReason"), null),
                reason(text(json, "correctionReasonCode"), text(json, "correctionReason"),
                        text(json, "correctionReasonComment")),
                reply.body());
    }

    private static List<StatusChange> statusChanges(Reply reply) {
        if (!(reply.json().get("statusHistory").orElse(null) instanceof JsonArray history)) {
            return List.of();
        }
        return history.items().stream()
                .filter(JsonObject.class::isInstance)
                .map(item -> statusChange((JsonObject) item))
                .toList();
    }

    private static StatusChange statusChange(JsonObject json) {
        JsonObject oldStatus = objectOrEmpty(json, "oldStatus");
        JsonObject newStatus = objectOrEmpty(json, "newStatus");
        return new StatusChange(text(json, "euid"), text(oldStatus, "code"), text(oldStatus, "name"),
                text(newStatus, "code"), text(newStatus, "name"), dateTime(json, "timestamp"),
                text(json, "comment"), text(json, "cancelComment"),
                reason(text(json, "voidReasonCode"), text(json, "voidReason"), text(json, "voidComment")),
                reason(text(json, "rejectReasonCode"), text(json, "rejectReason"), null));
    }

    private static JsonObject objectOrEmpty(JsonObject json, String key) {
        return json.get(key).orElse(null) instanceof JsonObject object ? object : JsonObject.builder().build();
    }

    private static StatusReason reason(String code, String text, String comment) {
        return code == null && text == null && comment == null ? null : new StatusReason(code, text, comment);
    }

    private static String text(JsonObject json, String key) {
        return switch (json.get(key).orElse(null)) {
            case JsonString s -> s.value();
            case JsonNumber n -> n.value().toPlainString();
            case null, default -> null;
        };
    }

    private static BigDecimal decimal(JsonObject json, String key) {
        return json.get(key).orElse(null) instanceof JsonNumber n ? n.value() : null;
    }

    private static Integer integer(JsonObject json, String key) {
        BigDecimal value = decimal(json, key);
        try {
            return value == null ? null : value.intValueExact();
        } catch (ArithmeticException e) {
            return null;
        }
    }

    private static Boolean bool(JsonObject json, String key) {
        return json.get(key).orElse(null) instanceof JsonBoolean b ? b.value() : null;
    }

    private static Instant instant(JsonObject json, String key) {
        String value = text(json, key);
        try {
            return value == null ? null : Instant.parse(value);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static LocalDate date(JsonObject json, String key) {
        String value = text(json, key);
        try {
            return value == null ? null : LocalDate.parse(value);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static LocalDateTime dateTime(JsonObject json, String key) {
        String value = text(json, key);
        try {
            return value == null ? null : LocalDateTime.parse(value);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static void requireEuid(String euid) {
        if (euid == null || euid.isBlank()) {
            throw new IllegalArgumentException("euid must not be blank");
        }
    }

    private record Reply(int httpStatus, ErrorNamespace namespace, String body, JsonObject json) {
    }

    /**
     * Builds a {@link UjpClient}. Everything but the {@link HttpClient} is required, and nothing defaults to a
     * host: the base URL is the caller's decision, never this library's.
     */
    public static final class Builder {
        private URI baseUrl;
        private String eujpId;
        private String edb;
        private String certificateSerialNumber;
        private Serializer serializer;
        private Signer signer;
        private HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        private Throttle throttle = Throttle.oncePerSecond();

        private Builder() {
        }

        /**
         * The gateway's host, without a path: the endpoints carry their own {@code /einvoice_api} or
         * {@code /JSONReceiver} root. {@link UjpEndpoints#TEST_BASE_URL} is the sandbox. There is no default.
         */
        public Builder baseUrl(URI baseUrl) {
            this.baseUrl = requireNonNull(baseUrl, "baseUrl");
            return this;
        }

        /** The signer's e-УЈП identifier, sent as {@code X-EUJP-ID} (D1 „API Documentation"). */
        public Builder eujpId(String eujpId) {
            this.eujpId = requireText(eujpId, "eujpId");
            return this;
        }

        /** The ЕДБ of the company the documents are issued for, sent as {@code X-EDB}. */
        public Builder edb(String edb) {
            this.edb = requireText(edb, "edb");
            return this;
        }

        /**
         * The serial number of the signing certificate, sent as {@code X-SERIAL-NUMBER} on every signed call.
         * No obtained document states its format; it is sent as given.
         */
        public Builder certificateSerialNumber(String certificateSerialNumber) {
            this.certificateSerialNumber = requireText(certificateSerialNumber, "certificateSerialNumber");
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
            this.httpClient = requireNonNull(httpClient, "httpClient");
            return this;
        }

        /** For tests: replaces the one-per-second throttle, whose clock and sleeper it carries. */
        Builder throttle(Throttle throttle) {
            this.throttle = requireNonNull(throttle, "throttle");
            return this;
        }

        public UjpClient build() {
            require(baseUrl, "baseUrl");
            require(eujpId, "eujpId");
            require(edb, "edb");
            require(certificateSerialNumber, "certificateSerialNumber");
            require(serializer, "serializer");
            require(signer, "signer");
            return new UjpClient(this);
        }

        private static void require(Object value, String name) {
            if (value == null) {
                throw new IllegalStateException(name + " is required");
            }
        }

        private static <T> T requireNonNull(T value, String name) {
            if (value == null) {
                throw new NullPointerException(name + " must not be null");
            }
            return value;
        }

        private static String requireText(String value, String name) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(name + " must not be blank");
            }
            return value;
        }
    }
}
