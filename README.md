# ujp-einvoice

A Java library for the North Macedonian UJP e-Faktura e-invoicing gateway: a serialization-agnostic
invoice domain model, RS256 JWS signing, and an HTTP client — built with zero runtime dependencies,
targeting JDK 25+.

**This project is not affiliated with, endorsed by, or reviewed by УЈП (the Public Revenue Office of
the Republic of North Macedonia).** It is an independent, best-effort client built from public
research. Read the section below before using it against anything but a sandbox.

## Specification status

This is the most important section in this README. The authoritative source for the UJP e-Faktura
wire format — efakturawiki.ujp.gov.mk's "API Спецификација" — is **unreachable outside North
Macedonian networks**, and nobody who built this library has read it. Everything here was
reconstructed from public desk research and hands-on accounts from Macedonian integrators (forum
posts, blog write-ups, and support-channel screenshots), as of 2026-08-28. Treat all of it as a
starting point to validate against a real sandbox account, not as a confirmed contract.

| Area | Status | Basis |
|---|---|---|
| Wire format is a proprietary UJP JSON schema, signed as compact JWS (RS256) | **Reconstructed, moderate confidence** | Consistent across multiple independent integrator accounts |
| Wire format is **not** UBL 2.1 / XAdES | **Reconstructed, moderate confidence** | Contradicts generic e-invoicing compliance sites, which appear to describe the EU norm generically rather than UJP's actual endpoint |
| Submission returns an EUID and a QR verification link | **Reconstructed, moderate confidence** | Reported by integrators |
| Endpoint family `/JSONReceiver/sales-invoices/...` on `efakturatest.ujp.gov.mk` | **Reconstructed, moderate confidence** | Reported by integrators |
| Exact JSON field names (`invoiceNumber`, `seller`, `lineItems`, ...) | **Reconstructed, low confidence** | This library's own best-effort naming; see every field marked `@ProvisionalSpec` in `UjpJsonSerializer` |
| Status-polling endpoint path | **Reconstructed, very low confidence** | Guessed by analogy with the submit endpoint; no direct evidence found |
| Production base URL | **Reconstructed, very low confidence** | Inferred by removing "test" from the sandbox hostname |
| Error codes E1012 (certificate not pre-registered), E5004, E10001–E10003 exist | **Reconstructed, moderate confidence** | Codes observed in integrator reports |
| Meaning of E5004 and E10001–E10003 specifically | **Unknown** | Codes were seen; no confirmed explanation of what triggers them was found |
| Cert-based auth requires a KIBS- or Telekom-issued qualified certificate, pre-registered at `eujptest.ujp.gov.mk/ureg` | **Reconstructed, moderate confidence** | Reported by integrators |
| MK VAT rates: 18% standard, 10% and 5% reduced, 0% zero-rated, plus an exempt category | **Verified** | Public tax law (Law on Value Added Tax), independent of the UJP wire format |
| ЕДБ (unique tax number) as the party identifier | **Verified concept, format not enforced** | Public, well-known identifier; this library requires it be present but does not enforce a 13-digit format, to avoid rejecting legitimate edge cases this library cannot fully confirm |

Every reconstructed field, endpoint, and code in the source carries a `@ProvisionalSpec` annotation
(source-retention, for readers) or an explicit "PROVISIONAL" note in its javadoc. See `CLAUDE.md` for
the rule governing when something may be promoted from provisional to verified.

**If you have read the official spec and can confirm or correct any of the above, please open an
issue or PR citing the specific section.** That is the single most valuable contribution this project
can receive right now.

### Mandate timeline

Also unverified beyond public announcements, and worth stating plainly because it affects how urgent
any of this is:

- **October 2026** — voluntary go-live is the publicly stated target.
- **April 2027** — e-invoicing is expected to become mandatory for VAT payers, but the enabling law
  may still be in draft as of this writing. Treat "April 2027" as the planning assumption, not a
  confirmed legal deadline.

## Architecture

Three independent layers, so a corrected spec touches as little as possible:

```
model            Invoice, Party, LineItem, VatCategory, Totals — no knowledge that JSON exists.
  |
serialization    Serializer interface. UjpJsonSerializer is the one shipped implementation of the
  |              reconstructed wire shape above. A UBL serializer, or a corrected JSON shape, is a
  |              new implementation of Serializer — not a change to `model`.
  |
signing          Signer interface. JwsRs256Signer signs with JDK-only primitives (java.security).
  |              Pkcs12KeyStores / Pkcs11KeyStores load the private key.
  |
transport        UjpClient (java.net.http) composes a Serializer and a Signer to submit and poll.
                 Every endpoint path lives in UjpEndpoints, whose javadoc restates the caveat above.
```

## Quickstart

```java
import net.aetherealtech.ujpeinvoice.model.*;
import net.aetherealtech.ujpeinvoice.serialization.UjpJsonSerializer;
import net.aetherealtech.ujpeinvoice.signing.JwsRs256Signer;
import net.aetherealtech.ujpeinvoice.signing.Pkcs12KeyStores;
import net.aetherealtech.ujpeinvoice.transport.*;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.time.LocalDate;

// 1. Build the invoice. Totals are computed from the line items — there is no way to hand-construct
//    an Invoice whose totals disagree with what its lines add up to.
Invoice invoice = Invoice.builder()
        .invoiceNumber("INV-2026-0001")
        .issueDate(LocalDate.now())
        .currency("MKD")
        .seller(new Party("Seller DOOEL", "4030012345678",
                new Address("Bul. Partizanski Odredi 1", "Skopje", "1000", "MK")))
        .buyer(new Party("Buyer DOO", "4057098765432",
                new Address("Ul. Makedonija 10", "Bitola", "7000", "MK")))
        .addLineItem("Consulting services", new BigDecimal("2"), new BigDecimal("100.00"),
                VatCategory.STANDARD_18)
        .build();

// 2. Load the signing key. From a PKCS#12 export...
PrivateKey key = Pkcs12KeyStores.loadPrivateKey(Path.of("signing-cert.p12"), "password".toCharArray(), "alias");
// ...or from a USB qualified-certificate token via PKCS#11 — see Pkcs11KeyStores' javadoc.

// 3. Submit against the sandbox (UjpClient defaults to UjpEndpoints.TEST_BASE_URL).
UjpClient client = UjpClient.builder()
        .serializer(new UjpJsonSerializer())
        .signer(new JwsRs256Signer(key))
        .build();

try {
    SubmissionResult result = client.submit(invoice);
    System.out.println("EUID: " + result.euid() + ", QR: " + result.qrLink());
} catch (UjpException e) {
    System.err.println("Rejected: " + e.errorCode() + " — " + e.getMessage());
}
```

## Certificate registration prerequisite

Per integrator accounts (see "Specification status" above), submitting invoices requires a
KIBS- or Telekom-issued **qualified certificate**, pre-registered against your taxpayer account at
`eujptest.ujp.gov.mk/ureg` (sandbox) before the corresponding production registration. A certificate
that signs correctly but was never registered there is the likely cause of error `E1012`
(`UjpException.E1012_CERTIFICATE_NOT_REGISTERED`).

## Consuming this library (GitHub Packages)

This package is published to GitHub Packages, which — unlike Maven Central — **requires
authentication to download even public packages**. You need a GitHub personal access token with at
least `read:packages` scope, even just to depend on this library.

Add the repository:

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/Aethereal-Tech/ujp-einvoice</url>
    </repository>
</repositories>

<dependency>
    <groupId>net.aetherealtech</groupId>
    <artifactId>ujp-einvoice</artifactId>
    <version>0.1.0</version>
</dependency>
```

And configure the credentials Maven needs to reach it, in `~/.m2/settings.xml` (never committed to a
project's own `pom.xml`):

```xml
<settings>
    <servers>
        <server>
            <id>github</id>
            <username>YOUR_GITHUB_USERNAME</username>
            <password>YOUR_GITHUB_TOKEN</password>
        </server>
    </servers>
</settings>
```

## Building from source

```
./mvnw clean verify
```

Requires JDK 25+. Runs the full test suite (JUnit 5, WireMock for the transport layer) and the
JaCoCo coverage gate (90% line coverage on `model`, `serialization`, and `signing`).

## License

MIT — see [LICENSE](LICENSE).
