package net.aetherealtech.ujpeinvoice.transport;

import net.aetherealtech.ujpeinvoice.ProvisionalSpec;

import java.net.URI;

/**
 * Every UJP e-Faktura HTTP endpoint this library calls, gathered in one place so a corrected value
 * only ever needs to change here.
 *
 * <p><strong>Every constant in this class is reconstructed, not verified.</strong> The paths are
 * assembled from hands-on Macedonian integrator accounts describing the {@code /JSONReceiver/...}
 * family of endpoints exercised against {@code efakturatest.ujp.gov.mk}; the official source —
 * efakturawiki.ujp.gov.mk's "API Спецификација" — is unreachable outside North Macedonian networks
 * and has not been read by anyone who built this library. See
 * {@code openspec/specs/specification-status/spec.md} and {@code openspec/specs/transport/spec.md}
 * for the full verified-vs-reconstructed inventory and {@code CLAUDE.md} for the rule that nothing
 * here may be marked verified without citing the specific section of the official spec that
 * confirms it.
 */
public final class UjpEndpoints {

    private UjpEndpoints() {
    }

    /**
     * Sandbox environment base URL, for pre-production integration testing and for the certificate
     * pre-registration step described at {@code eujptest.ujp.gov.mk/ureg}. PROVISIONAL: the host
     * itself is as reported by integrators; not independently confirmed against the official spec.
     */
    @ProvisionalSpec("Host reported by integrators; scheme and exact hostname not independently confirmed.")
    public static final URI TEST_BASE_URL = URI.create("https://efakturatest.ujp.gov.mk");

    /**
     * Production environment base URL. PROVISIONAL, and more speculative than {@link #TEST_BASE_URL}:
     * inferred by removing "test" from the sandbox hostname, a pattern common to UJP's other systems,
     * but not itself confirmed by any integrator account this library's authors found.
     */
    @ProvisionalSpec("Inferred from the sandbox hostname by pattern; not independently observed or confirmed.")
    public static final URI PRODUCTION_BASE_URL = URI.create("https://efaktura.ujp.gov.mk");

    /**
     * Submit a sales invoice for processing. PROVISIONAL: path as reported by integrators, expects a
     * compact JWS (RS256) request body per those same accounts.
     */
    @ProvisionalSpec("Path as reported by integrators for JWS-signed sales invoice submission.")
    public static final String SALES_INVOICE_SEND = "/JSONReceiver/sales-invoices/send";

    /**
     * Poll submission status by EUID; {@code "%s"} is replaced with the URL-encoded EUID. PROVISIONAL
     * and weaker than {@link #SALES_INVOICE_SEND}: this exact path pattern was not itself reported —
     * it follows the send endpoint's naming convention, which is a guess about a guess.
     */
    @ProvisionalSpec("Path pattern guessed by analogy with SALES_INVOICE_SEND; no direct integrator evidence found.")
    public static final String SALES_INVOICE_STATUS_TEMPLATE = "/JSONReceiver/sales-invoices/status/%s";
}
