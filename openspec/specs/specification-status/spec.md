# Specification status

## Purpose

**This is the most important capability in this library.** The authoritative source for the UJP e-Faktura wire
format — efakturawiki.ujp.gov.mk's „API Спецификација" — is unreachable outside North Macedonian networks, and
nobody who built this library has read it. Everything else recorded here was reconstructed from public desk
research and hands-on accounts from Macedonian integrators (forum posts, blog write-ups, support-channel
screenshots), as of 2026-08-28. This capability records exactly which parts are verified, which are reconstructed
and at what confidence, and the one rule that governs moving anything from one column to the other.

## Requirements

### Requirement: nothing marked @ProvisionalSpec is promoted to verified without citing the official spec
Nothing marked `@ProvisionalSpec`, and no endpoint in `UjpEndpoints`, SHALL be promoted to verified without citing
the specific section of the official „API Спецификација" (efakturawiki.ujp.gov.mk) that confirms it.

A sandbox account behaving as expected is evidence, not confirmation — integrators' own reconstructions have been
self-consistent and still wrong before. "I tested it against efakturatest and it worked" is not a citation.

#### Scenario: A field behaves correctly against the sandbox
- **WHEN** a `@ProvisionalSpec`-marked field or an `UjpEndpoints` constant behaves as expected against
  `efakturatest.ujp.gov.mk`
- **THEN** that alone is not sufficient to remove its `@ProvisionalSpec` marker or call it verified
- **AND** promoting it requires citing the specific section of the official spec that confirms it

#### Scenario: The official spec becomes readable
- **WHEN** the official „API Спецификација" is read for a marked field or endpoint
- **THEN** the section is cited, the field or endpoint is corrected if needed, its `@ProvisionalSpec` marker is
  removed, and the specification-status inventory is updated in the same change

### Requirement: the wire format is a proprietary UJP JSON schema, signed as compact JWS, not UBL 2.1
The wire format SHALL be treated as a proprietary UJP JSON schema, signed as compact JWS (RS256), and NOT as UBL
2.1 / XAdES.

Both are reconstructed at moderate confidence: the JSON-and-JWS shape is consistent across multiple independent
integrator accounts, while the not-UBL conclusion contradicts generic e-invoicing compliance sites, which appear to
describe the EU norm rather than UJP's actual endpoint.

#### Scenario: A generic compliance source claims UBL 2.1
- **WHEN** a generic e-invoicing compliance source describes North Macedonia as using UBL 2.1 / XAdES
- **THEN** that is treated as describing the EU norm rather than UJP's actual endpoint, at moderate confidence

### Requirement: submission returns an EUID and a QR verification link
Submission SHALL be treated as returning an EUID and a QR verification link, reconstructed at moderate confidence,
reported by integrators.

#### Scenario: A submission succeeds
- **WHEN** an invoice is submitted
- **THEN** the response is expected to carry an EUID and a QR verification link, at moderate confidence

### Requirement: the endpoint family is /JSONReceiver/sales-invoices/... on efakturatest.ujp.gov.mk
The submission endpoint family SHALL be treated as `/JSONReceiver/sales-invoices/...` on
`efakturatest.ujp.gov.mk`, reconstructed at moderate confidence, reported by integrators.

#### Scenario: The sandbox endpoint family is exercised
- **WHEN** the sandbox is exercised
- **THEN** the endpoint family is `/JSONReceiver/sales-invoices/...` on `efakturatest.ujp.gov.mk`, at moderate
  confidence

### Requirement: the status-polling path and the production base URL are very-low-confidence guesses
The status-polling endpoint path SHALL be treated as a very-low-confidence guess by analogy with the submit
endpoint, with no direct evidence. The production base URL SHALL be treated as a very-low-confidence guess,
inferred by removing "test" from the sandbox hostname.

#### Scenario: The status-polling path is used before verification
- **WHEN** the status-polling endpoint path is used
- **THEN** it is treated as a guess by analogy with the submit endpoint, with no direct evidence behind it

#### Scenario: The production base URL is used before verification
- **WHEN** the production base URL is used
- **THEN** it is treated as inferred from the sandbox hostname by pattern, never observed

### Requirement: SubmissionStatus vocabulary is this library's own guess, and unrecognized values fall back to UNKNOWN
`SubmissionStatus`'s vocabulary and its wire spellings SHALL be treated as this library's own guess at what a
gateway of this kind reports, at very-low confidence. An unrecognized wire value SHALL fall back to `UNKNOWN` rather
than throwing.

#### Scenario: The gateway reports a status value the library does not recognize
- **WHEN** the gateway reports a `SubmissionStatus` wire value this library does not recognize
- **THEN** it falls back to `UNKNOWN` rather than throwing

### Requirement: error codes E1012, E5004, E10001–E10003 exist, but only E1012's meaning is known
Error codes E1012, E5004, E10001, E10002 and E10003 SHALL be treated as existing, reconstructed at moderate
confidence from codes observed in integrator reports. E1012 SHALL be treated as meaning the signing certificate is
not pre-registered, at moderate confidence. The specific meanings of E5004, E10001, E10002 and E10003 are
**unknown** — the codes have been seen, with no confirmed explanation of what triggers them.

#### Scenario: E1012 is returned
- **WHEN** the gateway returns error code E1012
- **THEN** it is treated as meaning the signing certificate was not pre-registered, at moderate confidence

#### Scenario: E5004 or E10001–E10003 is returned
- **WHEN** the gateway returns E5004, E10001, E10002 or E10003
- **THEN** the code's existence is treated as confirmed, but its specific meaning is unknown

### Requirement: cert-based auth needs a KIBS- or Telekom-issued qualified certificate, pre-registered at eujptest.ujp.gov.mk/ureg
Certificate-based authentication SHALL be treated as requiring a KIBS- or Telekom-issued qualified certificate,
pre-registered at `eujptest.ujp.gov.mk/ureg`, reconstructed at moderate confidence, reported by integrators.

#### Scenario: A certificate is used without portal registration
- **WHEN** a signing certificate has not been registered at `eujptest.ujp.gov.mk/ureg`
- **THEN** submission is expected to fail, most likely with E1012

### Requirement: MK VAT rates are verified public tax law, independent of the UJP wire format
MK VAT rates — 18% standard, 10% and 5% reduced, 0% zero-rated, plus an exempt category — SHALL be treated as
**verified**: public tax law, independent of the UJP wire format.

#### Scenario: A VAT rate is read
- **WHEN** any of `VatCategory`'s rates (18%, 10%, 5%, 0%, or exempt) is read
- **THEN** it is treated as verified public tax law, not as a UJP-specific reconstruction

### Requirement: ЕДБ as the party identifier is a verified concept, with its format deliberately unenforced
The ЕДБ as the party identifier SHALL be treated as a **verified concept, format not enforced**: presence is a
verified public identifier requirement on a business, and its exact format is deliberately left unchecked.

#### Scenario: A taxId of an unexpected format is supplied
- **WHEN** a business `Party`'s `taxId` does not match an expected digit format
- **THEN** it is accepted regardless — the concept of the ЕДБ is verified, but its format is deliberately unchecked

### Requirement: every constant in the field-name inventory is marked @ProvisionalSpec
Every constant in `UjpJsonSerializer`'s field-name inventory SHALL be marked `@ProvisionalSpec`.

#### Scenario: A new field-name constant is added to UjpJsonSerializer
- **WHEN** a new field-name constant is added to `UjpJsonSerializer`
- **THEN** it carries the `@ProvisionalSpec` marker

### Requirement: some field names carry no evidence at all, and are this library's own naming
`FIELD_DOCUMENT_TYPE` (wire name `documentType`), `DOCUMENT_TYPE_CODES` (`"CREDIT_NOTE"`, `"DEBIT_NOTE"`),
`FIELD_CORRECTED_INVOICE` (`correctedInvoice`), `FIELD_REFERENCE_NUMBER` (`number`), `FIELD_REFERENCE_ISSUE_DATE`
(`issueDate`) and `FIELD_LINE_UNIT` (`unit`) SHALL be treated as this library's own naming, with no evidence at all
in any integrator account — first to correct once the spec can be read.

How UJP distinguishes an invoice from a note is undescribed anywhere; an `INVOICE` writes no type field at all.
`FIELD_REFERENCE_ISSUE_DATE`'s ISO-8601 format is assumed. Nothing says UJP expects a unit at all. The choice to
omit an absent field rather than writing `""` or `null` is likewise this library's own, with the gateway's
tolerance for either unverified.

#### Scenario: The document-type wire representation is queried
- **WHEN** `FIELD_DOCUMENT_TYPE`, `DOCUMENT_TYPE_CODES`, `FIELD_CORRECTED_INVOICE`, `FIELD_REFERENCE_NUMBER` or
  `FIELD_REFERENCE_ISSUE_DATE` is relied upon
- **THEN** it is treated as this library's own naming, backed by no integrator evidence at all

### Requirement: the remaining field names are reconstructed with integrator-account backing
The remaining wire field names SHALL be treated as reconstructed (not verified), backed by integrator accounts:
top level — `invoiceNumber`, `issueDate`, `dueDate`, `currency`, `seller`, `buyer`, `lineItems`, `totals`; Party —
`name`, `taxId`, `address`; Address — `street`, `city`, `postalCode`, `country`; line item — `description`,
`quantity`, `unit`, `unitPrice`, `vatCategory`, `netAmount`, `vatAmount`, `grossAmount`; Totals — `byCategory`,
`netTotal`, `vatTotal`, `grossTotal`; category subtotal — `category`, `net`, `vat`, `gross`.

#### Scenario: A top-level or nested field name is relied upon
- **WHEN** any of these field names is relied upon by a consumer
- **THEN** it is treated as reconstructed from integrator accounts, not verified against the official spec
