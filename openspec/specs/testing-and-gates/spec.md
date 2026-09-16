# Testing and gates

## Purpose

JUnit 5 + AssertJ, with WireMock for `transport`, run through `./mvnw clean verify` on JDK 25. This capability
records the coverage gate, its one sharp edge, the golden files that pin the serializer, and the independent JWS
verification script.

## Requirements

### Requirement: a single JaCoCo BUNDLE rule gates model, serialization and signing together at 90% line coverage
`model`, `serialization` and `signing` SHALL be gated together by one JaCoCo `BUNDLE` rule at 90% line coverage,
excluding `signing.Pkcs11KeyStores`.

#### Scenario: Coverage across model, serialization and signing falls below 90%
- **WHEN** the combined line coverage of `model`, `serialization` and `signing` (excluding
  `signing.Pkcs11KeyStores`) falls below 90%
- **THEN** `./mvnw clean verify` fails the JaCoCo gate

### Requirement: JaCoCo class patterns use / as the package separator, not .
The JaCoCo exclusion for `signing.Pkcs11KeyStores` SHALL be expressed using `/` as the package separator, not `.`.

A dotted pattern matches nothing and makes the ratio check pass vacuously — exactly the bug this project shipped
once and caught by hand.

#### Scenario: The exclusion pattern is written with dots instead of slashes
- **WHEN** the `Pkcs11KeyStores` exclusion pattern in `pom.xml` uses `.` instead of `/` as the separator
- **THEN** it matches nothing, and the coverage ratio check passes vacuously rather than actually excluding the
  class

### Requirement: signing.Pkcs11KeyStores is excluded from the coverage gate
`signing.Pkcs11KeyStores` SHALL be excluded from the JaCoCo coverage gate.

It needs a physical hardware token no CI runner has; keep it excluded rather than deleting the exclusion to force
coverage up.

#### Scenario: Pkcs11KeyStores cannot be exercised in CI
- **WHEN** CI runs `./mvnw clean verify` with no physical PKCS#11 token attached
- **THEN** `signing.Pkcs11KeyStores` is excluded from the coverage gate rather than dragging the bundle below 90%

### Requirement: transport is proven through WireMock, not chased to the 90% number
`transport` SHALL be proven through WireMock tests rather than being held to the 90% JaCoCo bundle applied to
`model`, `serialization` and `signing`.

#### Scenario: transport's coverage is measured
- **WHEN** `transport`'s test coverage is assessed
- **THEN** it is proven through WireMock tests, not chased to the same 90% figure as the other three packages

### Requirement: the serializer is pinned by golden files, regenerated deliberately
`UjpJsonSerializer`'s output SHALL be pinned by golden files in `src/test/resources/golden/`:
`simple-invoice.json`, `multi-category-invoice.json`, `credit-note.json` and `consumer-invoice.json`. They SHALL be
regenerated deliberately, never by hand-editing, and the `specification-status` inventory SHALL be updated in the
same change.

#### Scenario: A field genuinely needs to change
- **WHEN** a serialized field genuinely needs to change
- **THEN** the golden file is regenerated deliberately (not hand-edited), and the specification-status inventory is
  updated in the same change

### Requirement: scripts/verify-jws.py verifies a produced JWS independently of this library's own code
`scripts/verify-jws.py` SHALL verify a produced compact JWS independently of this library's own signing code.

#### Scenario: A compact JWS is produced
- **WHEN** `JwsRs256Signer` produces a compact JWS
- **THEN** `scripts/verify-jws.py` can verify it independently of this library's own code
