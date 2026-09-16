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

### Requirement: UjpException names the error codes with a reported meaning and those without one
`UjpException` SHALL name `E1012_CERTIFICATE_NOT_REGISTERED` (the only one with a reported meaning), `E5004`,
`E10001`, `E10002` and `E10003`.

#### Scenario: The gateway returns E1012
- **WHEN** the gateway returns error code E1012
- **THEN** `UjpException.errorCode()` is `E1012_CERTIFICATE_NOT_REGISTERED`
