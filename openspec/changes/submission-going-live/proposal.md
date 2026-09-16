## Why

`UjpClient` composes a reconstructed wire shape with a reconstructed transport, so calling it for real against a
live UJP account is a filing with the tax authority built on unverified ground. It must not go live until the wire
shape is confirmed and until the calling organization has a certificate the gateway will accept.

## What Changes

- `UjpClient` may be called for real submissions once `verification-pass-against-official-spec` has landed.
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

**Why it waits.** Waits on `verification-pass-against-official-spec`, **plus** a KIBS- or Telekom-issued qualified
certificate and UJP portal registration of that certificate at `eujptest.ujp.gov.mk/ureg`.

**Reopens when** all three conditions above are met.

**Prerequisites (owner).** The operator, for the certificate and its registration; the maintainers, for the client
once the wire shape is confirmed.
