# ujp-einvoice

A Java library for the North Macedonian UJP e-Faktura e-invoicing gateway: a serialization-agnostic
document model, RS256 JWS signing, and an HTTP client — built with zero runtime dependencies,
targeting JDK 25+.

> [!WARNING]
> **Work in progress. Do not use this to file.**
>
> The wire format in every published release is wrong. The official „API Спецификација" was obtained on
> 2026-09-13 and compared against 0.2.0 field by field: the payload shape, the request envelope and the
> mandatory request headers are all wrong, and the gateway would reject a submission at its first validation
> step. Six of roughly thirty-five wire names survive. A corrected release is planned; it will be `1.0.0` or a
> parallel package, and it will break every consumer pinned to 0.2.0.
>
> Read `openspec/specs/specification-status/spec.md` before depending on anything here. The comparison that
> found this is beside it, as `comparison-2026-09-13.md`.

**This project is not affiliated with, endorsed by, or reviewed by УЈП (the Public Revenue Office of
the Republic of North Macedonia).** It is an independent, best-effort client built from public
research. Read the section below before using it against anything but a sandbox.

**`openspec/` is the record** — `openspec/specs/` holds the full model surface, the complete provisional field
inventory, and the signing entry points; `openspec/changes/` holds the planned work with what each item waits on.
`CLAUDE.md` is the rules for working here. This README is the short version.

## Specification status

This is the most important section in this README, and its answer changed on 2026-09-13.

**The official specification has been read.** Until then it was believed unreachable outside North Macedonian
networks, and everything here was reconstructed from public desk research and hands-on accounts from Macedonian
integrators. It was never a geography problem: the documentation host serves its certificate without the
intermediate, so a client that will not complete the chain itself fails while one that does gets the page.

The comparison against 0.2.0 is recorded in full at `openspec/specs/specification-status/comparison-2026-09-13.md`,
and `openspec/specs/specification-status/spec.md` carries the inventory requirement by requirement. The short
version:

| Area | Verdict |
|---|---|
| A proprietary JSON schema signed as compact JWS, **not** UBL 2.1 / XAdES | **Confirmed** against the official documents |
| A submission answers with an EUID and a QR verification link | **Confirmed** |
| The sandbox host, and a qualified certificate registered in the УЈП portal | **Confirmed** in substance; the issuing authorities this library names are confirmed by nothing |
| The shape of the signed payload | **Wrong** — it nests under `document` with eight named blocks; 0.2.0 emits a flat invoice and no request timestamp at all |
| The request body | **Wrong** — the gateway takes a JSON envelope carrying the JWS; 0.2.0 posts the raw compact serialization |
| Request headers | **Wrong** — four are mandatory and 0.2.0 sends none of them |
| The send path | **Wrong** — it carries an `/api/v1` segment this library omits |
| Exact JSON field names | **Wrong** — six of roughly thirty-five survive |
| VAT category wire values | **Wrong** — they are `DDV-A` (18%), `DDV-V` (10%), `DDV-B` (5%) and `DDV-G` (0%), not the rate itself, and the letters are not alphabetical by rate. Exemption is a family of tax indicators, not one category |
| Document type, and how a credit or debit note is marked | **Answered** — a mandatory code on every document (`100`, `110`, `120`); cancellation is a separate axis; amounts stay positive |
| The status-polling endpoint | **Answered** — a signed POST, not a GET by id |
| The meanings of the certificate and company-validation error codes | **Answered** from the published catalogue. Codes are namespaced per API, so the same string means different things in different interfaces and this library's flat constant set is itself a modelling error |
| The production base URL | **Still unverified** — every published document names only the sandbox |
| What a natural-person buyer omits | **Still unverified** — every worked example bills a company, and the buyer's tax number and address are marked mandatory |
| Rounding, and writing an explicit null versus omitting a field | **Still unverified** — not published either way |
| MK VAT rates: 18% standard, 10% and 5% reduced, 0% zero-rated | **Verified** — public tax law, independent of the wire format |
| ЕДБ (unique tax number) as the party identifier | **Verified concept, format deliberately not enforced** |

One thing has not been read and should be, before any field name in a corrected release is called verified: the
JSON Schema the gateway actually validates against. It sits behind the Swagger interface on the API host rather
than the documentation host. The worked examples are illustrations of it, not the thing itself, and promoting names
on the strength of examples is how this library came to be wrong the first time.

Every reconstructed field, endpoint and code in the source carries a `@ProvisionalSpec` annotation
(source-retention, for readers) or an explicit "PROVISIONAL" note in its javadoc. Nothing is promoted to verified
without citing the specific section of an obtained document that confirms it, and a sandbox that behaves as
expected is evidence rather than confirmation — the reconstructions were self-consistent and still wrong. See
`CLAUDE.md` for the rule.

The mandate timeline — October 2026 voluntary, April 2027 mandatory — is a planning assumption, not
a confirmed legal deadline. See `openspec/changes/wire-layer-correction/`.

## Architecture

Three independent layers, so a corrected spec touches as little as possible:

```
model            Document and the types of UJP's type pages (Header, Party, Line, Payment, VatGroup,
  |              TaxIndicator, DocumentKind, Mode, ...) in UJP's own concepts. It names no wire field
  |              and knows no JSON. Totals are computed from the lines, never accepted.
  |
serialization    Serializer interface. UjpJsonSerializer is the one shipped implementation, writing the
  |              JSON UJP's type pages and worked examples describe. A UBL serializer would be a new
  |              implementation of Serializer, not a change to `model`.
  |
signing          Signer interface. JwsRs256Signer signs with JDK-only primitives (java.security),
  |              holds no static state, and is built per organization from that organization's own
  |              keystore. Pkcs12KeyStores / Pkcs11KeyStores load the private key.
  |
transport        UjpClient (java.net.http) composes a Serializer and a Signer to submit a Document and
                 poll its status, reading the gateway's clock for every request timestamp and spacing
                 requests one second apart. Every endpoint lives in UjpEndpoints, cited to its source.
```

## Quickstart

> [!CAUTION]
> This shows the shape of the API. **Do not run it against a real UJP host:** the `UjpClient` ban stands. It is never
> called for a real submission until the specification is verified, a qualified certificate is registered and its
> privileges are granted. The calls that would reach the gateway are commented out on purpose.

```java
import net.aetherealtech.ujpeinvoice.model.*;
import net.aetherealtech.ujpeinvoice.serialization.UjpJsonSerializer;
import net.aetherealtech.ujpeinvoice.signing.JwsRs256Signer;
import net.aetherealtech.ujpeinvoice.signing.Pkcs12KeyStores;
import net.aetherealtech.ujpeinvoice.signing.SigningCredential;
import net.aetherealtech.ujpeinvoice.transport.*;

import java.net.URI;
import java.time.LocalDate;

// 1. Build the document. Its kind says which fields it must carry, and a missing one is refused by name
//    before anything is signed. Totals are computed from the lines; a document whose totals disagree with
//    its lines cannot be constructed.
LocalDate today = LocalDate.now();
Document invoice = Document.builder(DocumentKind.INVOICE)
        .header(Header.of("INV-2026-0001", "1", today).withTurnoverDate(today))
        .seller(Party.company("Seller DOOEL", "4030012345678",
                new Address("Bul. Partizanski Odredi", "1", "1000", "Skopje")))
        .buyer(Party.company("Buyer DOO", "4057098765432",
                new Address("Ul. Makedonija", "10", "7000", "Bitola")))
        .payment(Payment.of(PaymentType.P_12))
        .line(Line.builder()
                .description("Consulting services")
                .unit("hour")
                .quantity("2")
                .unitPrice("100")
                .standardRate(VatGroup.DDV_A)
                .build())
        .build();

// 2. Load the signing key. One keystore per organization, straight from bytes: nothing touches disk, and the
//    single private key entry is found without being told its alias. For a qualified certificate on its
//    issuing USB token, see Pkcs11KeyStores.
SigningCredential credential = Pkcs12KeyStores.load(p12Bytes, password);

// 3. Build the client. Nothing defaults: the host, the signer's e-УЈП id, the company's ЕДБ and the
//    certificate's serial number are all yours to state. The signer below writes the header exactly as UJP's
//    FAQ shows it, {"alg":"RS256","typ":"JWT"}; the gateway identifies the certificate by X-SERIAL-NUMBER, so
//    the key alone is passed. new JwsRs256Signer(key, chain) would add an x5c claim that no obtained document
//    mentions, and whether the gateway accepts it is unknown.
UjpClient client = UjpClient.builder()
        .baseUrl(gatewayHost)                    // a URI you chose deliberately; there is no default
        .eujpId(eujpId)
        .edb("4030012345678")
        .certificateSerialNumber(serialNumber)
        .serializer(new UjpJsonSerializer())
        .signer(new JwsRs256Signer(credential.privateKey()))
        .build();

// 4. These would call the gateway. Each first reads its server time, so each takes at least a second:
//
// SubmissionResult receipt = client.submit(invoice);            // euid(), qrLink(), timestamp(), ...
// SubmissionStatus status = client.status(receipt.euid());      // code(), name() as the gateway serves them
// status.is(SubmissionStatus.ACCEPTED);                         // a code outside the ten known ones passes through
// client.statusHistory(receipt.euid(), LocalDate.of(2026, 1, 1)); // dateFrom's format is provisional
//
// A refusal is a UjpException: httpStatus(), namespace() (EINVOICE_API or JSON_RECEIVER), errorCode(),
// known() against the published catalogue, and responseBody() exactly as received. A request that never
// completed, or an answer that could not be read, is a UjpTransportException.
```

A `JwsRs256Signer` is an instance holding no JVM-global state, so a process serving many
organizations builds one per organization and each signs with its own key. A `UjpClient` is throttled per instance
to the gateway's limit of one request per second per user, so keep one instance per user.

## Certificate registration prerequisite

Submitting invoices requires a **qualified certificate** from any North Macedonian provider of qualified trust
services, in software or on a USB token (UJP's FAQ, `/ЧПП`, section 1), pre-registered against your taxpayer account at
`eujptest.ujp.gov.mk/ureg` (sandbox) before the corresponding production registration. A certificate
that signs correctly but was never registered there is the likely cause of error `E1012`, which the official
catalogue gives as "Certificate not found" (`UjpErrorCodes.lookup(ErrorNamespace.JSON_RECEIVER, "E1012")`).

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

Maven packages on GitHub inherit the permissions of the repository that published them, so a public
repository means a public package. A consuming workflow's own built-in `GITHUB_TOKEN` resolves it, with
no secret to configure, no `repo` scope and no membership of the Aethereal-Tech organization. A stored
personal access token carrying `read:packages` works too, and is what a consumer needs when it resolves
packages from a repository that is still private.

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
