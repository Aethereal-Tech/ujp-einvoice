## Why

The official specification is no longer unreachable. On 2026-09-13 it was retrieved and compared field by field
against the reconstruction: D1 (the „API Спецификација" wiki page), D2 (`API-Documentation-Public8`, superseded by
D1 where they differ), D3 (the 01.09.2026 JSON examples — the de facto field-level spec), D4 (the 24.04.2026 code-list
dictionary, partial), D5 (the official error catalogue) and D6 (the certificate-registration and privilege model).
The comparison is recorded in full at `openspec/specs/specification-status/comparison-2026-09-13.md`.

The finding is not a list of corrections to apply piecemeal — it is that 0.2.0 is wrong at the root. The signed
payload is not a flat invoice but `{requestTimestamp, document: {header, docReferences, seller, buyer, docPayment,
docItems, docTotals, vatTotals}}`; the request body is not the raw JWS but a JSON envelope wrapping it; and four
request headers the gateway requires (`X-EUJP-ID`, `X-EDB`, `X-SERIAL-NUMBER`, `X-DOC-TYPE-CODE`) are sent by
nobody. **0.2.0's serializer produces a document the gateway would reject on its first validation step.** What
remains is therefore not a verification pass — it is a rewrite of the wire layer, guided field-by-field by D1–D5
rather than by integrator reports.

## What Changes

**BREAKING.** Every serialized field name changes, `UjpClient.Builder` gains required configuration (EUJP id, ЕДБ,
certificate serial), `UjpClient.status`, `SubmissionStatus` and `SubmissionResult` change shape, `VatCategory`'s
wire codes and `EXEMPT` change meaning, `DocumentType`'s wire codes change and `DocumentReference` is replaced by a
list of typed references. A consumer pinned to 0.2.0 is not a drop-in upgrade; see the comparison's own "BREAKING
for a consumer pinned to 0.2.0" section for the full list.

The order of work, condensed from the comparison's §5:

1. **Pin the sources.** Record D1–D6 (URL, revision, retrieval date), the documentation host's TLS-chain gotcha
   (the intermediate certificate is missing from the handshake; retry with `curl`, not a fetcher that refuses an
   incomplete chain), and that D4 covers only code-list-bearing fields.
2. **Error codes first** — the cheapest, most self-contained, mostly-additive step. Replace the five constants with
   the real catalogue, namespaced per API (JSON Receiver / eInvoiceApi / Web App), correct `E1012`'s gloss, and
   settle E5004 / E10001–E10003 with citations.
3. **`model` — the decision that has to be made before anything else is written.** This library's architecture rule
   is that a wire fix reaching into `model` means the abstraction leaked and the design should be reconsidered
   rather than pushed through. It leaked: the corrected shape needs `docStorno`, the `docType` code and name,
   turnover/delivery/period dates, `docId`, notes/header/footer/indicator, a list of typed references, party
   country code and name, VAT number, foreign TIN, contact, email, subsidiary, a street number, the whole
   `docPayment` block, per-line number/SKU/sender-receiver codes/discount/tax indicator/domestic-product flag/HS
   code, discount and advance totals, rounded-vs-unrounded gross, per-indicator VAT totals with a note, and
   void/correction reason objects — none of which `model` can express today. Decide deliberately whether this
   extends `Invoice` or introduces a UJP-shaped document model beside it; do not let the answer emerge from the
   serializer.
4. **`VatCategory` and tax indicators.** Rate codes become `DDV-A`/`DDV-V`/`DDV-B`/`DDV-G` (18/10/5/0). Introduce
   the tax-indicator concept as its own type, with `vatImpact` (`STANDARD`, `NULA`, `OSLOBODEN`, `PRENESEN`) driving
   the calculations; `EXEMPT` cannot survive as one constant.
5. **Money and the totals formulas.** Re-derive against D1's published formulas — per-unit VAT first, then
   multiplied by quantity, at four decimal places on unit amounts — and add `docNetAmountDisc`, `docGrossAmountR`
   and `docFinalAmount` alongside the existing totals. The rounding mode and scale stay provisional; nothing is
   published.
6. **The serializer.** Write the real envelope and document shape, with `voidReason`/`correctionReason` as siblings
   of `document`. Decide explicit `null` versus omission for absent fields; the examples write `null` throughout,
   so following them is the defensible default.
7. **Transport.** Fix the send path to `/JSONReceiver/api/v1/sales-invoices/send`; wrap the JWS in
   `{requestTimestamp, jws}`; add the four mandatory headers, which means `UjpClient` needs an EUJP id, an ЕДБ and
   the certificate serial as configuration; source `requestTimestamp` from `GET /api/v1/server-time` (±5 minutes,
   Skopje time); add the 1-request-per-second throttle.
8. **Status.** Replace `status(String euid)` with `POST /documents/sales-invoice/current-status` over a signed
   body, plus the history call. Replace `SubmissionStatus` with the gateway's served code/name pair rather than a
   closed enum that goes stale.
9. **Submit response.** `qr_link` not `qrLink`; `status` is an integer, not a word; add `timestamp` and
   `e_invoice_user`. The error body shape for send is unpublished — keep whatever is parsed marked provisional.
10. **Golden files.** All four are regenerated; the "an INVOICE serializes byte-for-byte as in 0.1.0" guarantee
    ends, because `docType` is mandatory on every document. Add per-document-type goldens (100, 110, 120, storno,
    correction, advance) derived from D3, and keep `scripts/verify-jws.py` as the independent check.
11. **Sweep `@ProvisionalSpec`.** Remove each marker with a citation to D1/D3/D4/D5 in the same change.
    `PRODUCTION_BASE_URL` keeps its marker — still unstated. So does the RS256 choice, `x5c`, the rounding
    convention, the null-versus-omit choice, and the natural-person buyer shape.
12. **Do not lift the `UjpClient` ban with this release.** Verification against the spec is one of three
    preconditions; a registered qualified certificate and a granted privilege set are the others, and neither is in
    hand. The rule that the submission client is never called until the specification is verified stays exactly as
    written, in this library and in every consumer that carries it.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `specification-status`: entries move from reconstructed to verified, cited section by section, as each step
  above lands.
- `serialization`: the envelope, the document shape, and the null-versus-omission decision.
- `transport`: the send path, the JWS envelope, the four mandatory headers, `requestTimestamp` sourcing and the
  throttle, and the status-polling endpoint and payload.
- `invoice-model`: the model decision in step 3 — whichever shape `Invoice` ends up taking to carry the fields the
  corrected wire format requires.
- `library-architecture`: the "a wire fix that touches `model` is a sign the abstraction leaked" invariant is the
  decision step 3 has to make deliberately, not the one this proposal pre-empts.

This change carries `skip_specs: true` (see `.openspec.yaml`): the corrected shape is not built yet, so a spec
delta here would state behaviour that does not exist.

## Impact

`ProvisionalSpec.java`, `UjpJsonSerializer`, `UjpEndpoints`, `UjpClient` (and its `Builder`), `UjpException`,
`SubmissionResult`, `SubmissionStatus`, `VatCategory`, `DocumentType`, `DocumentReference`, `Invoice`'s canonical
constructor and builder, the golden files, and `scripts/verify-jws.py`.

## Status

**Tag.** Untagged — not built yet.

**Why it waits.** Two things, both outside this change's reach: the gateway's own JSON Schema behind the Swagger UI
at `efakturatest.ujp.gov.mk/einvoice_api/swagger-ui/index.html` has not been fetched — it sits on the API host, not
the documentation host, so a person has to retrieve it, and D3's examples are illustrations rather than a
substitute for it. And the version decision — 1.0.0, or a parallel package for the verified shape, but never
`0.3.0` given the breadth of the break — has not been made.

**Reopens when** both the JSON Schema is in hand and the version decision is made.

**Prerequisites (owner).** The library owner — fetching the JSON Schema from the API host, and deciding the version
number.
