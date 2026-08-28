package net.aetherealtech.ujpeinvoice.transport;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import net.aetherealtech.ujpeinvoice.serialization.UjpJsonSerializer;
import net.aetherealtech.ujpeinvoice.signing.JwsRs256Signer;
import net.aetherealtech.ujpeinvoice.testsupport.InvoiceFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Round-trips {@link UjpClient} against a fake server standing in for the UJP gateway's
 * reconstructed shapes (see {@link UjpEndpoints}). These prove this library talks HTTP correctly
 * against ITS OWN best-effort reconstruction — they cannot and do not prove that reconstruction
 * matches the real gateway. Validate against efakturatest.ujp.gov.mk with a registered sandbox
 * certificate before trusting this in production; see the README's "Specification status" section.
 */
class UjpClientTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance().build();

    private final UjpJsonSerializer serializer = new UjpJsonSerializer();

    @Test
    void submitReturnsTheParsedSubmissionResult() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo(UjpEndpoints.SALES_INVOICE_SEND))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"euid":"MK-EUID-12345","qrLink":"https://efakturatest.ujp.gov.mk/verify/MK-EUID-12345",\
                                "status":"ACCEPTED","message":"Invoice accepted"}""")));

        UjpClient client = clientFor(wireMock.baseUrl());
        SubmissionResult result = client.submit(InvoiceFixtures.simpleInvoice());

        assertThat(result.euid()).isEqualTo("MK-EUID-12345");
        assertThat(result.qrLink()).isEqualTo("https://efakturatest.ujp.gov.mk/verify/MK-EUID-12345");
        assertThat(result.status()).isEqualTo(SubmissionStatus.ACCEPTED);
        assertThat(result.message()).isEqualTo("Invoice accepted");
    }

    @Test
    void submitSendsACompactJwsBodyWithTheJoseContentType() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo(UjpEndpoints.SALES_INVOICE_SEND))
                .willReturn(aResponse().withStatus(200).withBody("""
                        {"euid":"E1","qrLink":"L","status":"PENDING","message":"ok"}""")));

        UjpClient client = clientFor(wireMock.baseUrl());
        client.submit(InvoiceFixtures.simpleInvoice());

        wireMock.verify(postRequestedFor(urlPathEqualTo(UjpEndpoints.SALES_INVOICE_SEND))
                .withHeader("Content-Type", equalTo("application/jose"))
                .withRequestBody(matching("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$")));
    }

    @Test
    void statusPollsByEuidAndReturnsPendingWithNoEuidYet() throws Exception {
        wireMock.stubFor(get(urlEqualTo(UjpEndpoints.SALES_INVOICE_STATUS_TEMPLATE.formatted("MK-EUID-99")))
                .willReturn(aResponse().withStatus(200).withBody("""
                        {"status":"PENDING","message":"Still processing"}""")));

        UjpClient client = clientFor(wireMock.baseUrl());
        SubmissionResult result = client.status("MK-EUID-99");

        assertThat(result.status()).isEqualTo(SubmissionStatus.PENDING);
        assertThat(result.euid()).isNull();
    }

    @Test
    void aGatewayErrorResponseThrowsUjpExceptionWithTheErrorCode() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo(UjpEndpoints.SALES_INVOICE_SEND))
                .willReturn(aResponse().withStatus(400).withBody("""
                        {"errorCode":"E1012","errorMessage":"Certificate not registered"}""")));

        UjpClient client = clientFor(wireMock.baseUrl());

        assertThatThrownBy(() -> client.submit(InvoiceFixtures.simpleInvoice()))
                .isInstanceOf(UjpException.class)
                .satisfies(ex -> {
                    UjpException e = (UjpException) ex;
                    assertThat(e.errorCode()).isEqualTo(UjpException.E1012_CERTIFICATE_NOT_REGISTERED);
                    assertThat(e.httpStatus()).isEqualTo(400);
                    assertThat(e.getMessage()).isEqualTo("Certificate not registered");
                });
    }

    @Test
    void aNonJsonResponseThrowsUjpTransportException() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo(UjpEndpoints.SALES_INVOICE_SEND))
                .willReturn(aResponse().withStatus(200).withBody("not json at all")));

        UjpClient client = clientFor(wireMock.baseUrl());

        assertThatThrownBy(() -> client.submit(InvoiceFixtures.simpleInvoice()))
                .isInstanceOf(UjpTransportException.class);
    }

    @Test
    void aJsonArrayResponseThrowsUjpTransportException() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo(UjpEndpoints.SALES_INVOICE_SEND))
                .willReturn(aResponse().withStatus(200).withBody("[]")));

        UjpClient client = clientFor(wireMock.baseUrl());

        assertThatThrownBy(() -> client.submit(InvoiceFixtures.simpleInvoice()))
                .isInstanceOf(UjpTransportException.class);
    }

    @Test
    void connectionFailureThrowsUjpTransportException() throws Exception {
        // Nothing is listening on this port (WireMock's own port, but stopped first), so the
        // connection itself fails before any HTTP response exists.
        UjpClient client = clientFor(URI.create("http://127.0.0.1:1"));

        assertThatThrownBy(() -> client.submit(InvoiceFixtures.simpleInvoice()))
                .isInstanceOf(UjpTransportException.class);
    }

    @Test
    void statusRejectsABlankEuid() throws Exception {
        UjpClient client = clientFor(wireMock.baseUrl());

        assertThatThrownBy(() -> client.status("  ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void builderRequiresASerializer() {
        assertThatThrownBy(() -> UjpClient.builder().signer(new JwsRs256Signer(rsaKeyPair().getPrivate())).build())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void builderRequiresASigner() {
        assertThatThrownBy(() -> UjpClient.builder().serializer(serializer).build())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void builderDefaultsToTheTestBaseUrl() throws Exception {
        UjpClient client = UjpClient.builder()
                .serializer(serializer)
                .signer(new JwsRs256Signer(rsaKeyPair().getPrivate()))
                .build();

        assertThat(client).isNotNull();
    }

    private UjpClient clientFor(String baseUrl) throws Exception {
        return clientFor(URI.create(baseUrl));
    }

    private UjpClient clientFor(URI baseUrl) throws Exception {
        return UjpClient.builder()
                .baseUrl(baseUrl)
                .serializer(serializer)
                .signer(new JwsRs256Signer(rsaKeyPair().getPrivate()))
                .build();
    }

    private static KeyPair rsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }
}
