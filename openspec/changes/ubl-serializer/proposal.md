## Why

The `serialization` layer exists so a corrected schema, or an entirely different format, is a new `Serializer`
implementation rather than a change to `model`. Current evidence says UJP does not accept UBL 2.1 / XAdES — the
generic compliance sites describing UBL for North Macedonia appear to be describing the EU norm rather than this
endpoint — so there is nothing to build yet.

## What Changes

Add a second `Serializer` implementation for UBL 2.1 / XAdES, if and when evidence shows UJP accepts it.

## Capabilities

### New Capabilities

None yet.

### Modified Capabilities

None yet.

## Impact

A new class in `serialization`, implementing the existing `Serializer` interface. `model` is unaffected by
construction — see `library-architecture`.

## Status

**Tag.** PARKED.

**Why it waits.** Current evidence says UJP does not accept anything other than the proprietary JSON shape.

**Reopens when** evidence shows UJP accepts UBL 2.1 / XAdES (or another format) on this endpoint.

**Prerequisites (owner).** Library maintainers.
