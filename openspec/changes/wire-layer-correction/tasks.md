## 0. Owner decisions and inputs (blocking where marked)

- [x] 0.1 Decide the version: 1.0.0 off `next/1.0` (owner, 2026-10-19).
- [x] 0.2 Decide scope: 130 and 310 in 1.0, 170 out (owner, 2026-10-19).
- [x] 0.3 Approve the CLAUDE.md money-rule wording (owner, 2026-10-20), to land with 4.1: "BigDecimal and
  RoundingMode.HALF_UP everywhere, via internal.Money, at the scale each layer carries: line values unrounded up to 4
  decimals, VAT and document totals summed unrounded and rounded to 2, and the whole-number fields to 0 — as UJP's
  /json-struktura states. Never a second rounding mode."
- [x] 0.4 Fetch the gateway's API description (2026-10-20, `specification-status/openapi-2026-10-20.md`). It covers
  `einvoice_api` only: the error body, status and catalogues. The JSON Receiver, which takes the document, has
  none published, so 9 validates the transport side against it, and the document shape stays sourced from the type
  pages.
- [ ] 0.5 Obtain a qualified certificate, register it at `eujptest.ujp.gov.mk/ureg`, and get the privileges granted.
  Blocks 10 only.

## 1. Branch and CI

- [x] 1.1 Extend `pr-check.yml` `branches:` to `[master, 'next/**']` (a `ci:` commit, which mints no version). Create
  `next/1.0` from `master` once this plan has merged.
- [ ] 1.2 Record in `testing-and-gates` that `next/1.0` publishes nothing and merges once, with `BREAKING CHANGE` in
  the merge body.

## 2. Sources pinned

- [ ] 2.1 Fold `comparison-2026-10-19.md` into `specification-status/spec.md`: D3' replaces D3, plus the type pages,
  `/json-struktura`, `/status`, the FAQ and the changelog as sources, each with its retrieval date.
- [ ] 2.2 Record the `/известувања` changelog as the only history the wiki exposes, and the TLS leaf expiry of
  2026-12-13.

## 3. Error catalogue (self-contained, first code to land)

- [ ] 3.1 Replace the five flat constants with three namespaced catalogues (JSON Receiver, eInvoiceApi, Web App) from
  D5 as of 2026-10-05, including E1033–E1035, E500719/E500720 and E5014–E5019.
- [ ] 3.2 Keep an unknown code and the raw response body, and never map them to a guess.
- [ ] 3.3 Correct E1012 ("certificate not found"), and cite E5004 and E10001–E10003.

## 4. Money

- [x] 4.1 `internal.Money`: one `HALF_UP`, line values at scale 4 unrounded, VAT and document totals summed unrounded
  then rounded to 2, and the integer fields rounded to whole numbers. Replace `invoice-model`'s "round the net first"
  requirement.
- [x] 4.2 Add a property test: totals computed from unrounded lines differ from totals of rounded lines in the cases
  the guide describes, and the library always produces the former.

## 5. Model (decision 2)

- [x] 5.1 Amend `library-architecture`: `model` follows UJP's document concepts, with no wire names and no JSON types.
- [x] 5.2 Document, header, typed references (with the 11 reference types added 15.10), seller, buyer, payment, lines
  with attributes and per-line discount, document totals including `docAmountIncrease`/`docAmountDecrease` and the
  final-amount formula, and VAT totals by group and by indicator.
- [x] 5.3 `VatGroup`, and `TaxIndicator` with the dated `TaxIndicators` snapshot (decision 4).
- [x] 5.4 `DocumentKind` table for 100, 110, 120, 130 and 310 from the type pages, with mandatory fields refused
  by name.
- [x] 5.5 `Storno` and `Correction` as the `docStorno` axis (decision 5). Negative amounts are allowed only on a
  storno.
- [x] 5.6 `product-boundaries`: the natural-person address cut stands, since 170 is not in 1.0; record that 170
  re-opens it. The credit-note sign rule stands.

## 6. Serializer

- [x] 6.1 The envelope, and `document` with `voidReason`/`correctionReason` as siblings.
- [x] 6.2 `null` and `[]` for absent fields. Emit no undocumented member. `docAlt` and the currency block follow
  decision 6.
- [x] 6.3 Goldens from the 08.10 examples: 100, 100 storno, 100 correction, 110, 120, 130 and 310. Recompute every
  total from the model, and keep `scripts/verify-jws.py` as the independent check.

## 7. Signing

- [ ] 7.1 Header `{"alg":"RS256","typ":"JWT"}` cited to the FAQ, with its marker removed. `x5c` keeps its marker.

## 8. Transport

- [ ] 8.1 Send path, envelope, the four headers, and `UjpClient.Builder` requiring the EUJP id, the ЕДБ and the
  certificate serial.
- [ ] 8.2 `requestTimestamp` from `/server-time`, and the one-request-per-second throttle.
- [ ] 8.3 `current-status` and its history call. Status is the served code/name pair, with the ten known codes as
  constants.
- [ ] 8.4 The submit response fields. The error body is kept raw.
- [ ] 8.5 WireMock coverage of every path above. No real host appears anywhere in the tests.

## 9. Schema gate (needs 0.4)

- [ ] 9.1 Commit the `einvoice_api` OpenAPI under `src/test/resources`, and validate every status, catalogue and
  error body the client parses against it with a test-scoped validator.
- [ ] 9.2 Settle `docAlt`'s type, null versus omit, `docGrossAmountR`'s rounding and the undocumented members. Remove
  each remaining marker the schema confirms, with a citation.
- [ ] 9.3 Re-run the comparison against `/известувања` and record anything newer in a dated comparison file.
- [ ] 9.4 Sweep `@ProvisionalSpec`. What remains must be the production URL, `x5c`, and anything the schema left open,
  each named in `specification-status`.

## 10. Release (needs 9; submission additionally needs 0.5)

- [ ] 10.1 Merge `next/1.0` to `master` with `BREAKING CHANGE`, publish 1.0.0, and keep 0.1.0 and 0.2.0 resolvable.
- [ ] 10.2 Tell kapar.net (its checklist is the 09-13 comparison's "BREAKING" section) and reopen Invicta's
  `ujp-e-invoicing` to consume 1.0.0.
- [ ] 10.3 `submission-going-live` stays parked until the sandbox gate (design, decision 9.3). `UjpClient` is never
  called for a real filing before it.
