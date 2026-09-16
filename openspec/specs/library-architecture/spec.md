# Library architecture

## Purpose

The library is kept as four genuinely independent layers so that a corrected wire format, or an entirely different
one, touches as little of the codebase as possible. This capability records the layers themselves, what each one
depends on, and the baseline commitments — zero runtime dependencies and a single rounding convention — that hold
across all of them.

## Requirements

### Requirement: model depends on nothing and does not know JSON exists
The `model` package SHALL hold `Invoice`, `Party`, `Address`, `LineItem`, `VatCategory`, `Totals`, `CategoryTotal`,
`DocumentType`, `DocumentReference` and `InvoiceValidationException`, and SHALL depend on nothing else in the
library.

#### Scenario: model has no serialization dependency
- **WHEN** the `model` package is inspected
- **THEN** it depends on nothing else in the library and does not know JSON exists

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
`UjpException` and `UjpTransportException`, and SHALL depend on `Serializer` and `Signer` as interfaces, never
directly on `UjpJsonSerializer` or `JwsRs256Signer`.

#### Scenario: transport is composed with a Serializer and a Signer, not a concrete class
- **WHEN** `UjpClient` is built
- **THEN** it takes a `Serializer` and a `Signer` by interface, never `UjpJsonSerializer` or `JwsRs256Signer`
  directly

### Requirement: internal holds the JSON writer and the single rounding convention, and depends on nothing
The `internal` package SHALL hold `Money` (the single rounding convention) and `internal.json` (the minimal JSON
reader/writer), and SHALL depend on nothing else in the library.

#### Scenario: internal has no dependency on the other layers
- **WHEN** the `internal` package is inspected
- **THEN** it depends on nothing else in the library

### Requirement: a corrected wire format is a new Serializer implementation, never a change to model
A corrected schema, or an entirely different format (UBL 2.1, say), SHALL be implemented as a new `Serializer`
implementation, never as a change to `model`.

A change to fix the wire format that has to touch `model` is a sign the abstraction leaked — stop and reconsider
before proceeding.

#### Scenario: A wire-format correction touches model
- **WHEN** a change intended to fix the wire format needs to touch `model`
- **THEN** that is a sign the abstraction leaked, and the change should stop and reconsider before proceeding

### Requirement: the library ships with zero runtime dependencies, compiled with --release 25
`net.aetherealtech:ujp-einvoice` SHALL ship with zero runtime dependencies, compiled with `--release 25`.

Consumers need JDK 25+, which is deliberate, not an oversight to work around.

#### Scenario: A consumer resolves the library
- **WHEN** a consumer adds `net.aetherealtech:ujp-einvoice` as a dependency
- **THEN** no transitive runtime dependency is pulled in, and JDK 25 or newer is required to use it

### Requirement: money math has a single rounding convention, through internal.Money.round
All money math SHALL use `BigDecimal`, `RoundingMode.HALF_UP`, two decimal places, exclusively through
`internal.Money.round`.

#### Scenario: An amount is rounded
- **WHEN** any amount in the library is rounded
- **THEN** it goes through `internal.Money.round`, using `BigDecimal`, `RoundingMode.HALF_UP`, at two decimal places
