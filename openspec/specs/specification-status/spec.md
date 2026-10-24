# Specification status

## Purpose

**This is the most important capability in this library, and its answer changed on 2026-09-13.** Until then the
official „API Спецификација" had never been read, and everything here was reconstructed from public desk research
and hands-on accounts from Macedonian integrators. On that day the specification and its companion documents were
obtained and compared against the published 0.2.0, field by field. The comparison is in this directory as
`comparison-2026-09-13.md`, and the finding is not "still unverified" but something sharper: the reconstruction is
**wrong at the root**, and 0.2.0 would be rejected at the gateway's first validation step. This capability records
what the comparison settled, what it left open, what must not be done with the current release, and the rule that
governs moving anything from one column to the other.

## Requirements

### Requirement: 0.2.0's wire layer is known-wrong and MUST NOT be used to file
Version 0.2.0 of this library SHALL NOT be used to submit a document to УЈП. Its serializer produces a payload the
gateway rejects on its first validation step, and a submission is a filing with a tax authority.

This is stronger than the position the library held before 2026-09-13. "Reconstructed, not verified" invited a
consumer to try it and see; the comparison replaced that with knowledge. Six of roughly thirty-five wire names
survive. Three mismatches sit above the field level and each alone is fatal, and every VAT category value is wrong.
Nothing about the shape is salvageable by a patch.

#### Scenario: A consumer pins 0.2.0 and attempts a submission
- **WHEN** a consumer pinned to 0.2.0 sends a document to УЈП
- **THEN** the gateway rejects it at validation, because the payload shape, the request envelope and the required
  headers are all wrong
- **AND** the library's own record says so rather than leaving the consumer to discover it against a tax authority

#### Scenario: Someone proposes patching 0.2.0 rather than rewriting it
- **WHEN** a correction to 0.2.0's wire layer is proposed as a patch release
- **THEN** it is refused: the three structural mismatches and every serialized field name change together, which is
  a rewrite

### Requirement: the official specification has been obtained, and these are the documents it consists of
The official documents SHALL be treated as obtained, and any claim about the wire format SHALL cite one of them.

Retrieved 2026-09-13 from `efakturawiki.ujp.gov.mk`: the **API Спецификација** page at `/тест_апи`, which is the
authoritative page; **`api-documentation-public8.pdf`**, revision "Public8", the same material in downloadable form
and a subset of the page; **`primer_za_json_08.10.pdf`** dated 08.10.2026 (D3'), thirty-odd worked JSON documents, one per
document type, which is the de facto field-level specification and replaces the 01.09.2026 set (D3); **`sifrarnici_24042026.pdf`**, the code lists and
the mandatory markers, but only for fields that draw on a code list; and the **error catalogue** at `/error-codes`,
which is not linked from the specification page and was found by probing. The older example sets (01.09.2026 and
2026-04-29) are superseded and named only so a reader does not mistake them for current. The sources added on
2026-10-19 are in the next requirement.

The code lists tag each field `З`, `ЗШ`, `ЗП`, `О`, `ОШ` or `ОП`. They carry no legend, but UJP's `/json-struktura`
publishes one (retrieved 2026-10-19): `З` mandatory, `О` optional, `Ш` code (шифра), `П` name (назив). The older
guide `upatstvo_efaktura_opsta_24042026.pdf` carries it too, so it predates the first comparison, which had not read
that guide. What stays unexplained is the asterisk in `ЗШ*` (`buyerTin` on 100, `buyerForeignTin` on 510).

#### Scenario: A wire-format claim is made without a citation
- **WHEN** a field name, endpoint, code or behaviour is called verified
- **THEN** it cites the specific document and section that confirms it, from the inventory above

#### Scenario: A mandatory marker is read off a type page or the code lists
- **WHEN** a field's obligation is taken from a `З` / `О` marker
- **THEN** it cites the `/json-struktura` legend, which publishes the meaning
- **AND** a `ЗШ*` marker is recorded as conditional on something UJP does not state, never as plain mandatory

### Requirement: every source is pinned with the date it was retrieved, and the changelog is the only history
Every document this capability cites SHALL be named with its state and the date it was retrieved, and a source that is
replaced SHALL be recorded as replaced rather than deleted from the record. The detailed field-by-field record stays
in `comparison-2026-09-13.md` and `comparison-2026-10-19.md`; this is the index into them.

Retrieved 2026-10-19 with `curl` from `efakturawiki.ujp.gov.mk` unless a row says otherwise (`updated-at` is the
page's own stamp, `Last-Modified` the PDF's):

| Source | State when retrieved | Role |
|---|---|---|
| D1 „API Спецификација", `/тест_апи` | `updated-at` 2026-10-16T14:36:59Z | Endpoints, headers, validations, formulas, response shapes; links D3' |
| D2 `downloads/api-documentation-public8.pdf` | 35 pp, `Last-Modified` 2026-08-20 | A subset of D1: lacks `/purchase-tax-indicators*`, calls `/reject-reasons` what D1 calls `/reject-reason` |
| **D3' `downloads/primer_za_json_08.10.pdf`** | 71 pp, `Last-Modified` 2026-10-08, sha256 `1011fc23...` | **Replaces D3.** The only set D1 links. One example differs from D3: the exempt-from-tax one now uses `DDV-8-2` and carries members no table lists |
| D3 `primer_za_json_01.09.pdf` | 71 pp, `Last-Modified` 2026-09-07 | Superseded; still served |
| `primer_za_json_3.pdf`, `primer_za_json_2.pdf` | 23 pp (created 2026-07-29), 16 pp (created 2026-04-29) | Interim and older sets; superseded |
| D4 `sifrarnici_24042026.pdf` and `/шифрарници` | PDF 8 pp, created 2026-04-28; the page `updated-at` 2026-10-16T07:52Z | Code lists and markers, for code-list fields only |
| D5 `/error-codes` | `updated-at` 2026-10-05T21:17Z | The error catalogue (`UjpErrorCodes`) |
| D6 `/ДоделувањепристапвоеУЈП` | `updated-at` 2026-10-14; `pristap_eujp.pdf` 4 pp, 2026-06-13 | Privileges and certificate registration |
| The 23 per-type pages, `/100-faktura` to `/999-povrzani-dokumenti` | most `updated-at` 2026-10-14; `/100-faktura` 2026-10-16 | **A source of its own:** the field table of each kind (code `UJPnn-nn`, name, type, length, `З`/`О`), which the model and the serializer cite |
| `/json-struktura` | read 2026-10-19 | The legend, and the rounding rules (`ROUND_HALF_UP`, four decimals on a line, two on the totals) |
| `/status` | `updated-at` 2026-10-15 | The ten status codes (`SubmissionStatus` constants) |
| `/danocni-indikatori` | `updated-at` 2026-08-27 | The tax-indicator list, about 104 codes (`TaxIndicators`) |
| FAQ `/ЧПП` | `updated-at` 2026-06-09 | The JWS header `{"alg":"RS256","typ":"JWT"}` |
| Changelog `/известувања` | entries 02.01.2026 to 16.10.2026 | Dated changes; the only history the wiki exposes (next requirement) |
| `einvoice_api` API description | fetched 2026-10-20 by the owner from the API host, `openapi-2026-10-20.md` | The `einvoice_api` shapes; not the JSON Receiver |

The 08.10 examples, not the 01.09 set, are what the goldens' numbers come from; D3 stays in the record only because it
is still served.

#### Scenario: A reader asks which example set is current
- **WHEN** an example is cited for a field's shape
- **THEN** it is cited to D3' (08.10.2026), and the 01.09.2026 set is named only as superseded

### Requirement: the changelog is the only history the wiki exposes
The changelog page `/известувања` ("Промени во системот еФактура") SHALL be treated as the only history of the
documentation available, and a re-run of the comparison SHALL read it.

The host is Wiki.js. Its page history (`/h/...`), page source (`/s/...`) and `?action=history` all answer the login
shell of about 2 KB, so per-page history is not public; `Special:RecentChanges` and `/sitemap.xml` are 404 and
`robots.txt` is empty. What a reader has is the changelog's dated entries, each page's `updated-at` and each PDF's
`Last-Modified`. That cannot tell "changed since the last comparison" from "missed by it": the legend, the FAQ's
algorithm and the rounding rule were of the second kind. A page edited without a changelog entry is invisible except
through its `updated-at`.

#### Scenario: A page's content is compared against an earlier pass
- **WHEN** a page differs from the earlier comparison
- **THEN** the changelog's entries and the page's `updated-at` date the change where they can, and the record says so
  where they cannot

### Requirement: the correction is a breaking release, never a 0.3.0
The corrected wire layer SHALL ship as `1.0.0` or as a parallel package beside 0.2.0, never as a `0.3.0`.

Every serialized field name changes, so any consumer asserting on serializer output is invalidated and any stored
0.2.0 document is unconvertible — it lacks data the model never captured. `UjpClient.Builder` gains required
configuration, `status` changes verb, path and return type, `SubmissionStatus` and `SubmissionResult` change shape,
`VatCategory`'s values change meaning in a way that compiles silently, `DocumentType`'s codes change and `INVOICE`
stops being expressed by absence, and the single document reference becomes a typed list. A consumer should migrate
deliberately rather than discover this at the first rejection.

#### Scenario: EXEMPT is carried forward into the corrected release
- **WHEN** the corrected release handles the exempt category
- **THEN** `EXEMPT` is renamed or removed rather than quietly redefined, because it becomes a family of tax
  indicators and a silent redefinition would compile

### Requirement: the signed payload nests under `document`, and is not a flat invoice
The signed payload SHALL be `{requestTimestamp, document: {header, docReferences, seller, buyer, docPayment,
docItems, docTotals, vatTotals}}`, with `voidReason` and `correctionReason` as **siblings of `document`** rather
than members of it.

0.2.0 emits a flat object with `invoiceNumber` at the top level and no `requestTimestamp` at all. The timestamp is
not decoration: it must be Skopje-local, formatted `2026-01-05T12:00:00`, and within five minutes of server time,
and there is a service that serves that time.

#### Scenario: A document is serialized
- **WHEN** a document is serialized for submission
- **THEN** its blocks nest under `document`, and any void or correction reason sits beside `document`, not inside it

#### Scenario: The request timestamp drifts
- **WHEN** the request timestamp is more than five minutes from the gateway's server time
- **THEN** the request is refused, which is why the timestamp is sourced from the server-time service rather than
  from the caller's clock

### Requirement: the request body is an envelope carrying the JWS, not the JWS itself
The request body SHALL be the JSON envelope `{requestTimestamp, jws}`. The compact JWS SHALL NOT be posted as the
body.

0.2.0 posts the raw compact serialization with `Content-Type: application/jose`. The signing envelope itself is
unchanged and was reconstructed correctly: JWS over JSON, not UBL 2.1 with XAdES.

#### Scenario: A signed document is sent
- **WHEN** a signed document is sent to the gateway
- **THEN** the compact JWS travels as a member of a JSON envelope, alongside the request timestamp

### Requirement: four request headers are mandatory, and the send path carries `/api/v1`
Every request SHALL carry `X-EUJP-ID`, `X-EDB` and `X-SERIAL-NUMBER`, and a send SHALL also carry
`X-DOC-TYPE-CODE`. The send path SHALL be `/JSONReceiver/api/v1/sales-invoices/send`.

0.2.0 sends none of the four and omits the version segment. The gateway publishes a distinct error for missing
headers, another for a header that disagrees with the payload, and one per header for an invalid value, which is
how much it cares. The consequence for the client is configuration: it needs an e-УЈП identifier, an ЕДБ and the
signing certificate's serial number before it can send anything. The gateway also limits a user to one request per
second.

All of this is built in `UjpClient` and cited to D1 „API Documentation", „Време на сервер" and „Праќање на документ"
(`transport`): the builder requires the three identifiers, `X-DOC-TYPE-CODE` is the document kind's code, the send path
carries `/api/v1`, `requestTimestamp` is read from `GET /einvoice_api/api/v1/server-time` on every call, and requests
are spaced one second apart per client instance. `TEST_BASE_URL` and the send path lost their markers with that
citation; the old `SALES_INVOICE_STATUS_TEMPLATE` is gone.

#### Scenario: A request is built without the identifying headers
- **WHEN** a request reaches the gateway without `X-EUJP-ID`, `X-EDB` or `X-SERIAL-NUMBER`
- **THEN** it is refused for missing required headers, before any payload validation

#### Scenario: A header disagrees with the payload it accompanies
- **WHEN** a header names a party or document type that the signed payload does not
- **THEN** the gateway refuses the mismatch, so the headers are derived from the document rather than configured
  independently of it

### Requirement: error codes are namespaced per API, so a flat constant set is a modelling error
Error codes SHALL be modelled per API — the eInvoice API, the JSON Receiver and the Web App each have their own —
and SHALL NOT be held as one flat set of constants. The catalogue in `UjpErrorCodes` is D5 as it stood on
2026-10-05 and is **verified**, cited to D5 (`https://efakturawiki.ujp.gov.mk/error-codes`, page `updated-at`
2026-10-05T21:17Z, retrieved 2026-10-19): 55 codes under eInvoiceApi, 104 under JSON Receiver and 32 under Web App,
every entry on the page, each meaning as the page states it, untranslated.

The same string means different things in different interfaces: E1020 is "This certificate for this company already
exists!" in the eInvoice API and "Invalid calculated totals" in the JSON Receiver, E4023 is "Notification with ID was
not found" in one and "Invalid X-EDB" in the other, and E1019 differs between all three. A consumer that matches on
the string alone will eventually act on the wrong meaning, and the library would be the reason. This is a fault in
how the library models codes, not a quirk of the gateway to be documented and tolerated. `ErrorNamespace` names the
three catalogues and `UjpErrorCode(namespace, code, meaning)` carries the pair as its identity.

The catalogue is a snapshot, and a code it does not list resolves to empty, never to a guess. The OpenAPI
description's `ErrorStatusBean` enum (72 codes, `openapi-2026-10-20.md`) and D5's eInvoiceApi section (55) overlap
fully in one direction only: every D5 eInvoiceApi code is in the enum, and 17 enum codes — E1003, E1005, E1007,
E1008, E4006, E4017, E4018, E4019, E4020, E4021, E4022, E4024, E4027, E5009, E5010, E5012 and E5013 — are listed by
D5 under no eInvoiceApi entry (E4018–E4022, E4024 and E5012 appear in D5 under the JSON Receiver only). They are
left out of the catalogue rather than recorded with an invented or null meaning, so that every entry is a quotation.
The error body of `einvoice_api` is `ErrorStatus(errorCode, errorMessage, errorPropertyMap)`, cited to
`openapi-2026-10-20.md`; its `errorCode` stays a string so that a code outside the catalogue survives. The JSON
Receiver's own error body is described by no obtained document.

`UjpException` keeps the HTTP status, the code, the raw response body and the namespace (`null` when unknown), and
resolves the pair with `known()`. A code without a namespace is not resolved: the string alone does not say which
meaning applies.

The meanings that were open are settled by D5: E1012 is "Certificate not found" in all three catalogues, broader than
the "not pre-registered" gloss the library carried (the registration case is E1013, "User is not authorized to sign
documents for this company"); E5004 is "Certificate validation failed" in all three; E10001 is "Missing TIN during
company validation process", E10002 "An incorrect company name was provided" and E10003 "An incorrect VAT number was
provided for the company", all JSON Receiver. E1033–E1035, E500719/E500720 (JSON Receiver) and E5014–E5019
(eInvoiceApi) are in. The `@ProvisionalSpec` markers on the five flat constants are gone with the constants.

#### Scenario: The same code arrives from two different interfaces
- **WHEN** one code string is returned by the eInvoice API and by the JSON Receiver
- **THEN** the two are distinct values, because the interface is part of the code's identity

#### Scenario: A previously unknown code's meaning is looked up
- **WHEN** the meaning of a code such as the certificate-validation or company-validation failures is needed
- **THEN** it comes from the published error catalogue, which settles them, rather than from an integrator's
  account

#### Scenario: A code the catalogue does not list arrives
- **WHEN** the gateway returns a code D5's 2026-10-05 page does not list, or the namespace is not known
- **THEN** `UjpException.known()` is empty, and the code and the raw body are kept as received

### Requirement: status is polled with a signed POST, not fetched by id
Current status SHALL be read with `POST /api/v1/documents/sales-invoice/current-status` on the eInvoice API,
carrying a JWS-signed `{requestTimestamp, euid}`. Status history SHALL be read with the sibling `status` operation,
which also takes a date to read from.

0.2.0's `GET /JSONReceiver/sales-invoices/status/{euid}` is wrong in verb, path root, path shape and payload — the
library's own code called it a guess about a guess, and it was. The status vocabulary is **served** by the gateway
as a code list, so the corrected client carries the code and its name rather than a closed enumeration that goes
stale.

Built: `UjpClient.status` and `UjpClient.statusHistory`, cited to D1 „Тековен статус на излезна фактура" and „Статус
историја на документ" and the API description's `getSalesInvoicesCurrentStatus` and `getStatusSalesInvoice`.
`SubmissionStatus` carries the served `statusCode` and `statusName`; the ten codes of `/status` (`updated-at`
2026-10-15) are named constants, and a code outside them passes through. The old four-word vocabulary and its marker
are gone. The history call's `dateFrom` is the one open point (the table below).

#### Scenario: A document's current status is wanted
- **WHEN** a consumer asks for a submitted document's current status
- **THEN** the request is a signed POST carrying the EUID, and the answer is a status code with its name

### Requirement: a document type code is mandatory on every document, and cancellation is a separate axis
Every document SHALL carry `header.docType`: `100` for an invoice, `110` for a credit note («Книжно одобрение»),
`120` for a debit note («Книжно задолжение»), each with its name. Amounts on notes SHALL stay **positive**.
Cancellation and correction SHALL be modelled separately from the document type, as `header.docStorno` with its own
void or correction reason object.

0.2.0 treats the type as a discriminator that appears only on notes, and an invoice writes no type field at all.
That cannot survive: the code is mandatory everywhere. The document a note relates to travels in a typed list of
references rather than a single corrected-invoice object, and whether a given type must carry a reference is
published by a code-list service rather than fixed here. The positive-amounts decision the library already took is
confirmed.

#### Scenario: An ordinary invoice is serialized
- **WHEN** an ordinary invoice is serialized
- **THEN** it carries the document type code for an invoice explicitly, rather than expressing it by absence

#### Scenario: A document is cancelled
- **WHEN** a document is cancelled rather than credited
- **THEN** that is expressed on the cancellation axis with a reason object, not by choosing a different document
  type

### Requirement: the VAT rate codes are `DDV-A`, `DDV-V`, `DDV-B` and `DDV-G`, and exemption is a family
The wire values for VAT SHALL be `DDV-A` at 18%, `DDV-V` at 10%, `DDV-B` at 5% and `DDV-G` at 0%. Exemption SHALL
be modelled as a family of tax indicators, not as one category.

0.2.0 writes the rate itself as the value — `"18"`, `"10"`, `"5"`, `"0"`, `"EXEMPT"` — and every one of those is
wrong on the wire. Note that the letters are not alphabetical by rate: `V` is 10 and `B` is 5. The **rates**
themselves remain verified public tax law, independent of the wire format, and are unaffected by this correction;
what was wrong is how they are spelled to the gateway. The corrected model needs a tax-indicator concept of its own,
carrying how the indicator affects the calculation.

#### Scenario: A line's VAT category is written
- **WHEN** a line's VAT category is serialized
- **THEN** it is written as the gateway's code, not as the percentage

#### Scenario: A rate is read for calculation
- **WHEN** 18%, 10%, 5% or 0% is used to compute a figure
- **THEN** it is treated as verified public tax law, unchanged by the wire-format correction

### Requirement: nothing is promoted to verified without citing an obtained document
Nothing marked `@ProvisionalSpec`, and no endpoint constant, SHALL be promoted to verified without citing the
specific section of an obtained document that confirms it. A marker SHALL be removed in the same change as the
correction it describes, never separately.

A sandbox that behaves as expected is evidence, not confirmation. This rule is why the library was wrong rather
than wrong and surprised: the reconstructions were self-consistent and still false. It is unchanged by the
specification arriving — it is what the specification is now cited *for*.

#### Scenario: A field behaves correctly against the sandbox
- **WHEN** a marked field or endpoint behaves as expected against the sandbox
- **THEN** that alone does not remove its marker or make it verified

#### Scenario: A field is corrected against the specification
- **WHEN** a field is corrected against an obtained document
- **THEN** the section is cited, the marker is removed, and this capability is updated in the same change

### Requirement: these remain unverified even after the correction lands
The production base URL, whether the JWS header carries `x5c`, the choice between writing an explicit null and omitting
a field, and `docGrossAmountR`'s rounding SHALL keep their `@ProvisionalSpec` markers after the wire layer is
corrected. The signing algorithm no longer does: the FAQ (`/ЧПП`, `updated-at` 2026-06-09, retrieved 2026-10-19,
section 2 „Генерирање на JWS барање") states the header `{"alg":"RS256","typ":"JWT"}` and `signature =
RSA-SHA256(...)`, so RS256 and `typ` are cited and `JwsRs256Signer` writes exactly that header (`signing`).

Every URL in every obtained document names the sandbox host; no production host is published anywhere the
investigation reached, so the production URL stays an inference drawn by removing "test" from a hostname. The
rounding **mode and scale are now published** (/json-struktura, "Заокружување": `ROUND_HALF_UP`, four decimals on a
line, two on the totals, whole numbers for UJP08-08 to UJP08-10) and the library follows them; what stays open is the
rounding of `docGrossAmountR`, which the 28.04 changelog calls banking rounding and the newer guide calls ordinary, and
which none of the worked examples (no value is a tie) can settle. The examples write explicit nulls throughout and the
gateway's tolerance for omission is unstated, so following the examples is the defensible default rather than a known
rule. Every one of the worked documents bills a **company**, and kind 170 (the invoice to a natural person) is not in
1.0, so a natural-person buyer is refused by name rather than guessed at.

#### Scenario: A consumer asks whether a private individual can be billed
- **WHEN** a consumer needs to bill a natural person rather than a company
- **THEN** the library refuses the buyer by name and states that the document is kind 170, which it does not ship

#### Scenario: The production host is needed
- **WHEN** a production base URL is needed
- **THEN** it is treated as never observed, and confirmed with УЈП or read off the production portal before use

### Requirement: every remaining `@ProvisionalSpec` is listed once here, grouped by what would settle it
The model, the money rule and the serializer of the wire-layer correction are built. Every `@ProvisionalSpec` in
`src/main` SHALL appear in the one table below, with what is provisional, the sources that conflict or are silent, and
what would settle it, and a marker SHALL stay until that settles it. The table is the sweep of 2026-10-20: 19 markers
on 19 elements, none of them settled by what the record now holds (each row was checked against
`comparison-2026-10-19.md` and the API description of 2026-10-20, and none of those confirms a choice). The wire names `UjpJsonSerializer` writes are the ones the type pages' tables
and the 08.10 examples print, each with its UJP code; they are cited to those pages, and none is **verified** against
the gateway's schema (the schema requirement below). The 0.2.0 reconstructions (`documentType`, `correctedInvoice`,
`invoiceNumber`, `lineItems`, `byCategory` and the rest) went with the old model; the markers they carried went with
them.

The three groups, by what settles a row:

- **(a) The gateway's document schema, which UJP has not published.** The JSON Receiver, which takes the signed
  document, has no API description; the `einvoice_api` one does not describe the `document` (`InvoicePayload.payload`
  is an opaque string, `openapi-2026-10-20.md`). Eight markers.
- **(b) The production URL.** No obtained document names a production host. One marker.
- **(c) Sandbox evidence or a UJP answer.** A rule, a default or a reading that no schema would state, or a catalogue
  the gateway serves and no obtained copy holds. Sandbox behaviour alone is evidence, not confirmation (above), so a
  row here closes on a UJP answer or on a served catalogue cited to its retrieval. Ten markers.

| Marker | What is provisional | Sources that conflict or are silent | What would settle it |
|---|---|---|---|
| **(a) The gateway's document schema** | | | |
| `UjpJsonSerializer` (class) | `null` for an absent scalar and `[]` for an empty list rather than omission; the envelope on 110, 120 and 130; one reason object where the 310 example writes both as null; no member written that no table documents (`source`, a trailing `requestTimestamp`, `sellerEinvoiceUser`, `buyerEinvoiceUser`, `sellerEntityType`, `buyerEntityType`, `docGlobalDiscountAmount`, `docItemUnitItemDiscountAmount`, `docItemHs`); which optional members are written when absent, among them a line's empty `attributes` | Every example writes explicit nulls; D1 states a rule for empty amounts alone ("празно или не се прати" is treated as 0). The 21.10 examples are bare on 110, 120, 130 and the ordinary 100s, a `document` with its reason and no timestamp on the 100 storno and correction, and enveloped on 310 alone; the members appear in some examples and in no table; a line's `attributes` is in the one example that has a line attribute and in no other | The JSON Receiver's JSON Schema |
| `UjpJsonSerializer.header()` | `docAlt`: written as a JSON boolean. `docIndicator` is written on every kind | `docAlt` is Boolean in the 14.09 changelog, Integer(4) in the 08.10 tables, `false` in every 21.10 example of 100 and absent from the others. Only the 100 table lists `docIndicator`; the 110, 120, 130 and 310 examples carry it | The schema's type for `docAlt` and its member list per kind |
| `UjpJsonSerializer.appendCurrencyRate` | `docCurrencyDate` and `docCurrencyExchRate` are the document date and 1 beside MKD, `null` for another currency | The 130 and 310 tables list them (UJP10-10 `О` on 310 and `ОП` on 130, UJP10-11 `О` on both) and say nothing of how they are filled; the 15.10 changelog removed them from 100, the 21.10 examples of 100 no longer print them, and the 130 and 310 examples still do | The schema, with the per-kind member list |
| `UjpJsonSerializer.item` | `docItemTotalOriginalPriceWoVat` is written on every kind | In every example and the 310 table (UJP07-15), not in the 100, 110, 120 or 130 tables; the 24.06 changelog made it no longer mandatory | The schema's member list per kind |
| `UjpJsonSerializer.totals` | The advance block and the increase and decrease are written on every kind | In every 100 example; the 110, 120, 130 and 310 examples print no increase or decrease, and the tables list UJP08-06 to UJP08-09 on the 100 table alone | The schema's member list per kind |
| `UjpJsonSerializer.vatTotal` | An absent `vatTaxIndicatorNote` is `null` | Every 21.10 example writes `null` (the 08.10 examples of 100 wrote `""`, so that conflict is gone); nullability itself is unstated | The schema (nullability) |
| `UjpClient.JSON` | The send request's `Content-Type` is `application/json` | No obtained document states it for the JSON Receiver; the `einvoice_api` operations declare `application/json` | The JSON Receiver's API description |
| `UjpClient.refusal` | The JSON Receiver's error body is parsed as either `einvoice_api` shape (`GenericAPIResponse` or a bare `ErrorStatusBean`) and otherwise kept raw | D1, D2 and the flow chart show only "Враќање на грешка"; every example writes `"errorStatus": null` | The JSON Receiver's API description |
| **(b) The production URL** | | | |
| `UjpEndpoints.PRODUCTION_BASE_URL` | The host, inferred from the sandbox name by removing "test"; used by nothing | Every URL in every obtained document names `efakturatest` or `eujptest`; no document uses the word production; the programme is described as test phases | A host UJP publishes or confirms, or the production portal |
| **(c) Sandbox evidence or a UJP answer** | | | |
| `DocumentKind.requiresReferences` | Reported, not enforced: UJP02-01 is `З` on 110 and 120 | The 110 and 120 pages mark it `З (1..n)`; their 08.10 examples send `[]`; the page's own note makes the four sub-fields conditional on a reference being used | A UJP answer, or the served document-types catalogue (`isReferenceMandatory`, `hasReference`) read and cited |
| `Mode` | A storno or correction is not refused on 110, 120 and 130 | `docStorno` has no row on those three pages and their examples send 0 | A UJP answer |
| `VoidReason.needsComment` | The free-text comment of "Друго" is mandatory | The code list says "Друго (внес на слободен текст)"; `VoidReasonDto` in the API description has no comment flag | A UJP answer |
| `CorrectionReason.needsComment` | The same, for the correction reasons | The same; `CorrectionReasonDto` has no comment flag | A UJP answer |
| `Payment` | An absent currency reads as MKD | Every example carries MKD; the tables mark the currency optional (`ОШ`) and say nothing of a default; the 28.07 changelog says it will be dropped | A UJP answer |
| `VatTotal.of` | The VAT shown under a `PRENESEN` indicator is the base times the group's rate | The API description says the indicator shows its VAT with a note; the one art. 32 example that shows an amount is not internally consistent | A UJP answer, or a worked example that reconciles |
| `TaxIndicators` | `vatImpact` of every entry except `DDV-A`, `DDV-V`, `DDV-B` and `DDV-G` is the library's reading of the name | `/danocni-indikatori` has no `vatImpact` column; the API description's `TaxIndicatorDto` serves it per indicator but no obtained copy holds the values | The served tax-indicators catalogue read and cited to its retrieval, or a UJP answer |
| `Money.whole` | `docGrossAmountR` rounds half-up (in MKD, where it stays a whole number) | The 28.04 changelog calls it banking rounding; `/json-struktura` says `ROUND_HALF_UP`; no example value is a tie (the 21.10 multi-item prints a gross of 667.5, from an unrounded 667.5032, and 668 is the answer under either mode) | A UJP answer, or a tie value in a worked example |
| `Money.grossRounded` | In a foreign currency `docGrossAmountR` is the gross at two decimals, not a whole number | The 19.10 changelog makes the field Decimal 15.2 and reserves decimals for foreign currencies, with no rounding rule; the two 21.10 EUR examples (kind 510, page 76) have a whole gross (300, 295), which both readings print alike | A UJP answer, or a foreign-currency example whose gross has a fraction |
| `JwsRs256Signer.x5c` | The claim is written only when a chain is supplied | No obtained document mentions it; the certificate is identified by the registered certificate and `X-SERIAL-NUMBER` (`E5012`); the FAQ's header has two members; the API description's example header is a truncated `{"alg":"RS256"}` | A UJP answer |
| `UjpClient.statusHistory` | `dateFrom` is written as a date and omitted when `null` | A date-time in D1 (`2025-12-25T13:07:30`); a date in the API description's example (`2026-01-01`, "on or after this date"); no document says it may be omitted | A UJP answer |

Open questions for the schema, not decisions: the `ЗШ*` asterisk, the members the examples carry and no table documents,
`MARGINALNA`'s arithmetic (the model refuses it), and the `vatType` of an indicator.

#### Scenario: A marker is removed
- **WHEN** a source obtained later confirms one of these choices
- **THEN** the marker goes, the section is cited, and this table changes in the same commit

#### Scenario: A marker is added
- **WHEN** a new `@ProvisionalSpec` is written in `src/main`
- **THEN** it is added to this table in the same commit, in the group of what would settle it

#### Scenario: A reader asks where the golden numbers come from
- **WHEN** the figure in a golden is questioned
- **THEN** it is the number UJP's own example prints, which a test asserts the model derives; the golden is this library's output

### Requirement: the gateway's own JSON Schema must be fetched from the API host before any field name is verified
The JSON Schema the gateway validates against SHALL be fetched and read before any field name in the corrected
serializer is called verified. It is **not** on the documentation host: it sits behind the Swagger interface on the
API host, and fetching it is a person's job.

The gateway publishes a distinct error for schema validation failure, which proves a real schema exists. The worked
examples are illustrations of it, not the thing itself, and the code lists cover only fields that draw on a code
list — description, quantity, unit, document number and date appear in examples alone, with no stated type, length
or obligation. Promoting names on the strength of examples is exactly how 0.2.0 came to be wrong, with better
evidence this time.

Partly met on 2026-10-20: the description of `einvoice_api` was fetched and is committed under
`src/test/resources/ujp/` (`openapi-2026-10-20.md`), and the transport and status shapes are checked against it
(`transport`). It does not cover the JSON Receiver, so the `document`, its optionality and the receiver's error body
still have no schema behind them.

#### Scenario: Field names are promoted on the strength of the worked examples
- **WHEN** a serializer field name is called verified with only a worked example behind it
- **THEN** it is refused, because an example is not the schema the gateway enforces

### Requirement: the documentation host serves its certificate without the intermediate
A reader SHALL NOT conclude the documentation host is unreachable because a fetch failed. It serves a valid leaf
certificate and omits the intermediate, so a client that will not fetch the missing link itself fails while others
succeed. The page is fetched with `curl` against the system trust store, never with `-k`.

This cost the project months: the site was assumed to be reachable only from inside North Macedonia, and it was
never a geography problem at all. The state as of 2026-10-19: the leaf (`CN=efakturawiki.ujp.gov.mk`, issued by Let's
Encrypt `YR1`) was **renewed on 2026-09-14** and **expires on 2026-12-13**, and the chain is **still leaf-only**
(`openssl s_client -showcerts` prints one certificate and `verify error:num=21`); `curl` answers `HTTP 200` with
`ssl_verify=0`. The earlier leaf's expiry of 2026-09-23 passed without the chain being fixed, so a further renewal
cannot be assumed to fix it, and the same symptom, and the same wrong conclusion, remain available to draw until it
is fixed.

#### Scenario: A fetch of the documentation host fails to verify
- **WHEN** a client fails to verify the documentation host's certificate chain
- **THEN** the cause is the missing intermediate, and a client that completes the chain itself retrieves the page

### Requirement: what the reconstruction got right
These SHALL be treated as confirmed by the comparison: JWS as the signing envelope; a proprietary JSON schema
rather than UBL 2.1 with XAdES; that a submission answers with an EUID and a QR verification link; the sandbox
host; that amounts stay positive on credit and debit notes; falling back to an unknown value rather than throwing;
not enforcing an ЕДБ format; not inventing a unit code list; and the names `seller`, `buyer`, `city`, `postalCode`,
`euid` and `message`.

Worth stating plainly, because the rest of this capability is a list of what was wrong. The certificate
prerequisite is confirmed in substance — a qualified certificate registered in the УЈП portal, with a published
privilege model and an error code that enforces it — but the issuing authorities the library names are confirmed by
no obtained document.

#### Scenario: A generic compliance source claims UBL 2.1
- **WHEN** a source describes North Macedonia as using UBL 2.1 with XAdES
- **THEN** it is describing the EU norm rather than this gateway, which the obtained documents now settle directly

### Requirement: the ЕДБ is a verified concept whose format is deliberately unenforced
The ЕДБ SHALL be treated as a verified party identifier whose exact format is deliberately left unchecked.

Presence is a public identifier requirement on a business. The format is not the library's to police, and the
comparison confirms the gateway validates it; a consumer's own screen is where a typo is best caught.

#### Scenario: A tax identifier of an unexpected shape is supplied
- **WHEN** a business party's tax identifier does not match an expected digit pattern
- **THEN** it is accepted, because the concept is verified and the format is deliberately unchecked
