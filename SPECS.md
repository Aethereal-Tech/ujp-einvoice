# SPECS.md

The record of what this library is and what it does not yet do. `CLAUDE.md` holds the rules for
working in it; this file holds the facts.

**Legend for `# FUTURE`:** **CUT** — decided against; do not re-propose without new information.
**PARKED** — may return, and the entry says what it is waiting on.

---

# PRESENT

## What this is

`net.aetherealtech:ujp-einvoice` — a Java library for the North Macedonian UJP e-Faktura
e-invoicing gateway: a serialization-agnostic invoice domain model, RS256 JWS signing, and an HTTP
client. **Zero runtime dependencies**, `--release 25`, published to GitHub Packages from a private
repository.

Not affiliated with, endorsed by, or reviewed by УЈП. Most of the wire format is **reconstructed
from third-party integrator accounts, not read from the official specification** — see
"Specification inventory" below and the one hard rule in `CLAUDE.md`.

## The three layers

Kept genuinely independent, so a corrected spec touches as little as possible.

| Layer | Contents | Depends on |
|---|---|---|
| `model` | `Invoice`, `Party`, `Address`, `LineItem`, `VatCategory`, `Totals`, `CategoryTotal`, `DocumentType`, `DocumentReference`, `InvoiceValidationException` | nothing — **does not know JSON exists** |
| `serialization` | `Serializer` interface; `UjpJsonSerializer`, the one shipped implementation of the reconstructed wire shape | `model` |
| `signing` | `Signer` interface; `JwsRs256Signer`, `SigningCredential`, `Pkcs12KeyStores`, `Pkcs11KeyStores`, `KeyStoreEntries` (package-private), `SigningException` | JDK `java.security` only |
| `transport` | `UjpClient`, `UjpEndpoints`, `SubmissionResult`, `SubmissionStatus`, `UjpException`, `UjpTransportException` | `Serializer` and `Signer` **as interfaces** |
| `internal` | `Money` (the single rounding convention), `internal.json` (the minimal JSON reader/writer) | nothing |

A change that has to touch `model` to fix the wire format means the abstraction leaked.

Money is `BigDecimal`, `RoundingMode.HALF_UP`, scale 2, through `internal.Money.round` and nowhere
else. A line rounds its net first, then computes VAT from the rounded net; `Totals.compute`
reconciles the per-category and invoice-wide sums against the lines, and `Invoice`'s canonical
constructor refuses totals that do not reconcile.

## Model surface

### `Invoice`

`Invoice(String invoiceNumber, LocalDate issueDate, LocalDate dueDate, Currency currency, Party
seller, Party buyer, List<LineItem> lineItems, Totals totals, DocumentType documentType,
DocumentReference correctedInvoice)`, plus an 8-argument constructor that omits the last two and
means `INVOICE` / no reference. `Invoice.builder()` accumulates and computes `Totals` on `build()`.

| Field | Required | Rule |
|---|---|---|
| `invoiceNumber` | yes | non-blank |
| `issueDate` | yes | non-null |
| `dueDate` | no | must not precede `issueDate`; omitted from the wire when absent |
| `currency` | yes | `java.util.Currency`; builder also takes an ISO code string |
| `seller` | yes | **always a complete business** — name, ЕДБ, street, postal code, city, country. A missing part is refused by name (`Invoice.seller.address.postalCode must be present: …`) |
| `buyer` | yes | a business **or** a natural person (below) |
| `lineItems` | yes | at least one; defensively copied |
| `totals` | yes | must reconcile with `lineItems` |
| `documentType` | yes | defaults to `INVOICE` |
| `correctedInvoice` | on a note | required when `documentType.corrects()`, refused otherwise |

### `Party` — two shapes

`record Party(String name, String taxId, Address address)`.

- `Party.company(String name, String taxId, Address address)` — a business. Name, ЕДБ and a
  complete address; the first missing part is refused **by field name**. This is the only shape a
  seller may take.
- `Party.naturalPerson(String name)` — a private individual, known by name alone.
- `Party.naturalPerson(String name, Address address)` — the same, with whatever address is on
  record; any part of it may be absent.
- `Party.isNaturalPerson()` — true when there is no tax id.

**The B2C shape is deliberate and exact:** for a natural-person buyer the tax id is **absent**;
street, postal code and city are **all optional**; an absent country reads as `MK`; a `name` string
is the only requirement. The library does not demand a city, or any address at all, from a private
individual — refusing one would refuse invoices that are perfectly legal, ahead of a specification
nobody has read.

`name` is one string either way: a registered organization name, or a person's name composed
however the consumer composes it.

`taxId` is the ЕДБ. Required on a business, and **not** format-checked — a 13-digit rule would
reject legitimate cross-border and fixture cases this library cannot confirm.

### `Address`

`record Address(String street, String city, String postalCode, String country)`. Every field is
nullable; `country` defaults to `Address.DEFAULT_COUNTRY` (`"MK"`) when absent.

**Absent is not blank.** A `null` is an absent field and is omitted from the wire; a blank string is
refused everywhere (`Address.street must not be blank; omit it instead`), because it is a missing
value pretending to be present and would force the serializer to choose between writing `""` and
silently dropping it.

### `LineItem`

`record LineItem(String description, BigDecimal quantity, BigDecimal unitPrice, VatCategory
vatCategory, String unit)`, plus a 4-argument constructor for a line that states no unit.

- `description` non-blank, `quantity` strictly positive, `unitPrice` non-negative (zero allowed),
  `vatCategory` non-null.
- `unit` is **optional free text** as the seller writes it — „ком.", „м²", „час", `"kg"`. Not a code
  from a list: nothing says UJP expects a unit at all, let alone a UN/ECE Rec 20 code, and inventing
  a list would refuse units a seller legitimately uses. Blank is refused; absent is omitted from the
  wire.
- `netAmount()`, `vatAmount()`, `grossAmount()` are derived, never stored.

### `DocumentType` and `DocumentReference`

`enum DocumentType { INVOICE, CREDIT_NOTE, DEBIT_NOTE }` with `corrects()`, true for the two note
types. `INVOICE` is an ordinary sales invoice; `CREDIT_NOTE` is „книжно одобрение" (lowers what an
earlier invoice charged); `DEBIT_NOTE` is „книжно задолжување" (raises it).

`record DocumentReference(String number, LocalDate issueDate)` — the document a note corrects, named
so a recipient can find it again. Both parts required. This library cannot check that the referenced
document exists; a consumer that keeps its own invoices should check before building the note.

**Amounts on a note stay positive.** A credit note for 1.000 MKD carries a line of 1.000, never
−1.000: the type carries the direction, the amounts carry the magnitude. Signing the amounts would
state the same fact twice and would force `LineItem`'s "quantity must be positive" rule open for
notes, after which an ordinary typo on an ordinary invoice would stop being caught.

### `VatCategory`

North Macedonian VAT categories per the Law on Value Added Tax. The **rates** are public tax law and
verified; only the wire **code** for each is reconstructed.

| Constant | `code()` | Rate |
|---|---|---|
| `STANDARD_18` | `"18"` | 0.18 |
| `REDUCED_10` | `"10"` | 0.10 |
| `REDUCED_5` | `"5"` | 0.05 |
| `ZERO` | `"0"` | 0.00 |
| `EXEMPT` | `"EXEMPT"` | 0.00 |

## Specification inventory

The authoritative source — efakturawiki.ujp.gov.mk's „API Спецификација" — is unreachable outside
North Macedonian networks, and **nobody who built this library has read it**. Everything below was
reconstructed from public desk research and hands-on accounts from Macedonian integrators (forum
posts, blog write-ups, support-channel screenshots), as of 2026-08-28.

### Shape and behaviour

| Area | Status | Basis |
|---|---|---|
| Wire format is a proprietary UJP JSON schema, signed as compact JWS (RS256) | Reconstructed, moderate confidence | Consistent across multiple independent integrator accounts |
| Wire format is **not** UBL 2.1 / XAdES | Reconstructed, moderate confidence | Contradicts generic e-invoicing compliance sites, which appear to describe the EU norm rather than UJP's actual endpoint |
| Submission returns an EUID and a QR verification link | Reconstructed, moderate confidence | Reported by integrators |
| Endpoint family `/JSONReceiver/sales-invoices/...` on `efakturatest.ujp.gov.mk` | Reconstructed, moderate confidence | Reported by integrators |
| Status-polling endpoint path | Reconstructed, very low confidence | Guessed by analogy with the submit endpoint; no direct evidence |
| Production base URL | Reconstructed, very low confidence | Inferred by removing "test" from the sandbox hostname |
| `SubmissionStatus` vocabulary and its wire spellings | Reconstructed, very low confidence | This library's own guess at what a gateway of this kind reports; unrecognized values fall back to `UNKNOWN` rather than throwing |
| Error codes E1012, E5004, E10001–E10003 exist | Reconstructed, moderate confidence | Codes observed in integrator reports |
| Meaning of E5004 and E10001–E10003 specifically | **Unknown** | Seen, with no confirmed explanation of what triggers them |
| E1012 = signing certificate not pre-registered | Reconstructed, moderate confidence | Reported by integrators |
| Cert-based auth needs a KIBS- or Telekom-issued qualified certificate, pre-registered at `eujptest.ujp.gov.mk/ureg` | Reconstructed, moderate confidence | Reported by integrators |
| MK VAT rates: 18% standard, 10% and 5% reduced, 0% zero-rated, plus exempt | **Verified** | Public tax law, independent of the UJP wire format |
| ЕДБ as the party identifier | **Verified concept, format not enforced** | Public identifier; presence required on a business, format deliberately unchecked |

### Field-name inventory (`UjpJsonSerializer`)

Every constant below is `@ProvisionalSpec`. Grouped by how much is behind it.

**This library's own naming, with no evidence at all** — nothing in any integrator account
describes these. First to correct once the spec can be read.

| Constant | Wire name | Note |
|---|---|---|
| `FIELD_DOCUMENT_TYPE` | `documentType` | How UJP distinguishes an invoice from a note is undescribed anywhere |
| `DOCUMENT_TYPE_CODES` | `"CREDIT_NOTE"`, `"DEBIT_NOTE"` | An `INVOICE` writes **no type field at all** |
| `FIELD_CORRECTED_INVOICE` | `correctedInvoice` | Present only on a note |
| `FIELD_REFERENCE_NUMBER` | `number` | Inside the reference object |
| `FIELD_REFERENCE_ISSUE_DATE` | `issueDate` | ISO-8601 assumed |
| `FIELD_LINE_UNIT` | `unit` | Nothing says UJP expects a unit at all |
| omission of absent fields | — | Omitting rather than writing `""` or `null` is this library's own choice; the gateway's tolerance for either is unverified |

**Reconstructed field names** (top level): `invoiceNumber`, `issueDate`, `dueDate`, `currency`,
`seller`, `buyer`, `lineItems`, `totals`.
**Party**: `name`, `taxId`, `address`. **Address**: `street`, `city`, `postalCode`, `country`.
**Line item**: `description`, `quantity`, `unit`, `unitPrice`, `vatCategory`, `netAmount`,
`vatAmount`, `grossAmount`.
**Totals**: `byCategory`, `netTotal`, `vatTotal`, `grossTotal`.
**Category subtotal**: `category`, `net`, `vat`, `gross`.

Field order is fixed and deterministic (`byCategory` follows `VatCategory`'s declaration order via
an `EnumMap`) — golden-file tests depend on it.

**An `INVOICE` serializes byte for byte as it did in 0.1.0.** The document type is expressed by
absence, so introducing notes could not disturb a shape already in the field;
`golden/simple-invoice.json` and `golden/multi-category-invoice.json` are unchanged and pin it.

## Signing

Compact JWS, RS256 (`SHA256withRSA`), built from `java.security` and this library's own JSON writer
— no JOSE library. The certificate chain, when supplied, goes in the header's `x5c` claim
(RFC 7515 §4.1.6); without one the header is a bare `{"alg":"RS256"}`. Whether UJP expects `x5c` on
this endpoint is unconfirmed.

### `JwsRs256Signer` — an instance, never a JVM global

```java
public JwsRs256Signer(PrivateKey privateKey)
public JwsRs256Signer(PrivateKey privateKey, List<X509Certificate> certificateChain)
public String signCompact(byte[] payload)          // from Signer
```

**No static or JVM-global state.** Two signers built from two keystores in one JVM sign with their
own keys, and building one never disturbs another — the shape a multi-organization consumer needs,
where the failure mode is an invoice signed by the wrong taxpayer. `PerOrganizationSignerTest` pins
it three ways: cross-verification (each token verifies under its own certificate and fails under the
other's), byte-identical re-signing after another signer is constructed, and a reflective check that
`JwsRs256Signer` declares no non-final static field.

### `SigningCredential`

`record SigningCredential(PrivateKey privateKey, List<X509Certificate> certificateChain)` — one
identity as it came out of one keystore, read in a single pass under a single password, so a caller
holding many organizations' credentials never parses and unlocks the same bytes twice.

### `Pkcs12KeyStores` — every entry point

```java
public static SigningCredential load(byte[] pkcs12, char[] password)
public static SigningCredential load(InputStream pkcs12, char[] password)
public static SigningCredential load(Path pkcs12File, char[] password)
public static PrivateKey loadPrivateKey(Path pkcs12File, char[] password, String alias)
        throws GeneralSecurityException, IOException
public static List<X509Certificate> loadCertificateChain(Path pkcs12File, char[] password, String alias)
        throws GeneralSecurityException, IOException
```

- `load(byte[], char[])` is the entry point to reach for. A caller keeping one keystore per
  organization typically holds them encrypted in object storage; nothing here writes a temporary
  file, and a decrypted keystore never touches disk.
- `load(InputStream, char[])` reads its stream to the end and **does not close it** — the caller
  owns what it opened.
- The `load` methods **enumerate**: they expect exactly one private key entry and find it themselves,
  so a caller never stores an alias alongside the bytes. Zero or more than one is a `SigningException`
  saying **how many were found** (and, for more than one, their aliases).
- The password is a `char[]` and is **never copied into a `String`**. Nothing outlives the call, so a
  caller may zero the array as soon as the method returns, and no exception message can carry it.
- The two alias-addressed `Path` methods are the 0.1.0 surface, unchanged, for a keystore that
  genuinely holds several identities. They keep the JDK's own contract, including reporting a wrong
  PKCS#12 password as an `IOException`.
- Refusals raised by name: wrong password, truncated or non-PKCS#12 bytes, a stream that fails
  mid-read, a missing file, zero or several key entries, a key entry that is not a private key, and
  a key protected by a password other than the keystore's.

### `Pkcs11KeyStores` — every entry point

```java
public static PrivateKey loadPrivateKey(Path pkcs11ConfigFile, char[] pin, String alias)
        throws GeneralSecurityException, IOException
public static List<X509Certificate> loadCertificateChain(Path pkcs11ConfigFile, char[] pin, String alias)
        throws GeneralSecurityException, IOException
```

For a qualified certificate on its issuing USB token, addressed by alias — a token holds what it
holds, and enumeration would be guessing on someone else's hardware. Excluded from the coverage gate:
it needs a physical token and a vendor-supplied native module that no CI runner has.

## Transport

`UjpClient.builder()` takes `baseUrl` (default `UjpEndpoints.TEST_BASE_URL`), a `Serializer`, a
`Signer`, and optionally an `HttpClient`; `submit(Invoice)` and `status(String euid)` return
`SubmissionResult(euid, qrLink, status, message)`. Failures are `UjpException` (the gateway
refused, `errorCode()` may be null) or `UjpTransportException` (the request never completed).

**Every endpoint is provisional.**

| Constant | Value | Confidence |
|---|---|---|
| `TEST_BASE_URL` | `https://efakturatest.ujp.gov.mk` | Reported by integrators |
| `PRODUCTION_BASE_URL` | `https://efaktura.ujp.gov.mk` | Inferred from the sandbox hostname by pattern; never observed |
| `SALES_INVOICE_SEND` | `/JSONReceiver/sales-invoices/send` | Reported by integrators |
| `SALES_INVOICE_STATUS_TEMPLATE` | `/JSONReceiver/sales-invoices/status/%s` | Guessed by analogy — a guess about a guess |

Error codes named in `UjpException`: `E1012_CERTIFICATE_NOT_REGISTERED` (the only one with a
reported meaning), `E5004`, `E10001`, `E10002`, `E10003`.

## Consumers

- **kapar.net** — **generation only.** It builds and serializes documents; `UjpClient` is **never
  called**, and must not be until the "Specification status" verification lands (see FUTURE 1). The
  rule lives in kapar's own `CLAUDE.md` too.
- **invicta** — planned, parked on that project's e-invoice branch. It is the source of the
  requirements this library's 0.2.0 surface answers: per-organization keystores loaded from bytes or
  a stream, credit and debit notes, and natural-person buyers.

## Testing and gates

JUnit 5 + AssertJ; WireMock for `transport`. `./mvnw clean verify`, JDK 25.

- **90% line coverage**, one JaCoCo `BUNDLE` rule over `model`, `serialization` and `signing`
  together, excluding `signing.Pkcs11KeyStores`.
- JaCoCo class patterns use `/` as the package separator, not `.` — a dotted pattern matches nothing
  and makes the ratio check pass vacuously.
- `transport` is proven through WireMock rather than chased to the same number.
- The serializer is pinned by golden files in `src/test/resources/golden/`: `simple-invoice.json`,
  `multi-category-invoice.json`, `credit-note.json`, `consumer-invoice.json`. Regenerate
  deliberately, never by hand-editing, and update the inventory above in the same change.
- `scripts/verify-jws.py` verifies a produced compact JWS independently of this library's own code.

---

# FUTURE

1. **Verification pass against the official „API Спецификација".**
   Waits on: the document itself. efakturawiki.ujp.gov.mk is unreachable outside North Macedonian
   networks and the Wayback Machine has no copy, so it has to be fetched from inside the country.
   Owner: the operator. On arrival: cite the section, correct the field or endpoint, remove its
   `@ProvisionalSpec`, and update this file's inventory in the same change. Everything below depends
   on this landing first.

2. **Submission going live — `UjpClient` called for real.**
   Waits on: (1) above, **plus** a KIBS- or Telekom-issued qualified certificate and UJP portal
   registration of that certificate at `eujptest.ujp.gov.mk/ureg`. Owner: the operator for the
   certificate and registration; library maintainers for the client once the shape is confirmed.
   Until all three are in hand, consumers generate documents and do not submit them.

3. **Mandate timeline — probable, not confirmed.**
   October 2026 is the publicly stated target for voluntary go-live; April 2027 is the expected date
   e-invoicing becomes mandatory for VAT payers, with the enabling law possibly still in draft.
   Treat both as planning assumptions, not legal deadlines. Waits on: official publication.
   Owner: the operator. This is what sets the urgency of (1) and (2), and nothing else.

4. **PARKED — a status-polling endpoint that has been seen to work.**
   `SALES_INVOICE_STATUS_TEMPLATE` is derived by analogy from the submit path, with no integrator
   evidence behind it. Parked on: (1), or a single confirmed observation of the real path.
   Owner: library maintainers.

5. **PARKED — confirmed meanings for E5004 and E10001–E10003.**
   The codes were observed; what triggers them was not. Parked on: (1), or sandbox observation once
   (2) gives someone an account to observe with. Owner: library maintainers.

6. **PARKED — a second `Serializer` implementation (UBL 2.1 / XAdES).**
   The layering exists so a corrected schema, or an entirely different format, is a new `Serializer`
   rather than a change to `model`. Parked on: evidence that UJP accepts anything other than the
   proprietary JSON shape — current evidence says it does not, and the generic compliance sites
   describing UBL for North Macedonia appear to be describing the EU norm rather than this endpoint.
   Owner: library maintainers.

7. **CUT — a runtime dependency for JSON or JOSE.**
   Jackson or Nimbus would add a transitive surface, a version to track and a CVE feed to watch, for
   work this library already does in a few hundred lines it owns end to end. Do not re-propose while
   the wire shape stays a closed object graph and signing stays RS256-only. Reconsider only if a
   corrected spec needs general-purpose JSON — arbitrary nesting, unknown fields — which is a fact
   about (1), not a preference.

8. **CUT — a code list for `LineItem.unit`.**
   Nothing says UJP expects a unit at all, let alone a UN/ECE Rec 20 code. A code list would refuse
   units a seller legitimately writes. Do not re-propose without a spec section naming one.

9. **CUT — requiring an address, or a city, from a natural-person buyer.**
   Private individuals frequently have neither on file, and refusing them would refuse invoices that
   are perfectly legal. If the spec turns out to require more, that is a refusal for the consumer to
   raise by name before it calls this library. Do not re-propose without a spec section.

10. **CUT — signed (negative) amounts on a credit note.**
    The type carries the direction. Signing the amounts as well would state the same fact twice, let
    the two disagree, and force `LineItem`'s positive-quantity rule open for every document.

11. **CUT — enforcing a 13-digit ЕДБ format.**
    Presence is required on a business; the format is not checked, so legitimate cross-border
    counterparties and fixtures are not rejected by a rule this library cannot fully confirm.
