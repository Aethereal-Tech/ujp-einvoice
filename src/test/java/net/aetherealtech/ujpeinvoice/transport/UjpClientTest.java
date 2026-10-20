package net.aetherealtech.ujpeinvoice.transport;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import net.aetherealtech.ujpeinvoice.internal.json.JsonObject;
import net.aetherealtech.ujpeinvoice.internal.json.JsonParser;
import net.aetherealtech.ujpeinvoice.model.Document;
import net.aetherealtech.ujpeinvoice.serialization.UjpJsonSerializer;
import net.aetherealtech.ujpeinvoice.signing.JwsRs256Signer;
import net.aetherealtech.ujpeinvoice.testsupport.DocumentFixtures;
import net.aetherealtech.ujpeinvoice.testsupport.GatewayBodies;
import net.aetherealtech.ujpeinvoice.testsupport.UjpExamples;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Round-trips {@link UjpClient} against WireMock standing in for the gateway, on localhost. These prove the
 * client sends what D1 and the API description say and reads what they show; they cannot prove the gateway
 * accepts it. Nothing here contacts a UJP host, and no test sleeps: the throttle runs on a fake clock.
 */
class UjpClientTest {

    static final String EUJP_ID = "eujp-test-1";
    static final String EDB = "4030012345678";
    static final String SERIAL = "5A:3C:0F";
    static final String SERVER_TIME = "2026-01-05T12:00:00";

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance().build();

    private static final KeyPair KEYS = rsaKeyPair();

    private final UjpJsonSerializer serializer = new UjpJsonSerializer();
    private final FakeClock clock = new FakeClock();

    @BeforeEach
    void serverTimeAnswers() {
        stubServerTime(SERVER_TIME);
    }

    // ---- send ----

    @Test
    void submitPostsTheEnvelopeWithTheHeadersToTheReceiver() {
        stubSend(200, GatewayBodies.SUBMITTED);

        client().submit(DocumentFixtures.simpleInvoice());

        LoggedRequest request = onlyRequest(RequestMethod.POST, UjpEndpoints.SALES_INVOICE_SEND);
        assertThat(request.getUrl()).isEqualTo("/JSONReceiver/api/v1/sales-invoices/send");
        assertThat(request.getHeader("X-EUJP-ID")).isEqualTo(EUJP_ID);
        assertThat(request.getHeader("X-EDB")).isEqualTo(EDB);
        assertThat(request.getHeader("X-SERIAL-NUMBER")).isEqualTo(SERIAL);
        assertThat(request.getHeader("X-DOC-TYPE-CODE")).isEqualTo("100");
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/json");
        JsonObject envelope = (JsonObject) JsonParser.parse(request.getBodyAsString());
        assertThat(envelope.members().keySet()).containsExactly("requestTimestamp", "jws");
        assertThat(envelope.getString("requestTimestamp")).isEqualTo(SERVER_TIME);
    }

    @Test
    void theJwsIsTheSerializedDocumentSignedWithTheServerTimeInsideItToo() throws Exception {
        stubSend(200, GatewayBodies.SUBMITTED);
        Document document = DocumentFixtures.simpleInvoice();

        client().submit(document);

        String jws = ((JsonObject) JsonParser.parse(onlyRequest(RequestMethod.POST, UjpEndpoints.SALES_INVOICE_SEND)
                .getBodyAsString())).getString("jws");
        String[] parts = jws.split("\\.");
        assertThat(decode(parts[0])).isEqualTo("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
        byte[] expected = serializer.serialize(document, LocalDateTime.parse(SERVER_TIME));
        assertThat(Base64.getUrlDecoder().decode(parts[1])).isEqualTo(expected);
        assertThat(((JsonObject) JsonParser.parse(decode(parts[1]))).getString("requestTimestamp"))
                .isEqualTo(SERVER_TIME);
        assertThat(verifies(parts)).isTrue();
    }

    @Test
    void theTimestampIsTheServersNotTheLocalClocksAndKeepsWholeSeconds() {
        stubServerTime("2031-03-04T05:06:07.891");
        stubSend(200, GatewayBodies.SUBMITTED);

        client().submit(DocumentFixtures.simpleInvoice());

        JsonObject envelope = (JsonObject) JsonParser.parse(
                onlyRequest(RequestMethod.POST, UjpEndpoints.SALES_INVOICE_SEND).getBodyAsString());
        assertThat(envelope.getString("requestTimestamp")).isEqualTo("2031-03-04T05:06:07");
    }

    @Test
    void theServerTimeIsReadWithTheIdentifyingHeadersButNoSerialNumber() {
        stubSend(200, GatewayBodies.SUBMITTED);

        client().submit(DocumentFixtures.simpleInvoice());

        LoggedRequest request = onlyRequest(RequestMethod.GET, UjpEndpoints.SERVER_TIME);
        assertThat(request.getUrl()).isEqualTo("/einvoice_api/api/v1/server-time");
        assertThat(request.getHeader("X-EUJP-ID")).isEqualTo(EUJP_ID);
        assertThat(request.getHeader("X-EDB")).isEqualTo(EDB);
        assertThat(request.containsHeader("X-SERIAL-NUMBER")).isFalse();
    }

    @Test
    void theServerTimeIsReadAgainOnEveryCall() {
        stubSend(200, GatewayBodies.SUBMITTED);
        UjpClient client = client();

        client.submit(DocumentFixtures.simpleInvoice());
        client.submit(DocumentFixtures.simpleInvoice());

        wireMock.verify(2, getRequestedFor(urlPathEqualTo(UjpEndpoints.SERVER_TIME)));
    }

    @Test
    void theDocTypeHeaderIsTheKindsCode() {
        stubSend(200, GatewayBodies.SUBMITTED);
        UjpClient client = client();

        client.submit(UjpExamples.creditNote());
        client.submit(UjpExamples.cashTurnover());

        assertThat(requests(RequestMethod.POST, UjpEndpoints.SALES_INVOICE_SEND))
                .extracting(request -> request.getHeader("X-DOC-TYPE-CODE"))
                .containsExactly("110", "310");
    }

    @Test
    void submitReadsTheReceiptAsD1ShowsIt() {
        stubSend(200, GatewayBodies.SUBMITTED);

        SubmissionResult result = client().submit(DocumentFixtures.simpleInvoice());

        assertThat(result.euid()).isEqualTo("019b8d43-7840-7433-b358-08891b53605c");
        assertThat(result.message()).isEqualTo("Фактура успешно зачувана");
        assertThat(result.qrLink()).isEqualTo("https://gateway.test/euid/019b8d43-7840-7433-b358-08891b53605c");
        assertThat(result.status()).isEqualTo(200);
        assertThat(result.eInvoiceUser()).isFalse();
        assertThat(result.timestamp()).isEqualTo(Instant.parse("2026-01-05T08:26:07.846Z"));
        assertThat(result.responseBody()).isEqualTo(GatewayBodies.SUBMITTED);
    }

    @Test
    void anAcceptedReceiptWithMissingOrUnreadableMembersStillReturns() {
        stubSend(200, """
                {"euid":"E1","status":"fine","e_invoice_user":"no","timestamp":"yesterday"}""");

        SubmissionResult result = client().submit(DocumentFixtures.simpleInvoice());

        assertThat(result.euid()).isEqualTo("E1");
        assertThat(result.message()).isNull();
        assertThat(result.qrLink()).isNull();
        assertThat(result.status()).isNull();
        assertThat(result.eInvoiceUser()).isNull();
        assertThat(result.timestamp()).isNull();
        assertThat(result.responseBody()).contains("yesterday");
    }

    @Test
    void aFractionalStatusReadsAsAbsent() {
        stubSend(200, "{\"euid\":\"E1\",\"status\":200.5}");

        assertThat(client().submit(DocumentFixtures.simpleInvoice()).status()).isNull();
    }

    // ---- the throttle ----

    @Test
    void aSubmitSpacesTheSendOneSecondAfterTheServerTimeRead() {
        stubSend(200, GatewayBodies.SUBMITTED);

        client().submit(DocumentFixtures.simpleInvoice());

        assertThat(clock.sleeps).containsExactly(Duration.ofSeconds(1));
    }

    @Test
    void everyRequestOfEveryCallIsSpaced() {
        stubSend(200, GatewayBodies.SUBMITTED);
        stubStatus(200, GatewayBodies.CURRENT_STATUS);
        UjpClient client = client();

        client.submit(DocumentFixtures.simpleInvoice());
        client.status(GatewayBodies.EUID);

        assertThat(clock.sleeps).containsExactly(Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(1));
    }

    @Test
    void noWaitIsOwedOnceASecondHasPassed() {
        stubSend(200, GatewayBodies.SUBMITTED);
        UjpClient client = client();

        client.submit(DocumentFixtures.simpleInvoice());
        clock.nanos += Duration.ofSeconds(5).toNanos();
        client.submit(DocumentFixtures.simpleInvoice());

        assertThat(clock.sleeps).containsExactly(Duration.ofSeconds(1), Duration.ofSeconds(1));
    }

    @Test
    void anInterruptedWaitIsATransportFailureAndKeepsTheInterrupt() {
        stubSend(200, GatewayBodies.SUBMITTED);
        UjpClient client = UjpClient.builder().baseUrl(URI.create(wireMock.baseUrl())).eujpId(EUJP_ID).edb(EDB)
                .certificateSerialNumber(SERIAL).serializer(serializer).signer(new JwsRs256Signer(KEYS.getPrivate()))
                .throttle(new Throttle(Duration.ofSeconds(1), () -> 0L, duration -> {
                    throw new InterruptedException();
                })).build();

        try {
            assertThatThrownBy(() -> client.submit(DocumentFixtures.simpleInvoice()))
                    .isInstanceOf(UjpTransportException.class);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    // ---- status ----

    @Test
    void statusPostsTheSignedEuidInAnEnvelopeOfTheJwsAlone() {
        stubStatus(200, GatewayBodies.CURRENT_STATUS);

        client().status(GatewayBodies.EUID);

        LoggedRequest request = onlyRequest(RequestMethod.POST, UjpEndpoints.SALES_INVOICE_CURRENT_STATUS);
        assertThat(request.getUrl()).isEqualTo("/einvoice_api/api/v1/documents/sales-invoice/current-status");
        assertThat(request.getHeader("X-EUJP-ID")).isEqualTo(EUJP_ID);
        assertThat(request.getHeader("X-EDB")).isEqualTo(EDB);
        assertThat(request.getHeader("X-SERIAL-NUMBER")).isEqualTo(SERIAL);
        assertThat(request.containsHeader("X-DOC-TYPE-CODE")).isFalse();
        JsonObject envelope = (JsonObject) JsonParser.parse(request.getBodyAsString());
        assertThat(envelope.members().keySet()).containsExactly("jws");
        String[] parts = envelope.getString("jws").split("\\.");
        assertThat(decode(parts[0])).isEqualTo("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
        assertThat(decode(parts[1]))
                .isEqualTo("{\"requestTimestamp\":\"2026-01-05T12:00:00\",\"euid\":\"" + GatewayBodies.EUID + "\"}");
        assertThat(verifies(parts)).isTrue();
    }

    @Test
    void statusReadsEveryMemberOfTheCurrentStatus() {
        stubStatus(200, GatewayBodies.CURRENT_STATUS);

        SubmissionStatus status = client().status(GatewayBodies.EUID);

        assertThat(status.code()).isEqualTo("03");
        assertThat(status.name()).isEqualTo("Прифатена");
        assertThat(status.isKnown()).isTrue();
        assertThat(status.is(SubmissionStatus.ACCEPTED)).isTrue();
        assertThat(status.is(SubmissionStatus.REJECTED)).isFalse();
        assertThat(status.euid()).isEqualTo(GatewayBodies.EUID);
        assertThat(status.docNumber()).isEqualTo("e6e70a12-fdbe-4542-8");
        assertThat(status.docType()).isEqualTo("100");
        assertThat(status.docTypeName()).isEqualTo("Фактура");
        assertThat(status.docDate()).isEqualTo(LocalDate.of(2026, 5, 20));
        assertThat(status.docDeliveryDate()).isEqualTo(LocalDate.of(2026, 5, 20));
        assertThat(status.netAmount()).isEqualByComparingTo("1500.5");
        assertThat(status.vatAmount()).isEqualByComparingTo("270");
        assertThat(status.grossAmountRounded()).isEqualByComparingTo("1771");
        assertThat(status.advanceAmount()).isEqualByComparingTo("100");
        assertThat(status.finalAmount()).isEqualByComparingTo("1671");
        assertThat(status.sellerTin()).isEqualTo("4030995135699");
        assertThat(status.buyerTin()).isEqualTo("4030995135700");
        assertThat(status.senderTin()).isEmpty();
        assertThat(status.receiverTin()).isEmpty();
        assertThat(status.comment()).isEqualTo("прв коментар");
        assertThat(status.cancelComment()).isEqualTo("Грешка во износот");
        assertThat(status.voidReason()).isEqualTo(new StatusReason("S-1", "Рекламација", "Грешка во износот"));
        assertThat(status.rejectReason()).isEqualTo(new StatusReason("O-2", "Invalid date", null));
        assertThat(status.correctionReason()).isEqualTo(new StatusReason("C01", "Грешка во цена", "Погрешна цена"));
        assertThat(status.responseBody()).isEqualTo(GatewayBodies.CURRENT_STATUS);
    }

    @Test
    void aStatusCodeTheStatusesPageDoesNotListPassesThrough() {
        stubStatus(200, GatewayBodies.UNKNOWN_STATUS);

        SubmissionStatus status = client().status(GatewayBodies.EUID);

        assertThat(status.code()).isEqualTo("99");
        assertThat(status.name()).isEqualTo("Нов статус");
        assertThat(status.isKnown()).isFalse();
        assertThat(status.voidReason()).isNull();
        assertThat(status.netAmount()).isNull();
    }

    @Test
    void theTenKnownCodesAreNamed() {
        assertThat(List.of(SubmissionStatus.DRAFT, SubmissionStatus.SENT, SubmissionStatus.ACCEPTED,
                SubmissionStatus.AUTO_ACCEPTED, SubmissionStatus.REJECTED, SubmissionStatus.VOIDED,
                SubmissionStatus.CORRECTED, SubmissionStatus.RECORDED, SubmissionStatus.STORNO,
                SubmissionStatus.CORRECTIVE))
                .containsExactly("00", "01", "03", "04", "05", "07", "09", "10", "12", "13");
        assertThat(new SubmissionStatus(null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null).is(null)).isFalse();
    }

    @Test
    void aStatusAnswerWithoutADocStatusIsUnreadableAndKeepsItsBody() {
        stubStatus(200, "{\"success\":true}");

        assertThatThrownBy(() -> client().status(GatewayBodies.EUID))
                .isInstanceOfSatisfying(UjpTransportException.class, e -> {
                    assertThat(e.httpStatus()).isEqualTo(200);
                    assertThat(e.namespace()).isEqualTo(ErrorNamespace.EINVOICE_API);
                    assertThat(e.responseBody()).isEqualTo("{\"success\":true}");
                });
    }

    @Test
    void aDateThatDoesNotParseReadsAsAbsentRatherThanFailingThePoll() {
        stubStatus(200, """
                {"docStatus":{"statusCode":"03","statusName":"Прифатена","docDate":"soon","docNetAmount":"12"}}""");
        stubHistory(200, "{\"statusHistory\":[{\"euid\":\"E\",\"timestamp\":\"later\"}]}");
        UjpClient client = client();

        SubmissionStatus status = client.status(GatewayBodies.EUID);
        List<StatusChange> history = client.statusHistory(GatewayBodies.EUID, null);

        assertThat(status.code()).isEqualTo("03");
        assertThat(status.docDate()).isNull();
        assertThat(status.netAmount()).isNull();
        assertThat(history.getFirst().timestamp()).isNull();
    }

    @Test
    void statusRejectsABlankEuid() {
        UjpClient client = client();

        assertThatThrownBy(() -> client.status("  ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client.status(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client.statusHistory("", null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(wireMock.getAllServeEvents()).isEmpty();
    }

    // ---- history ----

    @Test
    void statusHistoryPostsTheSignedEuidAndDateFrom() {
        stubHistory(200, GatewayBodies.STATUS_HISTORY);

        client().statusHistory(GatewayBodies.EUID, LocalDate.of(2026, 1, 1));

        LoggedRequest request = onlyRequest(RequestMethod.POST, UjpEndpoints.SALES_INVOICE_STATUS_HISTORY);
        assertThat(request.getUrl()).isEqualTo("/einvoice_api/api/v1/documents/sales-invoice/status");
        assertThat(request.getHeader("X-SERIAL-NUMBER")).isEqualTo(SERIAL);
        String[] parts = ((JsonObject) JsonParser.parse(request.getBodyAsString())).getString("jws").split("\\.");
        assertThat(decode(parts[1])).isEqualTo("{\"requestTimestamp\":\"2026-01-05T12:00:00\",\"euid\":\""
                + GatewayBodies.EUID + "\",\"dateFrom\":\"2026-01-01\"}");
    }

    @Test
    void statusHistoryWithoutADateFromOmitsIt() {
        stubHistory(200, GatewayBodies.STATUS_HISTORY);

        client().statusHistory(GatewayBodies.EUID, null);

        String jws = ((JsonObject) JsonParser.parse(
                onlyRequest(RequestMethod.POST, UjpEndpoints.SALES_INVOICE_STATUS_HISTORY).getBodyAsString()))
                .getString("jws");
        assertThat(decode(jws.split("\\.")[1])).doesNotContain("dateFrom");
    }

    @Test
    void statusHistoryReadsTheChanges() {
        stubHistory(200, GatewayBodies.STATUS_HISTORY);

        List<StatusChange> history = client().statusHistory(GatewayBodies.EUID, null);

        assertThat(history).containsExactly(new StatusChange(GatewayBodies.EUID, "01", "Испратена (Нова)", "07",
                "Сторнирана", LocalDateTime.parse("2024-07-20T16:25:39"), "коментар", "Грешка во износот",
                new StatusReason("S-1", "Рекламација", "Грешка во износот"),
                new StatusReason("O-2", "Invalid date", null)));
    }

    @Test
    void statusHistoryToleratesAnEmptyOrSparseAnswer() {
        stubHistory(200, "{\"success\":true,\"statusHistory\":[{\"euid\":\"E\"},\"junk\"]}");
        assertThat(client().statusHistory(GatewayBodies.EUID, null)).containsExactly(
                new StatusChange("E", null, null, null, null, null, null, null, null, null));

        stubHistory(200, "{\"success\":true}");
        assertThat(client().statusHistory(GatewayBodies.EUID, null)).isEmpty();
    }

    // ---- failures ----

    @Test
    void anEinvoiceApiRefusalCarriesItsNamespaceCodeAndRawBody() {
        String body = GatewayBodies.wrappedError("E4012", "Invalid requestTimestamp");
        stubStatus(400, body);

        assertThatThrownBy(() -> client().status(GatewayBodies.EUID))
                .isInstanceOfSatisfying(UjpException.class, e -> {
                    assertThat(e).isNotInstanceOf(UjpTransportException.class);
                    assertThat(e.httpStatus()).isEqualTo(400);
                    assertThat(e.namespace()).isEqualTo(ErrorNamespace.EINVOICE_API);
                    assertThat(e.errorCode()).isEqualTo("E4012");
                    assertThat(e.getMessage()).isEqualTo("Invalid requestTimestamp");
                    assertThat(e.responseBody()).isEqualTo(body);
                    assertThat(e.known()).isPresent();
                });
    }

    @Test
    void aBareErrorStatusBeanIsReadToo() {
        stubStatus(500, GatewayBodies.bareError("E5000", "Internal server error"));

        assertThatThrownBy(() -> client().status(GatewayBodies.EUID))
                .isInstanceOfSatisfying(UjpException.class, e -> {
                    assertThat(e.httpStatus()).isEqualTo(500);
                    assertThat(e.errorCode()).isEqualTo("E5000");
                    assertThat(e.namespace()).isEqualTo(ErrorNamespace.EINVOICE_API);
                });
    }

    @Test
    void anUnlistedCodeSurvivesAndResolvesToNothing() {
        stubStatus(400, GatewayBodies.wrappedError("E9999", "Brand new refusal"));

        assertThatThrownBy(() -> client().status(GatewayBodies.EUID))
                .isInstanceOfSatisfying(UjpException.class, e -> {
                    assertThat(e.errorCode()).isEqualTo("E9999");
                    assertThat(e.namespace()).isEqualTo(ErrorNamespace.EINVOICE_API);
                    assertThat(e.known()).isEmpty();
                });
    }

    @Test
    void theRateLimitRefusalIsReadFromTheServerTimeRead() {
        wireMock.stubFor(get(urlPathEqualTo(UjpEndpoints.SERVER_TIME)).willReturn(aResponse().withStatus(429)
                .withHeader("Retry-After", "1")
                .withBody(GatewayBodies.wrappedError("E4007", "Rate limit exceeded"))));
        stubSend(200, GatewayBodies.SUBMITTED);

        assertThatThrownBy(() -> client().submit(DocumentFixtures.simpleInvoice()))
                .isInstanceOfSatisfying(UjpException.class, e -> {
                    assertThat(e.httpStatus()).isEqualTo(429);
                    assertThat(e.errorCode()).isEqualTo("E4007");
                    assertThat(e.known().orElseThrow().meaning()).isEqualTo("Rate limit exceeded");
                });
        wireMock.verify(0, postRequestedFor(urlPathEqualTo(UjpEndpoints.SALES_INVOICE_SEND)));
    }

    @Test
    void aReceiverRefusalIsReadInTheReceiversNamespace() {
        String body = GatewayBodies.wrappedError("E1020", "Invalid calculated totals");
        stubSend(400, body);

        assertThatThrownBy(() -> client().submit(DocumentFixtures.simpleInvoice()))
                .isInstanceOfSatisfying(UjpException.class, e -> {
                    assertThat(e.httpStatus()).isEqualTo(400);
                    assertThat(e.namespace()).isEqualTo(ErrorNamespace.JSON_RECEIVER);
                    assertThat(e.errorCode()).isEqualTo("E1020");
                    assertThat(e.known().orElseThrow().meaning()).isEqualTo("Invalid calculated totals");
                    assertThat(e.responseBody()).isEqualTo(body);
                });
    }

    @Test
    void aReceiverBodyOfAnotherShapeIsKeptRaw() {
        stubSend(502, "<html>Bad gateway</html>");

        assertThatThrownBy(() -> client().submit(DocumentFixtures.simpleInvoice()))
                .isInstanceOfSatisfying(UjpException.class, e -> {
                    assertThat(e).isNotInstanceOf(UjpTransportException.class);
                    assertThat(e.httpStatus()).isEqualTo(502);
                    assertThat(e.namespace()).isEqualTo(ErrorNamespace.JSON_RECEIVER);
                    assertThat(e.errorCode()).isNull();
                    assertThat(e.known()).isEmpty();
                    assertThat(e.getMessage()).isEqualTo("UJP gateway returned HTTP 502");
                    assertThat(e.responseBody()).isEqualTo("<html>Bad gateway</html>");
                });
    }

    @Test
    void anErrorStatusInsideASuccessfulAnswerIsARefusal() {
        stubStatus(200, GatewayBodies.wrappedError("E4008", "The requested resource could not be found"));

        assertThatThrownBy(() -> client().status(GatewayBodies.EUID))
                .isInstanceOfSatisfying(UjpException.class, e -> {
                    assertThat(e).isNotInstanceOf(UjpTransportException.class);
                    assertThat(e.httpStatus()).isEqualTo(200);
                    assertThat(e.errorCode()).isEqualTo("E4008");
                });
    }

    @Test
    void aNonJsonSuccessIsUnreadableAndMayHaveBeenAccepted() {
        stubSend(200, "not json at all");

        assertThatThrownBy(() -> client().submit(DocumentFixtures.simpleInvoice()))
                .isInstanceOfSatisfying(UjpTransportException.class, e -> {
                    assertThat(e.httpStatus()).isEqualTo(200);
                    assertThat(e.namespace()).isEqualTo(ErrorNamespace.JSON_RECEIVER);
                    assertThat(e.responseBody()).isEqualTo("not json at all");
                });
    }

    @Test
    void aJsonArraySuccessIsUnreadable() {
        stubSend(200, "[]");

        assertThatThrownBy(() -> client().submit(DocumentFixtures.simpleInvoice()))
                .isInstanceOf(UjpTransportException.class);
    }

    @Test
    void aServerTimeThatIsNotATimestampStopsTheCallBeforeAnythingIsSent() {
        stubServerTime("yesterday");
        stubSend(200, GatewayBodies.SUBMITTED);

        assertThatThrownBy(() -> client().submit(DocumentFixtures.simpleInvoice()))
                .isInstanceOf(UjpTransportException.class);
        wireMock.verify(0, postRequestedFor(urlPathEqualTo(UjpEndpoints.SALES_INVOICE_SEND)));
    }

    @Test
    void aServerTimeAnswerWithoutATimestampStopsTheCall() {
        wireMock.stubFor(get(urlPathEqualTo(UjpEndpoints.SERVER_TIME)).willReturn(aResponse().withBody("{}")));

        assertThatThrownBy(() -> client().status(GatewayBodies.EUID)).isInstanceOf(UjpTransportException.class);
    }

    @Test
    void aConnectionFailureIsATransportFailure() {
        UjpClient client = clientFor(URI.create("http://127.0.0.1:1"));

        assertThatThrownBy(() -> client.submit(DocumentFixtures.simpleInvoice()))
                .isInstanceOfSatisfying(UjpTransportException.class, e -> assertThat(e.httpStatus()).isEqualTo(-1));
    }

    // ---- the builder ----

    @Test
    void theBuilderHasNoDefaultHostOrIdentity() {
        assertThatThrownBy(() -> UjpClient.builder().eujpId(EUJP_ID).edb(EDB).certificateSerialNumber(SERIAL)
                .serializer(serializer).signer(signer()).build())
                .isInstanceOf(IllegalStateException.class).hasMessage("baseUrl is required");
        assertThatThrownBy(() -> base().eujpId(null).build()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> UjpClient.builder().baseUrl(URI.create(wireMock.baseUrl())).edb(EDB)
                .certificateSerialNumber(SERIAL).serializer(serializer).signer(signer()).build())
                .hasMessage("eujpId is required");
        assertThatThrownBy(() -> UjpClient.builder().baseUrl(URI.create(wireMock.baseUrl())).eujpId(EUJP_ID)
                .certificateSerialNumber(SERIAL).serializer(serializer).signer(signer()).build())
                .hasMessage("edb is required");
        assertThatThrownBy(() -> UjpClient.builder().baseUrl(URI.create(wireMock.baseUrl())).eujpId(EUJP_ID).edb(EDB)
                .serializer(serializer).signer(signer()).build())
                .hasMessage("certificateSerialNumber is required");
    }

    @Test
    void theBuilderRequiresASerializerAndASigner() {
        assertThatThrownBy(() -> UjpClient.builder().baseUrl(URI.create(wireMock.baseUrl())).eujpId(EUJP_ID).edb(EDB)
                .certificateSerialNumber(SERIAL).signer(signer()).build()).hasMessage("serializer is required");
        assertThatThrownBy(() -> UjpClient.builder().baseUrl(URI.create(wireMock.baseUrl())).eujpId(EUJP_ID).edb(EDB)
                .certificateSerialNumber(SERIAL).serializer(serializer).build()).hasMessage("signer is required");
    }

    @Test
    void theBuilderRefusesBlankIdentifiersAndNulls() {
        assertThatThrownBy(() -> UjpClient.builder().edb(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> UjpClient.builder().certificateSerialNumber("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> UjpClient.builder().baseUrl(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> UjpClient.builder().httpClient(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> UjpClient.builder().throttle(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void aCallerSuppliedHttpClientIsUsed() {
        stubSend(200, GatewayBodies.SUBMITTED);
        java.net.http.HttpClient http = java.net.http.HttpClient.newHttpClient();

        UjpClient client = base().httpClient(http).throttle(clock.throttle()).build();

        assertThat(client.submit(DocumentFixtures.simpleInvoice()).euid()).isNotNull();
    }

    // ---- support ----

    private UjpClient client() {
        return clientFor(URI.create(wireMock.baseUrl()));
    }

    private UjpClient clientFor(URI baseUrl) {
        return base().baseUrl(baseUrl).throttle(clock.throttle()).build();
    }

    private UjpClient.Builder base() {
        return UjpClient.builder().baseUrl(URI.create(wireMock.baseUrl())).eujpId(EUJP_ID).edb(EDB)
                .certificateSerialNumber(SERIAL).serializer(serializer).signer(signer());
    }

    private static JwsRs256Signer signer() {
        return new JwsRs256Signer(KEYS.getPrivate());
    }

    private static void stubServerTime(String timestamp) {
        wireMock.stubFor(get(urlPathEqualTo(UjpEndpoints.SERVER_TIME)).willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json").withBody(GatewayBodies.serverTime(timestamp))));
    }

    private static void stubSend(int status, String body) {
        stubPost(UjpEndpoints.SALES_INVOICE_SEND, status, body);
    }

    private static void stubStatus(int status, String body) {
        stubPost(UjpEndpoints.SALES_INVOICE_CURRENT_STATUS, status, body);
    }

    private static void stubHistory(int status, String body) {
        stubPost(UjpEndpoints.SALES_INVOICE_STATUS_HISTORY, status, body);
    }

    private static void stubPost(String path, int status, String body) {
        wireMock.stubFor(post(urlPathEqualTo(path)).willReturn(aResponse().withStatus(status)
                .withHeader("Content-Type", "application/json").withBody(body)));
    }

    private static List<LoggedRequest> requests(RequestMethod method, String path) {
        return wireMock.getAllServeEvents().stream().map(event -> event.getRequest())
                .filter(request -> request.getMethod().equals(method) && request.getUrl().equals(path))
                .sorted((a, b) -> a.getLoggedDate().compareTo(b.getLoggedDate()))
                .toList();
    }

    private static LoggedRequest onlyRequest(RequestMethod method, String path) {
        List<LoggedRequest> requests = requests(method, path);
        assertThat(requests).hasSize(1);
        return requests.getFirst();
    }

    private static String decode(String segment) {
        return new String(Base64.getUrlDecoder().decode(segment), StandardCharsets.UTF_8);
    }

    private static boolean verifies(String[] parts) throws RuntimeException {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(KEYS.getPublic());
            signature.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
            return signature.verify(Base64.getUrlDecoder().decode(parts[2]));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static KeyPair rsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A throttle's clock and sleeper that never sleep: the wait is recorded and the clock moves by it. */
    private static final class FakeClock {
        long nanos;
        final List<Duration> sleeps = new ArrayList<>();

        Throttle throttle() {
            return new Throttle(Duration.ofSeconds(1), () -> nanos, wait -> {
                sleeps.add(wait);
                nanos += wait.toNanos();
            });
        }
    }
}
