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

#### Scenario: A transport test needs the gateway
- **WHEN** a transport test needs a gateway
- **THEN** it is WireMock on localhost, the throttle runs on a fake clock so nothing sleeps for real, and no UJP host
  appears in a request, a link or a body; `OpenApiConformanceTest` additionally checks the `einvoice_api` side against
  the committed API description (`transport`)

### Requirement: the serializer is pinned by golden files, regenerated deliberately
`UjpJsonSerializer`'s output SHALL be pinned by golden files in `src/test/resources/golden/`, one per worked example of
UJP's 08.10 primer: `invoice-ordinary.json`, `invoice-storno.json`, `invoice-correction.json`,
`invoice-multi-item.json`, `credit-note.json`, `debit-note.json`, `advance-invoice.json` and `cash-turnover.json`. They
are this library's output and not UJP's JSON, which is not committed (see `serialization`). They SHALL be regenerated
deliberately with `-Dgolden.regenerate=true`, never by hand-editing, and the `specification-status` inventory SHALL be
updated in the same change.

#### Scenario: A field genuinely needs to change
- **WHEN** a serialized field genuinely needs to change
- **THEN** the golden file is regenerated deliberately (not hand-edited), and the specification-status inventory is
  updated in the same change

### Requirement: scripts/verify-jws.py verifies a produced JWS independently of this library's own code
`scripts/verify-jws.py` SHALL verify a produced compact JWS independently of this library's own signing code.

#### Scenario: A compact JWS is produced
- **WHEN** `JwsRs256Signer` produces a compact JWS
- **THEN** `scripts/verify-jws.py` can verify it independently of this library's own code

### Requirement: next/1.0 publishes nothing and reaches master once, with BREAKING CHANGE in the merge body
`next/1.0`, the integration branch of the wire-layer correction, SHALL publish nothing, and SHALL reach `master`
exactly once, as a merge whose body carries `BREAKING CHANGE`. Work lands on it through pull requests, and
`pr-check.yml` runs for pull requests into `master` and into `next/**`.

`publish.yml` triggers on a push to `master` alone, so nothing on `next/1.0` mints a tag, a release or a package.
Its version step reads the commit bodies since the last tag and bumps the major for `BREAKING CHANGE` (or a `!` after
a type), the minor for `feat`, the patch for `fix`, `perf` and `refactor`, and nothing for `ci`, `docs`, `chore`,
`test` and `style`. A merge whose body carries `BREAKING CHANGE` therefore cuts 1.0.0; a merge without it would cut
a minor or patch of 0.2.x over a wire layer that is incompatible with it, which the `specification-status`
capability rules out.

`next/**` in `pr-check.yml`'s `branches:` is what lets a pull request into the integration branch run the full
build; without it such a pull request runs no check at all. The merge to `master` waits for the schema gate
(`specification-status`).

#### Scenario: A pull request targets next/1.0
- **WHEN** a pull request is opened against `next/1.0`
- **THEN** `pr-check.yml` runs the same build and test suite as for `master`
- **AND** nothing is published when it merges, because `publish.yml` does not run for that branch

#### Scenario: next/1.0 is merged to master
- **WHEN** `next/1.0` reaches `master`
- **THEN** the merge body carries `BREAKING CHANGE`, so `publish.yml` derives the next major version
- **AND** a merge without it is wrong, because the derived version would be a minor or patch of the old line
