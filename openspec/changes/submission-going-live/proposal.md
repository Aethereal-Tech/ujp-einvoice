## Why

`UjpClient` composes a reconstructed wire shape with a reconstructed transport, so calling it for real against a
live UJP account is a filing with the tax authority built on unverified ground. It must not go live until the wire
shape is corrected and verified, and until the calling organization has a certificate the gateway will accept.

## What Changes

- `UjpClient` may be called for real submissions once `wire-layer-correction` has landed and been verified against
  the gateway. The specification is in hand now (see
  `openspec/specs/specification-status/comparison-2026-09-13.md`), but the corrected wire layer it describes is not
  built yet — reading the spec is not the same condition as this one.
- The calling organization must additionally hold a KIBS- or Telekom-issued qualified certificate, and that
  certificate must be registered at `eujptest.ujp.gov.mk/ureg`.
- Until all three conditions are in hand, consumers generate documents and do not submit them.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None yet — this proposal only records the gating condition; `transport` and `specification-status` are unchanged
until the prerequisites land.

## Impact

`UjpClient`, and every consumer currently limited to generation only.

## Status

**Tag.** Untagged — not built yet.

**Why it waits.** Waits on `wire-layer-correction` landing and being verified, **plus** a KIBS- or Telekom-issued
qualified certificate and UJP portal registration of that certificate at `eujptest.ujp.gov.mk/ureg`.

**Reopens when** all three conditions above are met.

**Prerequisites (owner).** The operator, for the certificate and its registration; the maintainers, for the client
once the wire layer is corrected and verified.
