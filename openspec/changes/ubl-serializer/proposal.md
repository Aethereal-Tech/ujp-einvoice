## Why

The `serialization` layer exists so a corrected schema, or an entirely different format, is a new `Serializer`
implementation rather than a change to `model`. Evidence had said UJP does not accept UBL 2.1 / XAdES — the generic
compliance sites describing UBL for North Macedonia appeared to be describing the EU norm rather than this
endpoint — so there was nothing to build yet.

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

**Why it waits.** The 2026-09-13 comparison against the official documents (see
`openspec/specs/specification-status/comparison-2026-09-13.md`) confirms directly, not by inference from generic
compliance sites, that UJP's wire format is a proprietary JSON schema signed as compact JWS — UBL 2.1 / XAdES is not
accepted on this endpoint. The reason this stays parked is now first-hand rather than inferred.

**Reopens when** evidence shows UJP accepts UBL 2.1 / XAdES (or another format) on this endpoint.

**Prerequisites (owner).** Library maintainers.
