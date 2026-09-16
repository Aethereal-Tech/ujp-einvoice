## Why

`UjpEndpoints.SALES_INVOICE_STATUS_TEMPLATE` (`/JSONReceiver/sales-invoices/status/%s`) is derived by analogy from
the submit path, with no integrator evidence behind it — a guess about a guess.

## What Changes

Confirm the real status-polling path and update `UjpEndpoints.SALES_INVOICE_STATUS_TEMPLATE`, citing the source.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `transport`: `SALES_INVOICE_STATUS_TEMPLATE` moves from a guess about a guess to a confirmed value.
- `specification-status`: the status-polling entry moves from very-low confidence to verified.

## Impact

`UjpEndpoints`.

## Status

**Tag.** PARKED.

**Why it waits.** No direct evidence exists yet for the real path.

**Reopens when** `verification-pass-against-official-spec` lands, or a single confirmed observation of the real
path arrives sooner.

**Prerequisites (owner).** Library maintainers.
