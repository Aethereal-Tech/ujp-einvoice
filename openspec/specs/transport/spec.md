# Transport

## Purpose

`UjpClient` composes a `Serializer` and a `Signer` to submit an invoice and poll its status over
`java.net.http`. Every endpoint constant is provisional — see `specification-status` for the confidence behind the
wire shape it carries.

## Requirements

### Requirement: UjpClient.builder() takes a base URL, a Serializer, a Signer and an optional HttpClient
`UjpClient.builder()` SHALL take `baseUrl` (default `UjpEndpoints.TEST_BASE_URL`), a `Serializer`, a `Signer`, and
optionally an `HttpClient`.

#### Scenario: A client is built with no explicit base URL
- **WHEN** `UjpClient.builder()` is used with no `baseUrl` supplied
- **THEN** it defaults to `UjpEndpoints.TEST_BASE_URL`

### Requirement: submit and status return a SubmissionResult
`UjpClient.submit(Invoice)` and `UjpClient.status(String euid)` SHALL return `SubmissionResult(euid, qrLink,
status, message)`.

#### Scenario: A submission succeeds
- **WHEN** `UjpClient.submit(Invoice)` completes successfully
- **THEN** it returns a `SubmissionResult` carrying `euid`, `qrLink`, `status` and `message`

### Requirement: a gateway refusal is UjpException; a failed request is UjpTransportException
A gateway refusal SHALL be reported as `UjpException` (`errorCode()` may be null); a request that never completed
SHALL be reported as `UjpTransportException`.

#### Scenario: The gateway refuses a submission
- **WHEN** the UJP gateway refuses a submitted invoice
- **THEN** `UjpClient` raises `UjpException`, whose `errorCode()` may be null

#### Scenario: The request never completes
- **WHEN** a submission or status request never completes (e.g. a network failure)
- **THEN** `UjpClient` raises `UjpTransportException`

### Requirement: every endpoint constant in UjpEndpoints is provisional
Every constant in `UjpEndpoints` SHALL be treated as provisional: `TEST_BASE_URL`
(`https://efakturatest.ujp.gov.mk`, reported by integrators), `PRODUCTION_BASE_URL`
(`https://efaktura.ujp.gov.mk`, inferred from the sandbox hostname by pattern, never observed),
`SALES_INVOICE_SEND` (`/JSONReceiver/sales-invoices/send`, reported by integrators), and
`SALES_INVOICE_STATUS_TEMPLATE` (`/JSONReceiver/sales-invoices/status/%s`, guessed by analogy — a guess about a
guess).

#### Scenario: A constant from UjpEndpoints is relied upon
- **WHEN** any constant from `UjpEndpoints` is relied upon before the verification pass lands
- **THEN** it is treated as provisional at the confidence named above, not as a confirmed endpoint

### Requirement: UjpException carries the code with its namespace and resolves it against the catalogue
`UjpException` SHALL carry `httpStatus()`, `errorCode()`, `namespace()` (`null` when unknown), `responseBody()` (may
be `null`) and `known()`, which resolves the namespace and code against `UjpErrorCodes`. It SHALL NOT name codes as
constants. The catalogue and its provenance are recorded in `specification-status`.

#### Scenario: The gateway returns a listed code from a known interface
- **WHEN** an exception carries namespace `JSON_RECEIVER` and code `E1020`
- **THEN** `known()` is the catalogue entry whose meaning is "Invalid calculated totals", and the same code under
  `EINVOICE_API` resolves to a different meaning

#### Scenario: The code is not listed, or the interface is not known
- **WHEN** an exception carries a code the catalogue does not list, or no namespace
- **THEN** `known()` is empty, and `errorCode()` and `responseBody()` are unchanged
