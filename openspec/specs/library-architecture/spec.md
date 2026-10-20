# Library architecture

## Purpose

The library is kept as four genuinely independent layers so that a corrected wire format, or an entirely different
one, touches as little of the codebase as possible. This capability records the layers themselves, what each one
depends on, and the baseline commitments — zero runtime dependencies and a single rounding convention — that hold
across all of them.

## Requirements

### Requirement: model follows UJP's document, names no wire field and knows no JSON
The `model` package SHALL hold `Document` and the types of UJP's document — `DocumentKind`, `Header`, `Mode`, `Reference`,
`Party`, `Address`, `Country`, `Subsidiary`, `Payment`, `Line`, `BasePrice`, `VatGroup`, `VatImpact`, `TaxIndicator`,
`TaxIndicators`, `DocumentTotals`, `VatTotal`, `Advance`, `Adjustments`, `Attribute` and the code-plus-name values — and
`DocumentValidationException`. It SHALL be written in domain names, SHALL NOT contain a wire name such as
`docItemUnitOriginalPriceWoVat` or a JSON type, and SHALL depend on nothing else in the library except `internal.Money`.

UJP's JSON is the only format this library writes (UBL 2.1 and XAdES were rejected on first-hand evidence), so a
model that follows UJP's concepts protects nothing by being abstract. The UJP code of a field appears in a refusal
message as a citation of the type page, and is not a wire name.

#### Scenario: model has no serialization dependency
- **WHEN** the `model` package is inspected
- **THEN** it holds no wire field name and no JSON type, and depends on nothing else in the library but `internal.Money`

### Requirement: serialization depends on model only
The `serialization` package SHALL hold the `Serializer` interface and `UjpJsonSerializer`, and SHALL depend on
`model` only.

#### Scenario: serialization's only dependency is model
- **WHEN** the `serialization` package is inspected
- **THEN** its only dependency within the library is `model`

### Requirement: signing depends on JDK java.security only
The `signing` package SHALL hold the `Signer` interface, `JwsRs256Signer`, `SigningCredential`, `Pkcs12KeyStores`,
`Pkcs11KeyStores`, `KeyStoreEntries` (package-private) and `SigningException`, and SHALL depend on JDK
`java.security` only.

#### Scenario: signing has no dependency on model or serialization
- **WHEN** the `signing` package is inspected
- **THEN** it depends on JDK `java.security` only, not on `model` or `serialization`

### Requirement: transport depends on Serializer and Signer as interfaces, never their implementations
The `transport` package SHALL hold `UjpClient`, `UjpEndpoints`, `SubmissionResult`, `SubmissionStatus`,
`StatusChange`, `StatusReason`, `UjpException`, `UjpTransportException`, `ErrorStatus`, `ErrorNamespace`,
`UjpErrorCode`, `UjpErrorCodes` and the package-private `Throttle`, and SHALL depend on `Serializer` and `Signer` as interfaces, never
directly on `UjpJsonSerializer` or `JwsRs256Signer`.

#### Scenario: transport is composed with a Serializer and a Signer, not a concrete class
- **WHEN** `UjpClient` is built
- **THEN** it takes a `Serializer` and a `Signer` by interface, never `UjpJsonSerializer` or `JwsRs256Signer`
  directly

### Requirement: internal holds the JSON writer and the single rounding convention, and depends on nothing
The `internal` package SHALL hold `Money` (the single rounding mode) and `internal.json` (the minimal JSON
reader/writer), and SHALL depend on nothing else in the library.

#### Scenario: internal has no dependency on the other layers
- **WHEN** the `internal` package is inspected
- **THEN** it depends on nothing else in the library

### Requirement: an entirely different wire format is a new Serializer implementation
An entirely different format (UBL 2.1, say) SHALL be implemented as a new `Serializer` implementation. A correction of
the UJP JSON that follows from a corrected schema is a change to `UjpJsonSerializer`; a change to what UJP's document
*is* — a field, a kind, a rule — is a change to `model`, with the type page cited.

This replaces the rule that a wire fix touching `model` meant the abstraction had leaked. It had: the abstraction
protected a second format that was rejected, at the cost of a model that could not hold UJP's own concepts.

#### Scenario: UBL 2.1 is wanted
- **WHEN** a second wire format is wanted
- **THEN** it is a new `Serializer` implementation over the same `Document`

#### Scenario: A type page adds a field
- **WHEN** a type page adds a field to a kind
- **THEN** the field is added to `model` with its UJP code cited, and written by `UjpJsonSerializer`

### Requirement: the library ships with zero runtime dependencies, compiled with --release 25
`net.aetherealtech:ujp-einvoice` SHALL ship with zero runtime dependencies, compiled with `--release 25`.

Consumers need JDK 25+, which is deliberate, not an oversight to work around.

#### Scenario: A consumer resolves the library
- **WHEN** a consumer adds `net.aetherealtech:ujp-einvoice` as a dependency
- **THEN** no transitive runtime dependency is pulled in, and JDK 25 or newer is required to use it

### Requirement: money math has a single rounding mode, through internal.Money
All money math SHALL use `BigDecimal` and `RoundingMode.HALF_UP`, exclusively through `internal.Money`, at the scale
each layer carries: line values unrounded up to four decimals, VAT and document totals summed unrounded and rounded to
two, and the whole-number fields to zero, as UJP's `/json-struktura` states. There SHALL be no second rounding mode.

#### Scenario: An amount is rounded
- **WHEN** any amount in the library is rounded
- **THEN** it goes through `internal.Money`, using `BigDecimal` and `RoundingMode.HALF_UP`, at the scale of the layer
  that carries it
