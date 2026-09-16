## Why

Everything recorded in `specification-status` was reconstructed from public desk research and hands-on integrator
accounts, not read from the official „API Спецификација" at efakturawiki.ujp.gov.mk. That page is unreachable
outside North Macedonian networks and the Wayback Machine has no copy, so it has to be fetched from inside the
country. Every other pending change in this library — `submission-going-live`, `status-polling-endpoint`,
`error-code-meanings` and `ubl-serializer` — waits on this landing first. October 2026 is the publicly stated
target for voluntary go-live and April 2027 the expected date e-invoicing becomes mandatory for VAT payers, with
the enabling law possibly still in draft; treat both as planning assumptions, not confirmed legal deadlines, but
they are what sets the urgency here.

## What Changes

- Fetch the official „API Спецификација" from a North Macedonian network.
- For each field, endpoint and behaviour it confirms: cite the specific section, correct the field or endpoint if
  the reconstruction was wrong, remove its `@ProvisionalSpec` marker, and update `specification-status` in the same
  change.
- If the specification defines document types beyond `CREDIT_NOTE` and `DEBIT_NOTE` — for example a storno /
  cancellation document — add them to `DocumentType` and `UjpJsonSerializer` at that point, cited the same way.
- A sandbox account behaving as expected remains evidence, not confirmation, throughout — see the hard rule in
  `specification-status`.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `specification-status`: entries move from reconstructed to verified, cited section by section, as the spec is
  read.
- `serialization`, `transport`, `invoice-model`: whichever field, endpoint or document type the specification
  corrects or adds.

## Impact

`ProvisionalSpec.java`, `UjpJsonSerializer`, `UjpEndpoints`, and any `model` type the specification turns out to
require beyond what is built (see the note on additional document types above).

## Status

**Tag.** Untagged — not built yet, and everything downstream depends on it.

**Why it waits.** The specification is reachable only from a North Macedonian network, and nobody who has built
this library has read it.

**Reopens when** the official specification has been fetched.

**Prerequisites (owner).** The operator — fetched from a North Macedonian network.
