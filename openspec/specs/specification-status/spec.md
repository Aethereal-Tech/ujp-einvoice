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
and a subset of the page; **`primer_za_json_01.09.pdf`** dated 01.09.2026, thirty-odd worked JSON documents, one per
document type, which is the de facto field-level specification; **`sifrarnici_24042026.pdf`**, the code lists and
the mandatory markers, but only for fields that draw on a code list; and the **error catalogue** at `/error-codes`,
which is not linked from the specification page and was found by probing. An older example set from 2026-04-29 is
superseded and named only so a reader does not mistake it for current.

The code lists tag each field `З`, `ЗШ`, `ЗП`, `О`, `ОШ` or `ОП` and **carry no legend**. That the `З` forms mean
mandatory and the `О` forms optional is inferred from usage, not stated, and is not to be treated as confirmed.

#### Scenario: A wire-format claim is made without a citation
- **WHEN** a field name, endpoint, code or behaviour is called verified
- **THEN** it cites the specific document and section that confirms it, from the inventory above

#### Scenario: A mandatory marker is read off the code lists
- **WHEN** a field's obligation is taken from the code lists' `З` / `О` markers
- **THEN** it is recorded as inferred from usage, because the document publishes no legend

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

#### Scenario: A request is built without the identifying headers
- **WHEN** a request reaches the gateway without `X-EUJP-ID`, `X-EDB` or `X-SERIAL-NUMBER`
- **THEN** it is refused for missing required headers, before any payload validation

#### Scenario: A header disagrees with the payload it accompanies
- **WHEN** a header names a party or document type that the signed payload does not
- **THEN** the gateway refuses the mismatch, so the headers are derived from the document rather than configured
  independently of it

### Requirement: error codes are namespaced per API, so a flat constant set is a modelling error
Error codes SHALL be modelled per API — the eInvoice API, the JSON Receiver and the Web App each have their own —
and SHALL NOT be held as one flat set of constants.

The same string means different things in different interfaces: one code is "certificate already exists" in the
eInvoice API and "invalid calculated totals" in the JSON Receiver. A consumer that matches on the string alone will
eventually act on the wrong meaning, and the library would be the reason. This is a fault in how the library models
codes, not a quirk of the gateway to be documented and tolerated.

#### Scenario: The same code arrives from two different interfaces
- **WHEN** one code string is returned by the eInvoice API and by the JSON Receiver
- **THEN** the two are distinct values, because the interface is part of the code's identity

#### Scenario: A previously unknown code's meaning is looked up
- **WHEN** the meaning of a code such as the certificate-validation or company-validation failures is needed
- **THEN** it comes from the published error catalogue, which settles them, rather than from an integrator's
  account

### Requirement: status is polled with a signed POST, not fetched by id
Current status SHALL be read with `POST /api/v1/documents/sales-invoice/current-status` on the eInvoice API,
carrying a JWS-signed `{requestTimestamp, euid}`. Status history SHALL be read with the sibling `status` operation,
which also takes a date to read from.

0.2.0's `GET /JSONReceiver/sales-invoices/status/{euid}` is wrong in verb, path root, path shape and payload — the
library's own code called it a guess about a guess, and it was. The status vocabulary is **served** by the gateway
as a code list, so the corrected client carries the code and its name rather than a closed enumeration that goes
stale.

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
The production base URL, the signing algorithm and certificate-carrying choice, the rounding convention, the choice
between writing an explicit null and omitting a field, and the shape of a natural-person buyer SHALL keep their
`@ProvisionalSpec` markers after the wire layer is corrected.

Every URL in every obtained document names the sandbox host; no production host is published anywhere the
investigation reached, so the production URL stays an inference drawn by removing "test" from a hostname. The
rounding mode and scale are not published, and a wrong choice surfaces as a totals-mismatch refusal in production.
The examples write explicit nulls throughout and the gateway's tolerance for omission is unstated, so following the
examples is the defensible default rather than a known rule. And every one of the thirty-odd worked documents bills
a **company**: the buyer's tax number and full address are marked mandatory, no example shows a natural person, and
while the specification says some checks are skipped depending on the document type, it does not say which.

#### Scenario: A consumer asks whether a private individual can be billed
- **WHEN** a consumer needs to bill a natural person rather than a company
- **THEN** the library states that the shape is unknown rather than guessing one

#### Scenario: The production host is needed
- **WHEN** a production base URL is needed
- **THEN** it is treated as never observed, and confirmed with УЈП or read off the production portal before use

### Requirement: the gateway's own JSON Schema must be fetched from the API host before any field name is verified
The JSON Schema the gateway validates against SHALL be fetched and read before any field name in the corrected
serializer is called verified. It is **not** on the documentation host: it sits behind the Swagger interface on the
API host, and fetching it is a person's job.

The gateway publishes a distinct error for schema validation failure, which proves a real schema exists. The worked
examples are illustrations of it, not the thing itself, and the code lists cover only fields that draw on a code
list — description, quantity, unit, document number and date appear in examples alone, with no stated type, length
or obligation. Promoting names on the strength of examples is exactly how 0.2.0 came to be wrong, with better
evidence this time.

#### Scenario: Field names are promoted on the strength of the worked examples
- **WHEN** a serializer field name is called verified with only a worked example behind it
- **THEN** it is refused, because an example is not the schema the gateway enforces

### Requirement: the documentation host serves its certificate without the intermediate
A reader SHALL NOT conclude the documentation host is unreachable because a fetch failed. It serves a valid leaf
certificate and omits the intermediate, so a client that will not fetch the missing link itself fails while others
succeed.

This cost the project months: the site was assumed to be reachable only from inside North Macedonia, and it was
never a geography problem at all. The leaf in place at the time of the comparison expires on 2026-09-23; if it is
renewed without fixing the chain, the same symptom returns and the same wrong conclusion is available to draw.

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
