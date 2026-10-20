package net.aetherealtech.ujpeinvoice.transport;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import net.aetherealtech.ujpeinvoice.internal.json.JsonNumber;
import net.aetherealtech.ujpeinvoice.internal.json.JsonObject;
import net.aetherealtech.ujpeinvoice.internal.json.JsonParser;
import net.aetherealtech.ujpeinvoice.internal.json.JsonString;
import net.aetherealtech.ujpeinvoice.internal.json.JsonValue;
import net.aetherealtech.ujpeinvoice.internal.json.JsonWriter;
import net.aetherealtech.ujpeinvoice.serialization.UjpJsonSerializer;
import net.aetherealtech.ujpeinvoice.signing.JwsRs256Signer;
import net.aetherealtech.ujpeinvoice.testsupport.GatewayBodies;
import net.aetherealtech.ujpeinvoice.testsupport.OpenApiSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.InputStream;
import java.net.URI;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the client against the gateway's own published API description of {@code einvoice_api}, committed at
 * {@code src/test/resources/ujp/} (task 9.1 of wire-layer-correction). It validates shape, not optionality, and
 * cannot see the JSON Receiver at all: read {@code ujp/README.md} before trusting a green run.
 */
class OpenApiConformanceTest {

    private static final String RESOURCE = "/ujp/einvoice-api-openapi-2026-10-20.json";
    private static final String SHA256 = "0c56a01ebd0e4ef6dde8f6e8612274a2f0dfc25ea830ed72b618eb70edc415a5";
    private static final OpenApiSchema API = OpenApiSchema.load(RESOURCE);

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance().build();

    private record Endpoint(String method, String path) {
    }

    private static final List<Endpoint> CALLED = List.of(
            new Endpoint("GET", UjpEndpoints.SERVER_TIME),
            new Endpoint("POST", UjpEndpoints.SALES_INVOICE_CURRENT_STATUS),
            new Endpoint("POST", UjpEndpoints.SALES_INVOICE_STATUS_HISTORY));

    private record Case(Endpoint endpoint, int httpStatus, String schema, String body) {
    }

    private static final Endpoint TIME = CALLED.get(0);
    private static final Endpoint CURRENT = CALLED.get(1);
    private static final Endpoint HISTORY = CALLED.get(2);

    private static final List<Case> BODIES = List.of(
            new Case(TIME, 200, "ServerTimeResponse", GatewayBodies.serverTime("2026-01-05T12:00:00")),
            new Case(TIME, 429, "GenericAPIResponse", GatewayBodies.wrappedError("E4007", "Rate limit exceeded")),
            new Case(CURRENT, 200, "DocumentCurrentStatusResponse", GatewayBodies.CURRENT_STATUS),
            new Case(CURRENT, 200, "DocumentCurrentStatusResponse", GatewayBodies.UNKNOWN_STATUS),
            new Case(CURRENT, 400, "ErrorStatusBean", GatewayBodies.bareError("E4013", "JSON schema validation failed")),
            new Case(CURRENT, 404, "GenericAPIResponse", GatewayBodies.wrappedError("E4008", "Not found")),
            new Case(CURRENT, 429, "GenericAPIResponse", GatewayBodies.wrappedError("E4007", "Rate limit exceeded")),
            new Case(HISTORY, 200, "DocumentStatusChangesResponse", GatewayBodies.STATUS_HISTORY),
            new Case(HISTORY, 400, "GenericAPIResponse", GatewayBodies.wrappedError("E4012", "Invalid requestTimestamp")));

    @Test
    void theCommittedDescriptionIsTheRecordedOne() throws Exception {
        try (InputStream in = OpenApiConformanceTest.class.getResourceAsStream(RESOURCE)) {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(in.readAllBytes());
            assertThat(HexFormat.of().formatHex(digest)).isEqualTo(SHA256);
        }
    }

    @Test
    void everyPathAndMethodTheClientCallsIsAnOperationOfTheDescription() {
        assertThat(API.serverPath()).isEqualTo("/einvoice_api");
        CALLED.forEach(endpoint -> {
            assertThat(endpoint.path()).startsWith(API.serverPath() + "/");
            assertThat(API.operation(endpoint.method(), endpoint.path().substring(API.serverPath().length())))
                    .as("%s %s", endpoint.method(), endpoint.path())
                    .isPresent();
        });
    }

    @Test
    void theReceiverTheClientSendsToIsNotInTheDescription() {
        assertThat(UjpEndpoints.SALES_INVOICE_SEND).startsWith("/JSONReceiver/");
        assertThat(API.hasPath(UjpEndpoints.SALES_INVOICE_SEND.substring("/JSONReceiver".length()))).isFalse();
    }

    @Test
    void everyRequestSendsTheHeadersTheDescriptionRequiresAndTheBodyItDeclares() throws Exception {
        stubAll();
        UjpClient client = client();

        client.status(GatewayBodies.EUID);
        client.statusHistory(GatewayBodies.EUID, java.time.LocalDate.of(2026, 1, 1));

        List<LoggedRequest> requests = wireMock.getAllServeEvents().stream().map(event -> event.getRequest()).toList();
        assertThat(requests).hasSize(4);
        for (LoggedRequest request : requests) {
            JsonObject operation = API.operation(request.getMethod().getName(),
                    request.getUrl().substring(API.serverPath().length())).orElseThrow();
            List<String> required = API.requiredHeaders(operation);
            required.forEach(header -> assertThat(request.containsHeader(header))
                    .as("%s %s sends %s", request.getMethod(), request.getUrl(), header).isTrue());
            if (request.getMethod().getName().equals("POST")) {
                assertThat(required).containsExactlyInAnyOrder("X-EDB", "X-EUJP-ID", "X-SERIAL-NUMBER");
                assertThat(API.requestSchemaName(operation)).contains("InvoiceSignedRequest");
                assertThat(request.getHeader("Content-Type")).isEqualTo(API.requestMediaType(operation).orElseThrow());
                assertThat(API.validate("InvoiceSignedRequest", request.getBodyAsString())).isEmpty();
                assertThat(((JsonObject) JsonParser.parse(request.getBodyAsString())).getString("jws")).isNotBlank();
            }
        }
    }

    @Test
    void everyBodyTheTestsFeedTheClientConformsToTheSchemaTheDescriptionDeclaresForItsStatus() {
        for (Case c : BODIES) {
            JsonObject operation = API.operation(c.endpoint().method(),
                    c.endpoint().path().substring(API.serverPath().length())).orElseThrow();
            assertThat(API.responseSchemaName(operation, c.httpStatus()))
                    .as("%s declares %d as %s", c.endpoint(), c.httpStatus(), c.schema())
                    .contains(c.schema());
            assertThat(API.validate(c.schema(), c.body()))
                    .as("%s %d", c.endpoint(), c.httpStatus())
                    .isEmpty();
        }
    }

    @Test
    void everyMemberOfInvoiceStatusIsRead() {
        Map<String, Function<SubmissionStatus, Object>> read = new LinkedHashMap<>();
        read.put("euid", SubmissionStatus::euid);
        read.put("statusCode", SubmissionStatus::code);
        read.put("statusName", SubmissionStatus::name);
        read.put("docNumber", SubmissionStatus::docNumber);
        read.put("docDate", SubmissionStatus::docDate);
        read.put("docDeliveryDate", SubmissionStatus::docDeliveryDate);
        read.put("docNetAmount", SubmissionStatus::netAmount);
        read.put("docVatAmount", SubmissionStatus::vatAmount);
        read.put("docGrossAmountR", SubmissionStatus::grossAmountRounded);
        read.put("docAvansAmount", SubmissionStatus::advanceAmount);
        read.put("docFinalAmount", SubmissionStatus::finalAmount);
        read.put("sellerTin", SubmissionStatus::sellerTin);
        read.put("buyerTin", SubmissionStatus::buyerTin);
        read.put("senderTin", SubmissionStatus::senderTin);
        read.put("receiverTin", SubmissionStatus::receiverTin);
        read.put("docType", SubmissionStatus::docType);
        read.put("docTypeName", SubmissionStatus::docTypeName);
        read.put("comment", SubmissionStatus::comment);
        read.put("cancelComment", SubmissionStatus::cancelComment);
        read.put("voidComment", s -> s.voidReason().comment());
        read.put("voidReason", s -> s.voidReason().text());
        read.put("voidReasonCode", s -> s.voidReason().code());
        read.put("rejectReason", s -> s.rejectReason().text());
        read.put("rejectReasonCode", s -> s.rejectReason().code());
        read.put("correctionReasonComment", s -> s.correctionReason().comment());
        read.put("correctionReason", s -> s.correctionReason().text());
        read.put("correctionReasonCode", s -> s.correctionReason().code());
        JsonObject invoiceStatus = sample("InvoiceStatus", "");
        assertThat(read.keySet()).containsExactlyInAnyOrderElementsOf(invoiceStatus.members().keySet());
        wireMock.stubFor(get(urlPathEqualTo(UjpEndpoints.SERVER_TIME)).willReturn(aResponse()
                .withBody(GatewayBodies.serverTime("2026-01-05T12:00:00"))));
        stubPost(UjpEndpoints.SALES_INVOICE_CURRENT_STATUS,
                JsonWriter.write(JsonObject.builder().put("docStatus", invoiceStatus).build()));

        SubmissionStatus status = client().status(GatewayBodies.EUID);

        read.forEach((member, getter) -> assertThat(String.valueOf(getter.apply(status)))
                .as(member).isEqualTo(text(invoiceStatus.members().get(member))));
    }

    @Test
    void everyMemberOfDocumentStatusChangeIsRead() {
        Map<String, Function<StatusChange, Object>> read = new LinkedHashMap<>();
        read.put("euid", StatusChange::euid);
        read.put("oldStatus", c -> c.oldStatusCode() + "|" + c.oldStatusName());
        read.put("newStatus", c -> c.newStatusCode() + "|" + c.newStatusName());
        read.put("timestamp", StatusChange::timestamp);
        read.put("comment", StatusChange::comment);
        read.put("cancelComment", StatusChange::cancelComment);
        read.put("voidComment", c -> c.voidReason().comment());
        read.put("voidReason", c -> c.voidReason().text());
        read.put("voidReasonCode", c -> c.voidReason().code());
        read.put("rejectReason", c -> c.rejectReason().text());
        read.put("rejectReasonCode", c -> c.rejectReason().code());
        JsonObject change = sample("DocumentStatusChange", "");
        assertThat(read.keySet()).containsExactlyInAnyOrderElementsOf(change.members().keySet());
        wireMock.stubFor(get(urlPathEqualTo(UjpEndpoints.SERVER_TIME)).willReturn(aResponse()
                .withBody(GatewayBodies.serverTime("2026-01-05T12:00:00"))));
        stubPost(UjpEndpoints.SALES_INVOICE_STATUS_HISTORY, JsonWriter.write(JsonObject.builder()
                .put("statusHistory", net.aetherealtech.ujpeinvoice.internal.json.JsonArray.of(List.of(change)))
                .build()));

        StatusChange read1 = client().statusHistory(GatewayBodies.EUID, null).getFirst();

        Map<String, String> expected = new HashMap<>();
        change.members().forEach((member, value) -> expected.put(member, value instanceof JsonObject status
                ? text(status.members().get("code")) + "|" + text(status.members().get("name"))
                : text(value)));
        read.forEach((member, getter) -> assertThat(String.valueOf(getter.apply(read1)))
                .as(member).isEqualTo(expected.get(member)));
    }

    @Test
    void everyCodeTheLibraryListsForTheEinvoiceApiIsInTheDescriptionsEnum() {
        JsonValue enumeration = API.schema("ErrorStatusBean").getObject("properties").getObject("errorCode")
                .get("enum").orElseThrow();
        List<String> described = ((net.aetherealtech.ujpeinvoice.internal.json.JsonArray) enumeration).items()
                .stream().map(item -> ((JsonString) item).value()).toList();

        assertThat(UjpErrorCodes.all(ErrorNamespace.EINVOICE_API))
                .extracting(UjpErrorCode::code)
                .isNotEmpty()
                .allSatisfy(code -> assertThat(described).contains(code));
    }

    /**
     * A body for the component schema with every declared member set: its example when it has one, a marker
     * otherwise, and for a nested status object a marker of its own so old and new cannot be confused.
     */
    private static JsonObject sample(String schemaName, String salt) {
        JsonObject.Builder body = JsonObject.builder();
        API.schema(schemaName).getObject("properties").members().forEach((member, definition) -> {
            JsonObject property = (JsonObject) definition;
            if (property.get("$ref").isPresent()) {
                body.put(member, sample("DocumentStatus", member.equals("newStatus") ? "new-" : ""));
            } else if (salt.isEmpty() && property.get("example").isPresent()) {
                body.put(member, property.get("example").get());
            } else {
                body.put(member, salt + member);
            }
        });
        return body.build();
    }

    private static String text(JsonValue value) {
        return switch (value) {
            case JsonString s -> s.value();
            case JsonNumber n -> n.value().toPlainString();
            default -> value.toString();
        };
    }

    private static UjpClient client() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            Throttle never = new Throttle(Duration.ofSeconds(1), () -> 0L, wait -> {
            });
            return UjpClient.builder().baseUrl(URI.create(wireMock.baseUrl())).eujpId("eujp-test-1")
                    .edb("4030012345678").certificateSerialNumber("5A:3C:0F").serializer(new UjpJsonSerializer())
                    .signer(new JwsRs256Signer(generator.generateKeyPair().getPrivate())).throttle(never).build();
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void stubAll() {
        wireMock.stubFor(get(urlPathEqualTo(UjpEndpoints.SERVER_TIME)).willReturn(aResponse()
                .withBody(GatewayBodies.serverTime("2026-01-05T12:00:00"))));
        stubPost(UjpEndpoints.SALES_INVOICE_CURRENT_STATUS, GatewayBodies.CURRENT_STATUS);
        stubPost(UjpEndpoints.SALES_INVOICE_STATUS_HISTORY, GatewayBodies.STATUS_HISTORY);
    }

    private static void stubPost(String path, String body) {
        wireMock.stubFor(post(urlPathEqualTo(path)).willReturn(aResponse().withBody(body)));
    }
}
