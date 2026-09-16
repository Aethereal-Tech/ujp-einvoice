## Why

E5004, E10001, E10002 and E10003 have been seen in integrator reports, but with no confirmed explanation of what
triggers any of them — unlike E1012, whose meaning (signing certificate not pre-registered) is reported.

## What Changes

Confirm what triggers E5004, E10001, E10002 and E10003, and document each in `UjpException` and
`specification-status`, cited.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `specification-status`: the meanings of E5004, E10001, E10002 and E10003 move from unknown to confirmed.

## Impact

`UjpException`.

## Status

**Tag.** PARKED.

**Why it waits.** The codes were observed; what triggers them was not.

**Reopens when** `verification-pass-against-official-spec` lands, or sandbox observation once
`submission-going-live` gives someone an account to observe with.

**Prerequisites (owner).** Library maintainers.
