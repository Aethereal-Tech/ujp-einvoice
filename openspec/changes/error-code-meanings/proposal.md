## Why

E5004, E10001, E10002 and E10003 had been seen in integrator reports, but with no confirmed explanation of what
triggered any of them — unlike E1012, whose meaning (signing certificate not pre-registered) was reported.

## What Changes

The meanings are now settled from the official error catalogue (D5), via the 2026-09-13 comparison (see
`openspec/specs/specification-status/comparison-2026-09-13.md`): `E5004` is certificate validation failed;
`E10001` is missing TIN during company validation; `E10002` is an incorrect company name; `E10003` is an incorrect
VAT number. `E1012`'s gloss also moves — the catalogue gives it as "certificate not found", broader than this
library's current "not pre-registered". The fact that matters most, beyond the four glosses: error codes are
namespaced per API, so the same code means something different in `eInvoiceApi`, the JSON Receiver and the Web
App — a single flat constant set is itself a modelling error. `UjpException` and `specification-status` move to the
real catalogue, namespaced per API, as part of `wire-layer-correction`.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `specification-status`: the meanings of E5004, E10001, E10002, E10003 and E1012 move from unknown or narrow to
  confirmed, and the catalogue is recorded as namespaced per API rather than flat.

## Impact

`UjpException`.

## Status

**Tag.** LANDED.

**Landed with** `wire-layer-correction` task 3 on `next/1.0`: the five flat constants are replaced by the
namespaced catalogue `UjpErrorCodes`, with `ErrorNamespace`, `UjpErrorCode`, `ErrorStatus` and `UjpException.known()`,
recorded in `specification-status` and `transport`. Not archived: it archives with `wire-layer-correction`.

**Prerequisites (owner).** None remaining.
