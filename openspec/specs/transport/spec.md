# Transport

## Purpose

`UjpClient` composes a `Serializer` and a `Signer` to submit a `Document` and poll its status over `java.net.http`,
with the library's own JSON and no runtime dependency. It is built from D1 („API Спецификација") and the gateway's
published description of `einvoice_api` (`specification-status/openapi-2026-10-20.md`), each endpoint cited to the
section that states it; what no document states stays `@ProvisionalSpec` and is listed in `specification-status`.

**The `UjpClient` ban is unchanged.** It is never called for a real submission until the specification is verified,
a qualified certificate is registered and its privileges are granted. Nothing in this library or its tests contacts
a UJP host: the tests run against WireMock on localhost.

## Requirements

### Requirement: UjpClient.builder() requires a base URL, an identity, a Serializer and a Signer, and defaults to no host
`UjpClient.builder()` SHALL require `baseUrl`, `eujpId`, `edb`, `certificateSerialNumber`, a `Serializer` and a
`Signer`, with no default for any of them, and `build()` SHALL refuse a missing one by name. An `HttpClient` is
optional. A blank `eujpId`, `edb` or `certificateSerialNumber`, or a null `baseUrl` or `HttpClient`, SHALL be refused
when set.

The base URL is the host alone: the endpoints carry their own `/einvoice_api` or `/JSONReceiver` root.
`UjpEndpoints.TEST_BASE_URL` is a constant a caller passes deliberately; `PRODUCTION_BASE_URL` is `@ProvisionalSpec`
(no obtained document names a production host) and is used by nothing. The library ships no default that points
anywhere real.

The three identifiers go on the wire as `X-EUJP-ID` (the signer's e-УЈП id), `X-EDB` (the company's ЕДБ) and
`X-SERIAL-NUMBER` (the certificate's serial number), cited to D1 „API Documentation". The serial number's format is
stated nowhere and is sent as given.

#### Scenario: A client is built with no base URL
- **WHEN** `UjpClient.builder()` is built without `baseUrl`
- **THEN** `build()` raises `IllegalStateException` naming `baseUrl`, rather than defaulting to the sandbox

#### Scenario: A client is built without an identity
- **WHEN** `eujpId`, `edb` or `certificateSerialNumber` is missing
- **THEN** `build()` raises `IllegalStateException` naming it

### Requirement: a call reads the gateway's clock first, and caches nothing
Every call SHALL first `GET /einvoice_api/api/v1/server-time` and use the answer, truncated to whole seconds, as
`requestTimestamp`. The answer SHALL NOT be cached across calls, and the local clock SHALL NOT stand in for it.

D1 „Време на сервер" gives the service and the answer `{"timestamp": "2025-11-19T10:45:51"}`; D1 „Валидација на
requestTimestamp" gives the format `2026-01-05T12:00:00`, Skopje time, and the ±5 minutes (`E40122`). D1 states no
validity period for the answer, so none is assumed. The read carries `X-EUJP-ID` and `X-EDB` and no `X-SERIAL-NUMBER`:
D1 requires the first two on all services and the third only where something is signed, though the API description
declares no header on that operation.

#### Scenario: Two calls are made
- **WHEN** two calls follow each other
- **THEN** each reads the server time, and the timestamp each signs is the one it read

#### Scenario: The server time cannot be read
- **WHEN** the server-time read is refused, is not JSON, or its `timestamp` is not a timestamp
- **THEN** nothing is signed or sent, and the failure is a `UjpException` or a `UjpTransportException`

### Requirement: submit posts the envelope {requestTimestamp, jws} to the JSON Receiver with four headers
`UjpClient.submit(Document)` SHALL `POST /JSONReceiver/api/v1/sales-invoices/send` with body
`{"requestTimestamp": ..., "jws": ...}` and the headers `X-EUJP-ID`, `X-EDB`, `X-SERIAL-NUMBER` and
`X-DOC-TYPE-CODE`, the last being the document kind's code. The `jws` SHALL be the compact JWS of the serialized
document.

Cited to D1 „Праќање на документ". **The timestamp is in both places, with one value.** D1 shows it in the outer body
and says it belongs inside the signed payload, where it is part of the signature and defeats replay (D1 „Валидација
на requestTimestamp"; the 28.04 changelog entry moved it inside the JWS); the `Serializer` writes the inner one and
the client writes the outer from the same server read. The request's content type is `application/json`,
`@ProvisionalSpec` for this call (no document states it), whatever `Serializer.contentType()` says of the payload.

#### Scenario: A credit note is submitted
- **WHEN** a document of kind 110 is submitted
- **THEN** `X-DOC-TYPE-CODE` is `110`, and the `jws` decodes to the serialized document, whose `requestTimestamp`
  equals the envelope's

### Requirement: a submission's answer is a SubmissionResult that reads leniently
A successful answer SHALL be returned as `SubmissionResult(euid, message, qrLink, status, eInvoiceUser, timestamp,
responseBody)`, read from `{euid, message, qr_link, status, e_invoice_user, timestamp}` (D1 „Праќање на документ").
`status` is the integer of the answer, not a document status; `timestamp` is the receipt time as an ISO-8601 instant.
A member that is missing, of another type, or (`timestamp`) not an instant SHALL read as `null`, and the raw body SHALL
be kept.

A document the gateway has accepted must never be reported as failed because a member could not be read: a failure
here invites a second submission. An answer that is not a JSON object is a `UjpTransportException` carrying the HTTP
status and the body, so the caller can see what arrived before deciding to submit again.

#### Scenario: The answer lacks members
- **WHEN** an accepted answer carries `euid` and nothing else
- **THEN** it is returned with the other members `null` and the body kept

### Requirement: status is a signed POST whose answer carries the served code and name
`UjpClient.status(String euid)` SHALL `POST /einvoice_api/api/v1/documents/sales-invoice/current-status` with the body
`{"jws": ...}`, the JWS signing `{"requestTimestamp", "euid"}`, and the headers `X-EUJP-ID`, `X-EDB` and
`X-SERIAL-NUMBER`. It SHALL return a `SubmissionStatus` read from the answer's `docStatus`.

Cited to D1 „Тековен статус на излезна фактура" and the API description's `getSalesInvoicesCurrentStatus` (headers
declared required, body `InvoiceSignedRequest`, answer `DocumentCurrentStatusResponse`). The outer body is the JWS
alone, since `InvoiceSignedRequest` has no timestamp.

`SubmissionStatus` carries `code` and `name` as served, and never narrows them to an enum: the vocabulary is a code
list the gateway serves and grows. The ten codes of the statuses page (`/status`, `updated-at` 2026-10-15) are string
constants — 00, 01, 03, 04, 05, 07, 09, 10, 12, 13 — and `isKnown()` and `is(code)` ask about them. It also carries
the other members of `InvoiceStatus`: the document number, type and dates, the five amounts, the four TINs, the
comments, and the void, reject and correction reasons as `StatusReason(code, text, comment)`. A member that is missing,
of another type, or a date that does not parse reads as `null`.

#### Scenario: A code the statuses page does not list arrives
- **WHEN** the answer's `statusCode` is `99`
- **THEN** the status carries `99` and its served name, and `isKnown()` is false

#### Scenario: The answer has no docStatus
- **WHEN** the answer is a JSON object without `docStatus`
- **THEN** the failure is a `UjpTransportException` carrying the HTTP status, the namespace and the body

### Requirement: statusHistory reads a document's status changes, with dateFrom provisional
`UjpClient.statusHistory(String euid, LocalDate dateFrom)` SHALL `POST /einvoice_api/api/v1/documents/sales-invoice/status`
with the signed payload `{"requestTimestamp", "euid", "dateFrom"}` under the same envelope and headers as `status`, and
SHALL return the answer's `statusHistory` as `List<StatusChange>`; an absent list reads as empty. Cited to D1
„Статус историја на документ" and the API description's `getStatusSalesInvoice`.

`dateFrom` is `@ProvisionalSpec`: D1 shows a date-time (`2025-12-25T13:07:30`) and the API description a date
(`2026-01-01`, "on or after this date, field lastModified"). It is written as a date, the machine-readable source, and
omitted when `null`, which no document says is allowed.

#### Scenario: A history is requested with no date
- **WHEN** `dateFrom` is `null`
- **THEN** the signed payload carries no `dateFrom`

### Requirement: requests are spaced one second apart per client instance
A client SHALL space its requests at least one second apart, counting the server-time reads, so a submit or a status
poll is two requests and at least a second long. Cited to D1 „API Documentation" („На ниво на корисник има поставено
ограничување од 1 request во секунда"; `E4007`).

The limit is the gateway's, per user; two client instances for one e-УЈП id are not throttled together, and one
instance per user is the caller's to keep. The clock and the sleeper are injectable through a package-private builder
step, so the tests run on a fake clock and never sleep for real. An interrupted wait is a `UjpTransportException` and
keeps the thread's interrupt flag.

#### Scenario: A document is submitted
- **WHEN** one `submit` runs on a fresh client
- **THEN** the send follows the server-time read by one second

#### Scenario: A second has already passed
- **WHEN** at least a second has passed since the previous request
- **THEN** the next request is not delayed

### Requirement: a gateway refusal is UjpException, with the namespace of the interface that raised it
A refusal SHALL be a `UjpException` carrying the HTTP status, the code, the namespace, the raw response body and a
message. The namespace is `EINVOICE_API` for server time, status and history, and `JSON_RECEIVER` for a send. The HTTP
status is authoritative: any status of 400 or more is a refusal, and a 2xx answer whose body carries an object-valued
`errorStatus` is one too.

The code and message are read from the body in two shapes, because the API description declares both: the
`errorStatus` of a `GenericAPIResponse` (the shape its examples show), and a bare `ErrorStatusBean` at the top level
(declared for `current-status`'s 400 and 500). A body of neither shape yields a refusal with no code, the message
"UJP gateway returned HTTP n", and the raw body.

**The JSON Receiver's error body is unpublished.** Both shapes are tried for a send, and the parse is
`@ProvisionalSpec`. A request that never completed (connection refused, timeout, interrupt) or an answer that cannot
be read (a 2xx that is not JSON, a JSON array, a missing `docStatus`) is a `UjpTransportException`, which carries the
HTTP status and body when there was a response.

#### Scenario: einvoice_api refuses with a listed code
- **WHEN** a status poll is answered 400 with `errorStatus.errorCode` `E4012`
- **THEN** the exception has namespace `EINVOICE_API`, code `E4012`, the gateway's message and the raw body, and
  `known()` resolves the catalogue entry

#### Scenario: The code is not listed
- **WHEN** the code is `E9999`
- **THEN** it is kept, and `known()` is empty

#### Scenario: The receiver answers in a shape nobody published
- **WHEN** a send is answered 502 with an HTML body
- **THEN** the exception has status 502, namespace `JSON_RECEIVER`, no code and the HTML as its body

#### Scenario: The request never completes
- **WHEN** the connection is refused
- **THEN** the failure is a `UjpTransportException` with status -1

### Requirement: the endpoints are cited, and only the production host and the send content type are not
Each constant in `UjpEndpoints` SHALL cite the section that states it: `TEST_BASE_URL` and `SALES_INVOICE_SEND` D1
„API Documentation" and „Праќање на документ"; `SERVER_TIME`, `SALES_INVOICE_CURRENT_STATUS` and
`SALES_INVOICE_STATUS_HISTORY` D1 and the API description's operations. `PRODUCTION_BASE_URL` SHALL stay
`@ProvisionalSpec`. The old `SALES_INVOICE_STATUS_TEMPLATE` is gone: it was a guess about a guess, and wrong in
verb, path root and shape.

#### Scenario: A constant is relied upon
- **WHEN** `PRODUCTION_BASE_URL` is relied upon
- **THEN** it is treated as never observed and confirmed with УЈП first
