# CLAUDE.md

Conventions for working in this repository.

**The record of what exists is `openspec/`** — `specs/<capability>/spec.md` holds the model surface, the
specification-status inventory and every other built fact with its reasoning; `changes/<name>/` the planned work,
why it waits and who owns its prerequisite; this file is the rules alone. A new rule or invariant is written into
its spec in the same commit as the code, and `openspec validate --all --strict` must pass before a commit — see
"Before a commit" below.

## What this is

A Java library for the North Macedonian UJP e-Faktura e-invoicing gateway. The published 0.2.0's wire
layer is **known-wrong, not merely unverified** — it was compared against the official specification on
2026-09-13 and would be rejected at the gateway's first validation step, so **0.2.0 must not be used to
file**. Read `openspec/specs/specification-status/spec.md` before touching `serialization/`,
`transport/`, or anything marked `@ProvisionalSpec`.

## The one hard rule

**Nothing marked `@ProvisionalSpec`, and no endpoint in `UjpEndpoints`, may be promoted to verified
without citing the specific section of the official spec (efakturawiki.ujp.gov.mk's
"API Спецификација") that confirms it.** A sandbox account behaving as expected is evidence, not
confirmation — integrators' own reconstructions have been self-consistent and still wrong before.
"I tested it against efakturatest and it worked" is not a citation. The specification has now been
obtained, so citing it is possible: cite the section, update the field/endpoint, remove its
`@ProvisionalSpec` marker, and update `openspec/specs/specification-status/spec.md` in the same change.

## Architecture

Three layers, kept genuinely independent:

1. **`model`** — the `Invoice` domain model. Serialization-agnostic: it does not know JSON exists.
2. **`serialization`** — `Serializer` interface + `UjpJsonSerializer`, the one shipped implementation
   of the reconstructed wire shape. A corrected schema, or an entirely different format (UBL 2.1,
   say), is a new `Serializer` implementation, not a change to `model`.
3. **`signing`** / **`transport`** — JWS signing and the HTTP client. Both depend on `Serializer` and
   `Signer` as interfaces, never on `UjpJsonSerializer` or `JwsRs256Signer` directly.

If a change to fix the wire format touches `model`, that is a sign the abstraction leaked — stop and
reconsider before proceeding.

## Conventions

- **Java 25**, compiled with `--release 25`. Consumers need JDK 25+; this is deliberate, not an
  oversight to work around.
- **Zero runtime dependencies.** See the comment above `<dependencies>` in `pom.xml` before adding
  one — the bar is "this is genuinely more than a few hundred lines of well-scoped JDK code", not
  "a library would be more convenient."
- **Money math**: `BigDecimal`, `RoundingMode.HALF_UP`, two decimal places, via
  `internal.Money.round`. Never introduce a second rounding convention.
- **Comments explain why, not what.** Don't narrate code that already reads clearly; do explain a
  non-obvious constraint, a rejected alternative, or a subtlety a future reader would otherwise have
  to re-derive.
- **Conventional commits**, no AI attribution in commit messages or trailers.
- **JaCoCo**: `model`, `serialization`, and `signing` are gated at 90% line coverage (see `pom.xml` —
  note the JaCoCo class-pattern syntax uses `/` separators, not `.`; a dotted pattern matches nothing
  and passes vacuously, which is exactly the bug this project shipped once and caught by hand).
  `transport` is proven through WireMock instead, not chased to the same number.
  `signing.Pkcs11KeyStores` is excluded from the gate — it needs a physical hardware token no CI
  runner has; keep it that way rather than deleting the exclusion to force coverage up.

## Before a commit

```bash
./mvnw clean verify
openspec validate --all --strict
```

Both must pass. A new rule or invariant discovered while making a change belongs in the relevant
`openspec/specs/<capability>/spec.md` in the same commit as the code, not left for later.

## Before publishing a release

`publish.yml` derives the version from conventional commit types since the last tag. A commit that
should not cut a release (docs, ci, chore, test, style with no accompanying fix/feat) should be typed
accordingly — the workflow trusts the commit type, not a judgment call at merge time.

**A merge is not finished until the branch is gone and the issue is closed** — delete the merged
branch, local and remote, in the same step as the merge, and close every GitHub issue it resolved,
naming the pull request or commit in the comment. Merged branches only: a parked branch stays until
its work lands. An outliving branch gets built on by mistake; an outliving issue gets planned twice.
