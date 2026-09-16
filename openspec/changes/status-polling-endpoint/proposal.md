## Why

`UjpEndpoints.SALES_INVOICE_STATUS_TEMPLATE` (`/JSONReceiver/sales-invoices/status/%s`) was derived by analogy from
the submit path, with no integrator evidence behind it — a guess about a guess.

## What Changes

The real endpoint is now known, from the 2026-09-13 comparison against the official spec (see
`openspec/specs/specification-status/comparison-2026-09-13.md`): `POST
/api/v1/documents/sales-invoice/current-status` on `einvoice_api`, with a JWS-signed body `{requestTimestamp,
euid}`. The library's current `GET /JSONReceiver/sales-invoices/status/{euid}` is wrong in verb, path root, path
shape and payload — all four change, and `UjpClient.status(String euid)` changes to build and sign that body rather
than address a path segment. This lands as part of `wire-layer-correction`, not as a change of its own.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `transport`: `SALES_INVOICE_STATUS_TEMPLATE`, `UjpClient.status`, and the request/response shape all move to the
  confirmed endpoint.
- `specification-status`: the status-polling entry moves from very-low confidence to verified.

## Impact

`UjpEndpoints`, `UjpClient`.

## Status

**Tag.** PARKED.

**Why it waits.** The real endpoint is known; what remains is `wire-layer-correction` landing and being verified,
not further evidence.

**Reopens when** `wire-layer-correction` lands.

**Prerequisites (owner).** Library maintainers.
