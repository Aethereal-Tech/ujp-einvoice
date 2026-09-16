# ujp-einvoice

A Java library for the North Macedonian UJP e-Faktura e-invoicing gateway: a serialization-agnostic
invoice domain model, RS256 JWS signing, and an HTTP client — built with zero runtime dependencies,
targeting JDK 25+.

**This project is not affiliated with, endorsed by, or reviewed by УЈП (the Public Revenue Office of
the Republic of North Macedonia).** It is an independent, best-effort client built from public
research. Read the section below before using it against anything but a sandbox.

**`openspec/` is the record** — `openspec/specs/` holds the full model surface, the complete provisional field
inventory, and the signing entry points; `openspec/changes/` holds the planned work with what each item waits on.
`CLAUDE.md` is the rules for working here. This README is the short version.

## Specification status

This is the most important section in this README. The authoritative source for the UJP e-Faktura
wire format — efakturawiki.ujp.gov.mk's „API Спецификација" — is **unreachable outside North
Macedonian networks, and nobody who built this library has read it.** Everything was reconstructed
from public desk research and hands-on accounts from Macedonian integrators (forum posts, blog
write-ups, and support-channel screenshots), as of 2026-08-28. Treat all of it as a starting point
to validate against a real sandbox account, not as a confirmed contract.

The summary below is deliberately short; **`openspec/specs/specification-status/spec.md` carries the complete
inventory**, field by field and endpoint by endpoint.

| Area | Status |
|---|---|
| Wire format is a proprietary UJP JSON schema, signed as compact JWS (RS256), **not** UBL 2.1 / XAdES | Reconstructed, moderate confidence |
| Submission returns an EUID and a QR verification link | Reconstructed, moderate confidence |
| Endpoint family `/JSONReceiver/sales-invoices/...` on `efakturatest.ujp.gov.mk` | Reconstructed, moderate confidence |
| Status-polling path, and the production base URL | Reconstructed, very low confidence — one guessed by analogy, the other inferred from the sandbox hostname |
| Exact JSON field names (`invoiceNumber`, `seller`, `lineItems`, ...) | Reconstructed, low confidence — this library's own naming |
| Document type field and its note codes (`documentType`, `CREDIT_NOTE`, `DEBIT_NOTE`) | Reconstructed, low confidence — this library's own naming. Nothing describes how UJP marks a note; an ordinary invoice writes no type field at all |
| Corrected-invoice reference (`correctedInvoice`, carrying `number` and `issueDate`) | Reconstructed, low confidence — this library's own naming |
| Line-item unit of measure (`unit`, free text) | Reconstructed, low confidence — this library's own naming. Nothing says UJP expects a unit at all, let alone a code from a list |
| Omitting an absent field entirely — a natural-person buyer's missing tax id, street, postal code or city — rather than writing `""` or `null` | Reconstructed, low confidence — this library's own choice; the gateway's tolerance for either is unverified |
| Error codes E1012, E5004, E10001–E10003 exist | Reconstructed, moderate confidence |
| Meaning of E5004 and E10001–E10003 specifically | **Unknown** |
| Cert-based auth needs a KIBS- or Telekom-issued qualified certificate, pre-registered at `eujptest.ujp.gov.mk/ureg` | Reconstructed, moderate confidence |
| MK VAT rates: 18% standard, 10% and 5% reduced, 0% zero-rated, plus an exempt category | **Verified** — public tax law, independent of the wire format |
| ЕДБ (unique tax number) as the party identifier | **Verified concept, format deliberately not enforced** |

Every reconstructed field, endpoint, and code in the source carries a `@ProvisionalSpec` annotation
(source-retention, for readers) or an explicit "PROVISIONAL" note in its javadoc. See `CLAUDE.md` for
the rule governing when something may be promoted from provisional to verified.

**If you are on the team and get to read the official spec, correct the inventory in
`openspec/specs/specification-status/spec.md`, citing the specific section.** That is worth more than anything
else in the backlog.

The mandate timeline — October 2026 voluntary, April 2027 mandatory — is a planning assumption, not
a confirmed legal deadline. See `openspec/changes/wire-layer-correction/`.

## Architecture

Three independent layers, so a corrected spec touches as little as possible:

```
model            Invoice, Party, Address, LineItem, VatCategory, Totals, DocumentType,
  |              DocumentReference — no knowledge that JSON exists. An Invoice is a plain invoice
  |              by default, or a credit/debit note naming the invoice it corrects. A Party is a
  |              company (name + ЕДБ + complete address) or a natural person (a name, and nothing
  |              else that is required).
  |
serialization    Serializer interface. UjpJsonSerializer is the one shipped implementation of the
  |              reconstructed wire shape above. A UBL serializer, or a corrected JSON shape, is a
  |              new implementation of Serializer — not a change to `model`.
  |
signing          Signer interface. JwsRs256Signer signs with JDK-only primitives (java.security),
  |              holds no static state, and is built per organization from that organization's own
  |              keystore. Pkcs12KeyStores / Pkcs11KeyStores load the private key.
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
import net.aetherealtech.ujpeinvoice.signing.SigningCredential;
import net.aetherealtech.ujpeinvoice.transport.*;

import java.math.BigDecimal;
import java.time.LocalDate;

// 1. Build the invoice. Totals are computed from the line items — there is no way to hand-construct
//    an Invoice whose totals disagree with what its lines add up to.
Invoice invoice = Invoice.builder()
        .invoiceNumber("INV-2026-0001")
        .issueDate(LocalDate.now())
        .currency("MKD")
        .seller(Party.company("Seller DOOEL", "4030012345678",
                new Address("Bul. Partizanski Odredi 1", "Skopje", "1000", "MK")))
        .buyer(Party.company("Buyer DOO", "4057098765432",
                new Address("Ul. Makedonija 10", "Bitola", "7000", "MK")))
        .addLineItem("Consulting services", new BigDecimal("2"), new BigDecimal("100.00"),
                VatCategory.STANDARD_18)
        .build();

// A private individual buys with a name and nothing else — no ЕДБ, and no address required. Any
// part of an address that IS on record can be given; an absent country reads as "MK".
Party consumer = Party.naturalPerson("Ана Ангеловска");
Party consumerWithCityOnly = Party.naturalPerson("Ана Ангеловска",
        new Address(null, "Битола", null, null));

// A credit note („книжно одобрение") must name the invoice it corrects. Its amounts stay positive:
// the document type carries the direction. A line may state its unit — free text, omitted if absent.
Invoice creditNote = Invoice.builder()
        .invoiceNumber("CN-2026-0007")
        .issueDate(LocalDate.now())
        .currency("MKD")
        .seller(invoice.seller())
        .buyer(invoice.buyer())
        .documentType(DocumentType.CREDIT_NOTE)
        .correctedInvoice(new DocumentReference("INV-2026-0001", LocalDate.of(2026, 8, 28)))
        .addLineItem("Consulting services (partial credit)", BigDecimal.ONE, new BigDecimal("100.00"),
                VatCategory.STANDARD_18, "час")
        .build();

// 2. Load the signing key. One keystore per organization, straight from bytes — nothing touches
//    disk, and the single private key entry is found without being told its alias.
SigningCredential credential = Pkcs12KeyStores.load(p12Bytes, password);
// ...an InputStream works the same way (read fully, and left open for its owner to close), as does
// a Path. For a qualified certificate on its issuing USB token, see Pkcs11KeyStores.

// 3. Submit against the sandbox (UjpClient defaults to UjpEndpoints.TEST_BASE_URL).
UjpClient client = UjpClient.builder()
        .serializer(new UjpJsonSerializer())
        .signer(new JwsRs256Signer(credential.privateKey(), credential.certificateChain()))
        .build();

try {
    SubmissionResult result = client.submit(invoice);
    System.out.println("EUID: " + result.euid() + ", QR: " + result.qrLink());
} catch (UjpException e) {
    System.err.println("Rejected: " + e.errorCode() + " — " + e.getMessage());
}
```

A `JwsRs256Signer` is an instance holding no JVM-global state, so a process serving many
organizations builds one per organization and each signs with its own key.

## Certificate registration prerequisite

Per integrator accounts (see "Specification status" above), submitting invoices requires a
KIBS- or Telekom-issued **qualified certificate**, pre-registered against your taxpayer account at
`eujptest.ujp.gov.mk/ureg` (sandbox) before the corresponding production registration. A certificate
that signs correctly but was never registered there is the likely cause of error `E1012`
(`UjpException.E1012_CERTIFICATE_NOT_REGISTERED`).

## Consuming this library (GitHub Packages)

**A GitHub token is required on every request**, even though this package is public — there is no
anonymous read, of this or of any GitHub Packages artifact. That is the registry's rule rather than
a choice made here. Being public is what keeps the bar low: any authenticated token carrying
`read:packages` resolves it, and inside a GitHub Actions workflow the run's own `GITHUB_TOKEN` does.
Neither `repo` scope nor membership of the Aethereal-Tech organization is needed.

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

### From a consuming repository's Actions

A workflow's built-in `GITHUB_TOKEN` reaches only its own repository's packages, and nothing widens
that: Maven packages on GitHub always inherit the permissions of the repository that published them,
with no per-package Actions access grant to extend read access to a consuming repository. For a
consuming repository, the only credential that works is a token of the kind described above, stored
as a secret and passed to Maven instead (Aethereal-Tech repositories use the organization secret
`PACKAGES_READ_TOKEN`, wired into `actions/setup-java` as `server-password: PACKAGES_READ_TOKEN`).

Every Maven step that resolves this dependency needs those credentials, in CI as much as locally: a
job that runs `mvn` without them fails at dependency resolution, not at some later step.

## Building from source

```
./mvnw clean verify
```

Requires JDK 25+. Runs the full test suite (JUnit 5, WireMock for the transport layer) and the
JaCoCo coverage gate (90% line coverage on `model`, `serialization`, and `signing`).

## License

MIT — see [LICENSE](LICENSE).
