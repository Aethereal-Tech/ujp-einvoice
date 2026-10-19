## Context

0.2.0's wire layer is known-wrong: `specs/specification-status/comparison-2026-09-13.md` found it would be rejected at
the gateway's first validation step. `specs/specification-status/comparison-2026-10-19.md` re-read the documentation
five weeks later. The wire contract it describes is unchanged: the envelope, the four headers, the nested `document`,
`current-status`, docType 100/110/120, `docStorno` 0/1/2 and the four VAT groups. Around that contract a good deal was
added or published that the step list in `proposal.md` did not know about.

The main addition is a **per-document-type field matrix**. Each type page carries every field with its type, length
and a `З/О/Ш/П` marker (mandatory / optional / code / name). The 09-13 pass named the missing matrix as one of the two
things the work had to wait for, and it is now in hand. The other, the gateway's machine-readable schema, is still
not on the documentation host. The 08.10 changelog promises a `swagger.json` download, but the only Swagger URL is on
the API host, which nobody in this project calls.

Two consumers bound what this release must do:

- **kapar.net** pins 0.2.0 and generates ordinary invoices only.
- **Invicta** is not wired to the library at all. Its e-invoicing change stays parked until this release exists and
  the owner holds a certificate.

Neither files anything with 0.2.0, which is wrong anyway. Nothing worth preserving depends on its shape.

Nothing in this plan calls the gateway. **`UjpClient` is never called for a real submission until the specification
is verified, a qualified certificate is registered, and the privileges are granted.** That rule stays exactly as
written, here and in every consumer.

## Goals / Non-Goals

**Goals.**

- A document model, serializer, signer and transport that produce what D1, the type pages and the 08.10 examples
  describe, for the document kinds the consumers file.
- Every field cited to its source, and every remaining guess still marked `@ProvisionalSpec`.
- A release that a consumer can generate with today and submit with once the external preconditions are met.

**Non-Goals.**

- **The purchase side** (accept, reject, payload list, VAT-deduction totals). No consumer receives supplier invoices
  through this library. It becomes its own change if one does.
- **The 23 document types beyond the scoped set**, such as 200 import, 220 internal calculation, 900 dispatch note
  and 999 linked documents. The model must not make them impossible, but none ships with rules or goldens.
- **Production.** No production base URL is published, so `PRODUCTION_BASE_URL` keeps its marker. The library
  ships no default that points anywhere real.
- **UBL 2.1 / XAdES.** It stays rejected first-hand, so `ubl-serializer` stays parked.

## Decisions

### 1. Version: 1.0.0 off an integration branch, with one breaking merge (owner, 2026-10-19)

`publish.yml` mints a release on every push to `master`, bumped by the commit types since the last tag. Landing this
work piecemeal on `master` would publish a string of half-corrected versions, and the first of them would be the
`0.3.0` the proposal rules out.

The work therefore lands on a long-lived branch, `next/1.0`, through pull requests. It reaches `master` once, with
`BREAKING CHANGE` in the merge body, so the workflow cuts **1.0.0**. `pr-check.yml` only runs for pull requests into
`master`, so its `branches:` must gain `next/**` first. That is a `ci:` commit, which mints nothing.

**Alternative: a parallel package beside 0.2.0's.** This would keep `model.Invoice` and the 0.2.0 serializer alive
next to the corrected shape. It protects nobody: no consumer files with 0.2.0, and what 0.2.0 produces is rejected.
It would double the public surface for a shape that must never be used. Rejected unless the owner wants kapar's
import paths frozen.

**Alternative: stay on 0.x.** The workflow's own seeding comment says 1.0.0 must not claim more than is verified.
That argues for **holding** the merge, not renumbering it. `next/1.0` reaches `master` only after the schema check
(decision 9) passes. Until then nothing is published, and kapar keeps 0.2.0 for generation, as today.

### 2. `model` follows UJP's document, and the architecture rule is rewritten rather than worked around

The 09-13 step list asked whether to extend `Invoice` or to introduce a UJP-shaped model beside it.
`library-architecture` says a wire fix that reaches into `model` means the abstraction leaked. It did leak, and for a
reason the rule did not foresee: UJP's JSON is the only format this library will ever write. The abstraction protected
a second format that was rejected on first-hand evidence.

**Decision.** `model` is replaced, in the same package, by a document whose concepts are UJP's own: a header, typed
references, seller and buyer, payment, lines, document totals, and VAT totals by group and by indicator.

- It is written in domain names. Wire names such as `docItemUnitOriginalPriceWoVat` stay in `serialization`, so the
  dependency direction holds and `model` still does not know JSON exists.
- Each document kind is a `DocumentKind` value: its code, which fields it requires, and which reference type it
  requires. These are built from the type pages.
- Validation refuses a missing mandatory field **by name**, before anything is signed.

`library-architecture` is amended in the same change, so it no longer forbids the model from following the only
format there is. What it still forbids is a wire name or a JSON type in `model`.

**Consequence for `product-boundaries`.** "Requiring an address from a natural-person buyer" was cut on the ground
that no spec section required it. Type 170 now marks every part of `buyerAddress` mandatory. The boundary is
therefore re-opened **for kind 170 only**, and its text records why. On every other kind the cut stands.

"Signed (negative) amounts on a credit note" stands as written: 110 and 120 keep positive amounts. A storno is not a
credit note (decision 5).

### 3. Money: one rounding mode, scale per layer, and the gateway's tolerance is never relied on

The guide on `/json-struktura` states the rule plainly:

- Line values are **not rounded**, and are carried to at most 4 decimals.
- They are summed unrounded into the VAT totals and the document totals, which are then rounded to **2 decimals,
  `ROUND_HALF_UP`**.
- `UJP08-08`, `UJP08-09` and `UJP08-10` are whole numbers.
- The gateway tolerates 0.1 of rounding difference.

The 28.04 changelog calls `docGrossAmountR` "банкарско заокружување" (banking rounding). The guide is newer,
explicit, and says "обично математичко заокружување" (ordinary mathematical rounding). Follow the guide.
`docGrossAmountR`'s rounding stays `@ProvisionalSpec` until the schema check, or a golden whose numbers would differ
under half-even, settles it.

This replaces two current rules:

- **`invoice-model`**, "a line rounds its net amount first, then computes VAT from the rounded net".
- **The library's CLAUDE.md money rule**, two decimals, half-up, one convention. It becomes one `RoundingMode`
  (`HALF_UP`) applied at the scale each layer is defined to carry, through `internal.Money`.

The CLAUDE.md wording is the owner's to approve. The 0.1 tolerance is the gateway's slack: the library computes
exactly and never leans on it.

### 4. VAT groups and tax indicators are two types, and the indicator catalogue is data with a date

`VatCategory` becomes `VatGroup`: `DDV-A` 18, `DDV-V` 10, `DDV-B` 5, `DDV-G` 0.

The tax indicator becomes a value type: code, `vatImpact` (`STANDARD`/`NULA`/`OSLOBODEN`/`PRENESEN`), `currencyType`
and `vatType`. Its note is required wherever the gateway raises `E1032`.

About 104 codes exist, and three releases added more between July and October. A closed enum would go stale with the
next changelog entry. Instead, a `TaxIndicators` catalogue ships as a dated snapshot of `/danocni-indikatori`. A
caller may still name an indicator outside the snapshot, by stating its `vatImpact`. The calculation is driven by
`vatImpact`, not by the code.

### 5. Storno and correction are a second axis, as the gateway has them

`docStorno` is 0 (ordinary), 1 (storno: negative amounts plus `voidReason`) or 2 (correction: a replacement document
plus `correctionReason`). It is independent of the document kind. Both modes reference the original document with
`docTypeRef: "ST"`.

The model has a `Storno` and a `Correction` that each wrap the original's reference and the reason object. A storno
is the one place amounts are negative. Statuses 12 Сторно and 13 Корективна are the gateway naming those documents,
and they arrive through the served status pair (decision 8).

### 6. Absent fields: write `null`, write `[]`, and emit nothing undocumented

The examples write `null` for every absent scalar and object, and `[]` for an empty list. Follow them. The guide's one
explicit rule ("празно или не се прати", empty or not sent, counts as 0) covers amounts and agrees with this choice.
The choice stays `@ProvisionalSpec` until the schema check.

Members that appear in an example but in no field table are **not emitted**: `source`, a trailing
`requestTimestamp`, `sellerEinvoiceUser` / `buyerEinvoiceUser`, `docGlobalDiscountAmount` and
`docItemUnitItemDiscountAmount`. They are recorded as open questions for the schema.

`docAlt` is Boolean in the changelog, Integer(4) in the table and `false` in the example. It stays provisional, and
the schema decides its type.

An invoice-level discount is spread over the lines and sent per line, as the 15.10 entry says, so the model takes it
as a per-line amount. `docAttributes` and line `attributes` (`{name, value, type}`, typed `STR`/`DEC`/`INT`/`DATE`)
are modelled but have no example yet. `docCurrencyDate` and `docCurrencyExchRate` were removed from type 100 on 15.10,
although the 08.10 examples still carry them. The newer changelog entry wins.

### 7. Signing: RS256 is now cited; `x5c` is not

The FAQ (`/ЧПП`) gives the header as `{"alg":"RS256","typ":"JWT"}`. Its marker goes, and `typ` is added. The
gateway identifies the certificate by the registered certificate plus `X-SERIAL-NUMBER` (`E5012`), and no document
mentions `x5c`. Its presence stays `@ProvisionalSpec`.

Certificates may come from **any qualified trust-service provider** in North Macedonia (D6, FAQ), held in software or
on a USB token. "KIBS- or Telekom-issued" in `submission-going-live` is corrected to that wording.

### 8. Transport and status

Steps 7–9 of the proposal stand:

- The send path is `/JSONReceiver/api/v1/sales-invoices/send`, carrying the envelope `{requestTimestamp, jws}`.
- `X-EUJP-ID` and `X-EDB` go on every call, `X-SERIAL-NUMBER` on every call carrying a JWS, and `X-DOC-TYPE-CODE` on
  send.
- `requestTimestamp` comes from `GET /api/v1/server-time` (Skopje time, ±5 minutes).
- The client throttles to one request per second per user.
- Status uses `POST /api/v1/documents/sales-invoice/current-status` and its history call.

**Status.** It is the gateway's served code and name pair, not a closed enum. The ten known codes (00, 01, 03, 04,
05, 07, 09, 10, 12, 13) ship as named constants for convenience. An unknown code passes through.

**The submit response** has `euid`, `message`, `qr_link`, `status` (an integer), `e_invoice_user` and `timestamp`.
The error body is still unpublished: the raw body is always kept, and anything parsed from it stays provisional.

### 9. Three gates, and only the first is ours

1. **Documents.** Goldens are derived from the 08.10 examples, one per scoped kind and mode, with numbers recomputed by
   the model and signatures checked by `scripts/verify-jws.py`. This gate is built and passing on `next/1.0` before
   anything else.
2. **Schema.** A person fetches `swagger.json` from the Swagger UI on the API host. Its JSON Schema is committed under
   `src/test/resources`, and every golden is validated against it in a test.
   - A test-scoped validator is allowed; zero *runtime* dependencies is unaffected.
   - Optionality markers and the null-versus-omit choice are removed only here.
   - **`next/1.0` merges to `master` only after this gate.**
3. **Sandbox.** This needs a qualified certificate, the `eujptest.ujp.gov.mk/ureg` registration and granted
   privileges, all the owner's. It is evidence, not citation, per this library's CLAUDE.md. It is the last
   precondition before any consumer lifts its `UjpClient` ban.

This splits what `proposal.md` "Why it waits" fused. The schema no longer gates **starting**, because the type pages
now give everything needed to build. It gates **publishing**.

## Risks / Trade-offs

- **The documentation moves weekly.** Fifteen changelog entries since July have added fields, types and indicators.
  Mitigation: every snapshot (indicators, error catalogue, kinds) carries its retrieval date. The step list is re-run
  against `/известувања` before the merge, and anything newer gets its own comparison file.
- **The type pages contradict their own examples.** `docReferences` is mandatory on 110 but the example sends `[]`.
  Kind 170 prints its code as "????", and the `ЗШ*` asterisk has no footnote. Mitigation: the table wins over the
  example where they disagree, the conflict is recorded, and the field keeps its marker until the schema check.
- **Kind 170 (natural-person buyer) and 310 (cash turnover) carry no example.** Mitigation: they are not in scope
  unless the owner puts them there (open questions).
- **A rewrite this broad hides regressions in the money math.** Mitigation: the goldens recompute the examples'
  printed totals, so a rounding slip shows as a byte difference against UJP's own numbers, not against our earlier
  output.
- **The TLS leaf on the documentation host expires 2026-12-13, and the chain is still incomplete.** Mitigation: use
  `curl`, as recorded. A failed fetch is not evidence the documentation is gone.

## Migration Plan

- **kapar.net** stays on 0.2.0 for generation until 1.0.0 is published, then moves in one step. The comparison's
  "BREAKING for a consumer pinned to 0.2.0" section is its checklist.
- **Invicta** consumes 1.0.0 when its own `ujp-e-invoicing` change reopens.
- 0.1.0 and 0.2.0 stay resolvable.
- **Rollback.** Do not merge `next/1.0`. Nothing reaches `master` before gate 2.

## Open Questions

Answered by the owner on 2026-10-19:

- **Version.** 1.0.0 off `next/1.0` (decision 1).
- **Scope.** In 1.0, beyond 100, storno, correction, 110 and 120: **130** (advance invoice) and **310** (cash
  turnover). **170** (invoice to a natural person) is not in 1.0.
- 310 is in scope although the draft law excludes sales covered by a fiscal receipt. Whether a given Invicta cash
  invoice is filed as 310 is the consumer's decision, not the library's.

Still open:

1. The CLAUDE.md money rule's new wording (decision 3). This needs the owner's approval.
2. Who fetches `swagger.json`, and when. This is a person's task, on the API host.
3. For UJP, should the owner choose to ask: the `ЗШ*` asterisk, `docAlt`'s type, the error body shape and the
   production base URL.
4. **Order of work.** The owner put Invicta's advance and partial invoices first (2026-10-19). The two meet at kind
   130: what Invicta's advance invoice states should be shaped so it maps onto 130 without a second redesign.
