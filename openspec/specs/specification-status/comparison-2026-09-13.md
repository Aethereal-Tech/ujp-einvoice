# УЈП e-invoicing specification vs `ujp-einvoice` 0.2.0

Investigation date: 2026-09-13. Library state: `~/Repositories/Private/ujp-einvoice`, master `eadcbd9`, clean,
published as `net.aetherealtech:ujp-einvoice` 0.2.0.

**No УЈП API endpoint was called.** No submission, no sandbox request, no authentication attempt, no `UjpClient`
execution, and no request of any kind to `efakturatest.ujp.gov.mk` or `efaktura.ujp.gov.mk`. Everything below was
read from the documentation host `efakturawiki.ujp.gov.mk` (HTML pages and PDFs it serves) and from the library's
own source. No file in either repository was modified; no build was run.

---

## 1. The specification is now readable

It is reachable from this machine. The obstacle that blocked earlier attempts is **not** geography and **not**
authentication — it is a TLS chain misconfiguration on the documentation host.

`efakturawiki.ujp.gov.mk` (146.255.93.226) serves a valid Let's Encrypt leaf certificate
(`CN=efakturawiki.ujp.gov.mk`, issuer `C=US, O=Let's Encrypt, CN=YR2`, valid 2026-06-25 → 2026-09-23) but sends
**only the leaf** — the intermediate is missing from the handshake. `openssl s_client` reports
`verify error:num=21:unable to verify the first certificate`; any client that will not fetch the missing
intermediate itself (Node, and therefore the agent `WebFetch` tool) fails outright, while `curl` against the macOS
system trust store succeeds and returns `HTTP 200`. **A future reader should not conclude the wiki is unreachable
because a fetch failed; retry with `curl`.** Note the leaf expires 2026-09-23 — if it is renewed without fixing the
chain, the same symptom returns.

### Documents in hand

| # | Title | URL | Format | Version / date | Role |
|---|---|---|---|---|---|
| D1 | **API Спецификација** — `wiki Ефактура` | `https://efakturawiki.ujp.gov.mk/тест_апи` | HTML | no version or date stated on the page; embedded example timestamps run to 2026-09-01 | The authoritative spec page named in the library's `CLAUDE.md`. Endpoints, headers, validations, totals formulas, response shapes |
| D2 | **API Documentation — eInvoice-single-processing-api** | `https://efakturawiki.ujp.gov.mk/downloads/api-documentation-public8.pdf` | PDF, 35 pages | PDF `/Title` = `API-Documentation-Public8` → **revision "Public8"**; no date inside | Same material as D1 in downloadable form. Where D1 and D2 differ, D1 is newer (it carries services D2 does not) |
| D3 | **еФактура JSON примери** | `https://efakturawiki.ujp.gov.mk/downloads/primer_za_json_01.09.pdf` (linked on D1 as `json_primeri_01.09.2026.pdf`) | PDF, 71 pages | **01.09.2026** per the filename and D1's link text | 30+ worked JSON documents, one per document type. **The de facto field-level specification** |
| D4 | **Шифрарник за полиња на е-фактура** | `https://efakturawiki.ujp.gov.mk/шифрарници` and `https://efakturawiki.ujp.gov.mk/downloads/sifrarnici_24042026.pdf` | HTML + PDF, 8 pages | filename **24.04.2026**; PDF `/CreationDate` = **2026-04-28** | The code lists (document types, tax indicators, VAT groups, reference types, payment types) and the mandatory markers — **but only for fields that draw on a code list** |
| D5 | **Error Codes — eInvoiceApi / JSON Receiver / Web App** | `https://efakturawiki.ujp.gov.mk/error-codes` | HTML | no version or date stated | The complete official error catalogue. Not linked from D1's body; found by probing. **This is the document that settles E5004 and E10001–E10003** |
| D6 | **Доделување на пристап во е-УЈП** | `https://efakturawiki.ujp.gov.mk/ДоделувањепристапвоеУЈП` (+ `downloads/pristap_eujp.pdf`) | HTML + PDF | not stated | The privilege model and certificate registration prerequisite |
| D7 | Second JSON example set | `https://efakturawiki.ujp.gov.mk/downloads/primer_za_json_2.pdf` | PDF, 16 pages | PDF `/CreationDate` = **2026-04-29** | Superseded by D3; keep named |

**Current:** D1 (page) + D3 (01.09.2026 examples) + D4 (24.04.2026 code lists) + D5 (error codes). **Superseded but
named:** D2 (`Public8` — its content is a subset of D1) and D7 (2026-04-29 examples, superseded by D3).

### What is still NOT published

- **No machine-readable schema on the documentation host.** The only JSON Schema / OpenAPI document lives behind
  the Swagger UI at `https://efakturatest.ujp.gov.mk/einvoice_api/swagger-ui/index.html` — on the **API host**, so
  it was deliberately not fetched. A person must retrieve it; error `E4013 – JSON schema validation failed` proves
  the gateway validates against one, and the examples in D3 are not a substitute for it.
- **No full field-optionality matrix.** D4 is titled a dictionary of *fields* but covers only code-list-bearing
  fields (`docType`, `docTypeName`, `docTypeRef`, `docNameRef`, the seller/buyer country, TIN, VAT number, name and
  address block, `docItemVat`, `docItemVatGroup`, `docItemTaxIndicator`, `docItemDomesticProduct`, the tax totals
  and the payment group). `docItemDesc`, `docItemQty`, `docItemMUnit`, `docNumber`, `docDate` and most of the rest
  appear **only in the D3 examples**, with no stated type, length or mandatory flag.
- **No production base URL anywhere.** Every URL in D1–D7 is `efakturatest.ujp.gov.mk`.
- **No response body shape for a failed submission.** D5 lists the codes; neither D1 nor D2 shows the JSON an error
  is delivered in for `/JSONReceiver/.../send`. The `einvoice_api` services show an `errorStatus` member, shape
  unspecified (`null` in every example).
- **No natural-person (B2C) buyer example.** All 30+ documents in D3 carry a company buyer with a TIN; `buyerTin`
  is never `null` and `buyerAddress` is never `null`. D4 marks `buyerTin` and the whole `buyerAddress` block
  mandatory. D1 does say "Одредени проверки може да се прескокнат, во зависност од типот на документот"
  (certain checks may be skipped depending on the document type), but does not say which.

### D4's mandatory markers

D4 tags each field with `З`, `ЗШ`, `ЗП`, `О`, `ОШ` or `ОП`. **The document contains no legend.** From usage, the
`З`-prefixed markers are mandatory and the `О`-prefixed optional, with `Ш` marking a value drawn from a code list
and `П` its human-readable name — but that reading is **inferred, not stated**, and should not be treated as
confirmed.

### Nothing addressed to an AI agent

No page or PDF read in this investigation contained text addressed to an AI agent, instructions to take an action,
or a claim of authority. The library's own files likewise contained none. All content was treated as data.

---

## 2. The headline: the reconstruction is wrong at the root

The library's guesses were internally consistent and are almost entirely wrong. Three structural mismatches sit
above the field level:

1. **The signed payload is not a flat invoice.** It is
   `{"requestTimestamp": "...", "document": {"header": {...}, "docReferences": [...], "seller": {...},
   "buyer": {...}, "docPayment": {...}, "docItems": [...], "docTotals": {...}, "vatTotals": [...]}}`,
   with `voidReason` / `correctionReason` as **siblings of `document`**, not inside it. The library emits a flat
   object with `invoiceNumber` at the top level and no `requestTimestamp` at all.
2. **The request body is not the JWS.** The gateway takes a JSON envelope
   `{"requestTimestamp": "2025-11-19T12:00:00", "jws": "string"}`; the library POSTs the raw compact JWS as the
   body with `Content-Type: application/jose`.
3. **Four request headers are mandatory and the library sends none of them** —
   `X-EUJP-ID`, `X-EDB`, `X-SERIAL-NUMBER`, and on the send endpoint `X-DOC-TYPE-CODE`. `E4016 – Missing required
   request headers`, `E4020 – Request header mismatch`, `E4021/E4022/E4023 – Invalid X-SERIAL-NUMBER / X-EUJP-ID /
   X-EDB` all exist for this.

Also load-bearing and absent from the library: `requestTimestamp` must be Skopje-local, format
`2026-01-05T12:00:00`, and **within 5 minutes of server time** (`E40122`); a `GET /api/v1/server-time` service
exists to source it; and there is a **rate limit of 1 request per second per user** (`E4007`).

---

## 3. Comparison table

Verdict vocabulary: **CONFIRMED** — library matches the spec. **DIFFERS** — both have it, differently.
**ABSENT FROM THE SPEC** — the library has it and no document names it. **ABSENT FROM THE LIBRARY** — the spec
requires or defines it and the library has nothing. **SPEC SILENT** — the question is open in the documents read.

### 3.1 Transport and envelope

| Library | Spec | Verdict |
|---|---|---|
| `TEST_BASE_URL = https://efakturatest.ujp.gov.mk` | `https://efakturatest.ujp.gov.mk/einvoice_api/` for services; the same host for the receiver | **CONFIRMED** (host). The library models one base URL; the spec has **two path roots** on that host — `/einvoice_api/` and `/JSONReceiver/` |
| `PRODUCTION_BASE_URL = https://efaktura.ujp.gov.mk` | not stated in any document read | **SPEC SILENT** — keep `@ProvisionalSpec` |
| `SALES_INVOICE_SEND = /JSONReceiver/sales-invoices/send` | `/JSONReceiver/api/v1/sales-invoices/send` (D1 gives both the absolute URL and `POST - /api/v1/sales-invoices/send`) | **DIFFERS** — `/api/v1` is missing |
| `SALES_INVOICE_STATUS_TEMPLATE = /JSONReceiver/sales-invoices/status/%s`, `GET` | `POST /api/v1/documents/sales-invoice/current-status`, JWS-signed body `{requestTimestamp, euid}`; history at `POST /api/v1/documents/sales-invoice/status` with `{requestTimestamp, euid, dateFrom}` | **DIFFERS** — wrong verb, wrong path, and the EUID travels in a signed body, not a path segment |
| submit body: raw compact JWS, `Content-Type: application/jose` | `{"requestTimestamp": "...", "jws": "..."}` | **DIFFERS**. The spec does not state a `Content-Type`; the body is JSON |
| no request headers | `X-EUJP-ID`, `X-EDB` on every service; `X-SERIAL-NUMBER` wherever signing is involved; `X-DOC-TYPE-CODE` (e.g. `100`) on send | **ABSENT FROM THE LIBRARY** |
| no `requestTimestamp` | mandatory, Skopje time, `yyyy-MM-dd'T'HH:mm:ss`, ±5 minutes of server time, inside the signed payload as replay protection | **ABSENT FROM THE LIBRARY** |
| no throttling | 1 request/second per user | **ABSENT FROM THE LIBRARY** |
| `x5c` certificate chain in the JWS header (unconfirmed) | not stated; the gateway identifies the certificate by `X-SERIAL-NUMBER` and pre-registration, and `E5012 – Certificate serial mismatch` exists | **SPEC SILENT** on `x5c`. The serial-number header is the mechanism the spec does name |
| RS256 compact JWS | JWS confirmed throughout (`E5005 – JWS validation failed`, `E50051 – Invalid JWS signature`); the **algorithm is never named** | **CONFIRMED** (JWS); **SPEC SILENT** (RS256 specifically) |
| wire format is proprietary JSON, not UBL/XAdES | confirmed — JSON throughout; an ASiC container exists as a separate retrieval service, not a submission format | **CONFIRMED** |

### 3.2 Submit response

| Library reads | Spec returns | Verdict |
|---|---|---|
| `euid` | `euid` (e.g. `019b8d43-7840-7433-b358-08891b53605c`) | **CONFIRMED** |
| `qrLink` | `qr_link` (`https://efakturatest.ujp.gov.mk/euid/<euid>`) | **DIFFERS** — snake_case |
| `status`, parsed as a word into `SubmissionStatus` | `status: 200` — an **integer** | **DIFFERS** — type and meaning both wrong |
| `message` | `message` (`"Фактура успешно зачувана"`) | **CONFIRMED** |
| — | `timestamp` (receipt time, `2026-01-05T08:26:07.846Z`) | **ABSENT FROM THE LIBRARY** — and it is the timestamp the whole process exists to obtain |
| — | `e_invoice_user` (boolean) | **ABSENT FROM THE LIBRARY** |
| `errorCode`, `errorMessage` on HTTP ≥ 400 | not shown for this endpoint. The `einvoice_api` services carry `success`, `createdDate`, `errorStatus` | **SPEC SILENT** — the error body shape for send is unpublished. `errorCode`/`errorMessage` as names are **ABSENT FROM THE SPEC** |

### 3.3 `SubmissionStatus`

| Library | Spec | Verdict |
|---|---|---|
| `PENDING` ← `PENDING`/`IN_PROGRESS`/`PROCESSING`; `ACCEPTED` ← `ACCEPTED`/`SUCCESS`/`OK`/`SENT`; `REJECTED` ← `REJECTED`/`FAILED`/`ERROR`; `UNKNOWN` fallback | `GET /api/v1/document-statuses` returns `{code, name, description}`: `00` Нацрт (draft), `01` Испратена (Нова), `03` Прифатена, `07` Сторнирана, and more the page elides with `…`. `current-status` returns `statusCode` + `statusName`; history returns `oldStatus`/`newStatus` pairs | **DIFFERS** entirely — the vocabulary is a **numeric code list served by the gateway**, not a word enum. The library's four constants map onto nothing. `UNKNOWN`-as-fallback saved it from throwing, and is the one design choice that survives |

### 3.4 Error codes

The catalogue is **namespaced per API** — the same code means different things in `eInvoiceApi`, `JSON Receiver`
and the Web App. The library models a single flat set, which is itself a mismatch. The submit endpoint is
`JSON Receiver`.

| Library | Spec | Verdict |
|---|---|---|
| `E1012_CERTIFICATE_NOT_REGISTERED = "E1012"`, documented as "signing certificate not pre-registered at `eujptest.ujp.gov.mk/ureg`" | `E1012 – Certificate not found` (all three APIs) | **DIFFERS in meaning** — the code and its neighbourhood are right, the gloss is narrower than the spec's. The spec's own "not registered" case is `E1013 – User is not authorized to sign documents for this company`, and the inverse is `E1020 – This certificate for this company already exists!` (eInvoiceApi only) |
| `E5004`, meaning unknown | **`E5004 – Certificate validation failed`** (all three APIs) | **CONFIRMED to exist; meaning now settled** |
| `E10001`, meaning unknown | **`E10001 – Missing TIN during company validation process`** (JSON Receiver) | **CONFIRMED to exist; meaning now settled** |
| `E10002`, meaning unknown | **`E10002 – An incorrect company name was provided`** (JSON Receiver) | **CONFIRMED to exist; meaning now settled** |
| `E10003`, meaning unknown | **`E10003 – An incorrect VAT number was provided for the company`** (JSON Receiver) | **CONFIRMED to exist; meaning now settled** |
| — | ~130 further codes across three namespaces, including the ones a submitter will actually meet: `E4012`/`E40121`/`E40122` (requestTimestamp), `E4013`/`E40131` (schema, malformed JSON in JWS), `E4015` (invalid signature), `E4016`/`E4020`/`E4021`/`E4022`/`E4023` (headers), `E4019` (missing required fields), `E1020`/`E10201`/`E10202`/`E10203` (invalid calculated totals — document, docTotals, vatTotals, item), `E1023` (invoice already exists for that ID), `E1024`–`E1031` (void/correction reference rules), `E5002`/`E5003`/`E5004`/`E5011`/`E5012` (certificate), `E5005`/`E50051` (JWS), `E5007` + `E50071`–`E500718` (document validation, field by field), `E5008` (TimeStampToken) | **ABSENT FROM THE LIBRARY** |

### 3.5 Document type and the note question

| Library | Spec | Verdict |
|---|---|---|
| `documentType` top-level field, written only for a note | `document.header.docType` — a **3-character code from a code list**, always present, plus `docTypeName` its Macedonian name. `100` = Фактура | **DIFFERS** |
| `"CREDIT_NOTE"` | **`docType` `"110"`, `docTypeName` `"Книжно одобрение"`** | **DIFFERS** |
| `"DEBIT_NOTE"` | **`docType` `"120"`, `docTypeName` `"Книжно задолжение"`** | **DIFFERS** |
| an `INVOICE` writes no type field | `docType` is mandatory on every document | **DIFFERS** — and this retires the "an INVOICE serializes byte for byte as in 0.1.0" guarantee |
| `correctedInvoice: {number, issueDate}` | `docReferences: []` — an **array** of `{docTypeRef, docNameRef, docDateRef, docNumberRef}`, `docTypeRef` from a code list (`ST` Сторно/Корекција, `DOG` Договор, `NAR` Нарачка, `PRO` Профактура, `C_D` Царинска декларација, `FIS` Фискална сметка, `DOC` Други документи, and others) | **DIFFERS** — one reference becomes many, each typed and named |
| amounts on a note stay positive | **CONFIRMED** — the `110` and `120` examples carry positive quantities and totals | **CONFIRMED**. The library's FUTURE 10 (CUT — signed amounts) holds *for notes*. It does **not** hold for storno: D1 says a cancellation sends `header.docStorno = 1` with **negative quantities and totals** |
| no storno / correction axis | `header.docStorno`: `0` ordinary, `1` storno (negative amounts + a `voidReason` object `{voidCode, voidReason, voidComment}`, codes `S-1`…), `2` correction (a replacement document + a `correctionReason` object `{correctionReasonCode, correctionReason, correctionReasonComment}`, codes `C01`…), both referencing the original via `docReferences` with `docTypeRef: "ST"` | **ABSENT FROM THE LIBRARY** |

### 3.6 Header (`document.header`)

| Library | Spec | Verdict |
|---|---|---|
| `invoiceNumber` | `docNumber` | **DIFFERS** |
| `issueDate` | `docDate` | **DIFFERS** |
| `dueDate` | `docPayment.docPaymentTypeDueDate` (+ `docPaymentTypeDueDays`) — not a header field at all | **DIFFERS** — relocated |
| — | `docId`, `docTurnoverDate`, `docDeliveryDate`, `docDelivery`, `docPeriodStartDate`, `docPeriodEndDate`, `docNotes`, `docIndicator`, `docHeader`, `docFooter`, `docStorno`, `docType`, `docTypeName` | **ABSENT FROM THE LIBRARY**. `E50071`/`E50072` prove `docDeliveryDate` and `docTurnoverDate` are validated against `docDate` |
| `currency` (ISO code string, top level) | `docPayment.docCurrency` + `docCurrencyCode` + `docCurrencyDate` + `docCurrencyExchRate`; a `GET /api/v1/currency` code list and a `POST /api/v1/currency-exchange/rate` service back them | **DIFFERS** — relocated, and one field becomes four |

### 3.7 Parties

| Library | Spec (seller; buyer is the same with a `buyer` prefix) | Verdict |
|---|---|---|
| `seller` / `buyer` object names | `seller` / `buyer` | **CONFIRMED** |
| `name` | `sellerName` / `buyerName` — mandatory (D4 `UJP03-06`, `UJP05-06`) | **DIFFERS** — prefixed per party, not a shared key |
| `taxId` (ЕДБ), omitted for a natural person | `sellerTin` / `buyerTin`, **mandatory** (D4 `UJP03-03` `ЗШ`, `UJP05-03` `ЗШ`); `E10001 – Missing TIN` | **DIFFERS** in name; **and the library's omission for a natural-person buyer is unsupported by anything published** |
| — | `sellerVatNumber` / `buyerVatNumber` (ДДВ број, e.g. `МК4030995135699`) — optional per D4, but `E10008 – VatNumber missing!` and `E10003`/`E10007` exist | **ABSENT FROM THE LIBRARY** |
| — | `sellerForeignTin` / `buyerForeignTin` | **ABSENT FROM THE LIBRARY** |
| `address.country` (ISO alpha-2, defaulting to `MK`, **inside the address**) | `sellerCCode` / `buyerCCode` **on the party**, plus `sellerCName` / `buyerCName` the country's name from a code list; `E10011`/`E10012` | **DIFFERS** — relocated out of the address, and the name is required alongside the code |
| `address` | `sellerAddress` / `buyerAddress` — mandatory object | **DIFFERS** in name |
| `address.street` | `streetAddress` (street name) **and** `streetNumber` (house number), separate mandatory fields | **DIFFERS** — the library has no street number and cannot express one |
| `address.postalCode` | `postalCode` — mandatory, from a code list; `E10005` | **CONFIRMED** (name) |
| `address.city` | `city` — mandatory, from a code list; `E10004` | **CONFIRMED** (name) |
| — | `sellerContact`, `sellerEmail`, `sellerSubsidiaryCode`, `sellerSubsidiaryName`, `sellerSubsidiaryAddress` (and the buyer equivalents, where the subsidiary is the delivery location) | **ABSENT FROM THE LIBRARY** |
| `Party.naturalPerson(name)` — no tax id, all address parts optional | no published shape for it. Every one of D3's 30+ examples has a company buyer with a TIN and a full address; D4 marks TIN, address, street, number, postal code and city mandatory. D1 says some checks are skipped per document type but not which | **SPEC SILENT / likely DIFFERS.** The library's FUTURE 9 (CUT — requiring an address from a natural person) is **now an open question, not a settled CUT** |
| `taxId` format not enforced | `String`, length 30 (D4) — no format rule published | **CONFIRMED** as a deliberate non-rule; the spec states a length the library does not check |
| blank refused, absent omitted | the examples write `null` for every absent optional field and never omit a key | **DIFFERS** — the library omits; the spec's examples are explicit `null`. Whether the gateway tolerates omission is unpublished (`E4019 – Missing required fields` exists) |

### 3.8 Line items

| Library (`lineItems[]`) | Spec (`docItems[]`) | Verdict |
|---|---|---|
| array name `lineItems` | `docItems` | **DIFFERS** |
| `description` | `docItemDesc` | **DIFFERS** |
| `quantity` | `docItemQty` | **DIFFERS** |
| `unit` — free text, optional, and the library's README says "nothing says UJP expects a unit at all" | `docItemMUnit` — present in every example (`"kg"`, `"par."`). Not in D4, so no code list and no stated optionality | **DIFFERS** in name. The library's doubt that the field exists is **resolved: it exists**. FUTURE 8 (CUT — a unit code list) still holds: no code list is published, and the example values are free text |
| `unitPrice` | `docItemUnitOriginalPriceWoVat` (list price) **and** `docItemUnitPriceWoVat` (after discount), with `docItemUnitDiscountAmount` between them | **DIFFERS** — one field becomes three, and the library cannot express a discount |
| `vatCategory` (one string) | three fields: `docItemVat` (the **numeric rate**, e.g. `18`), `docItemVatGroup` (the **rate code**), `docItemTaxIndicator` (the **treatment code**) | **DIFFERS** — one concept splits into three |
| `netAmount` | `docItemTotalPriceWoVat` (and `docItemTotalOriginalPriceWoVat` before discount) | **DIFFERS** |
| `vatAmount` | `docItemTotalVat`; plus `docItemUnitVat`, the per-unit VAT | **DIFFERS** |
| `grossAmount` | `docItemTotalPriceWVat` | **DIFFERS** |
| — | `docItemLineNo`, `docItemSku`, `docItemSenderCode`, `docItemReceiverCode`, `docItemDomesticProduct` (`MP` = Macedonian product), `docItemHs` (HS tariff code) | **ABSENT FROM THE LIBRARY** |

### 3.9 VAT categories — every code is wrong

| Library `VatCategory` | Rate | Spec `docItemVatGroup` | Verdict |
|---|---|---|---|
| `STANDARD_18` → `"18"` | 0.18 | **`DDV-A`** | **DIFFERS** |
| `REDUCED_10` → `"10"` | 0.10 | **`DDV-V`** | **DIFFERS** |
| `REDUCED_5` → `"5"` | 0.05 | **`DDV-B`** | **DIFFERS** |
| `ZERO` → `"0"` | 0.00 | **`DDV-G`** | **DIFFERS** |
| `EXEMPT` → `"EXEMPT"` | 0.00 | **no single code.** Exemption is a `docItemTaxIndicator`, and there are several: `DDV-7-I` (export, exempt with input-tax credit, art. 24(1)(1)), `DDV-8` (exempt with credit, art. 24(1)), `DDV-9` (exempt without credit, art. 23), `DDV-10-13` / `DDV-10-14` (goods / services outside the scope, arts. 2 and 13/14) | **DIFFERS, and not one-to-one** — a single `EXEMPT` constant cannot carry the distinction the gateway validates |
| — | `docItemTaxIndicator` also covers `DDV-G` (not a VAT payer, art. 51(3)) and reverse charge under art. 32-a: `DDV-11-A` (18%), `DDV-11-V` (10%), `DDV-11-B` (5%) | **ABSENT FROM THE LIBRARY** |
| — | `vatImpact`, the axis the calculations branch on: `STANDARD`, `NULA`, `OSLOBODEN`, `PRENESEN`. Under `PRENESEN` VAT is excluded from item and document totals but **must appear in the per-indicator totals with a note** (`E1032 – Invalid Vat Tax Indicator Note`) | **ABSENT FROM THE LIBRARY** |

**Note the trap:** `DDV-B` is **5%** and `DDV-V` is **10%** — the alphabetical intuition is wrong, and D1's own
worked example pairs `"docItemVatGroup": "DDV-B"` with `"docItemVat": 5`. `DDV-G` appears in both code lists with
different meanings (0% rate group; "not a VAT payer" indicator).

### 3.10 Totals

| Library | Spec | Verdict |
|---|---|---|
| `totals` | `docTotals` | **DIFFERS** |
| `netTotal` | `docNetAmount` (= Σ `docItemTotalOriginalPriceWoVat`) and `docNetAmountDisc` (after discount) | **DIFFERS** — one field becomes two |
| `vatTotal` | `docVatAmount` | **DIFFERS** |
| `grossTotal` | `docGrossAmount`, plus `docGrossAmountR` (**rounded**) and `docFinalAmount` (= `docGrossAmountR` − `docAvansAmount`) | **DIFFERS** — one field becomes three |
| — | `docDiscountAmount`, `docAvansDate`, `docAvansDesc`, `docAvansAmount` | **ABSENT FROM THE LIBRARY** |
| `byCategory[]` with `{category, net, vat, gross}`, ordered by the enum's declaration order | `vatTotals[]` with `{vatTaxIndicator, vatTaxIndicatorNote, vatCode, vatPercent, vatTaxableAmount, vatAmount, vatTotalAmount}`, **grouped by `docItemVatGroup` AND `docItemTaxIndicator`** | **DIFFERS** — name, member names, grouping key, and the extra `vatPercent` and note. No ordering rule is published, so the library's deterministic `EnumMap` order is **SPEC SILENT** |
| rounding: net rounded per line, VAT computed from the rounded net; HALF_UP scale 2 | the published formulas compute **per-unit** first: `docItemUnitVat = docItemUnitPriceWoVat * docItemVat / 100`, then `docItemTotalVat = docItemQty * docItemUnitVat`. The examples carry **4 decimal places** on unit amounts (`95.2381`, `4.7619`) and 2 on totals; `docGrossAmountR` is a separately rounded gross. No rounding mode or scale is stated | **DIFFERS** — the order of operations is not the library's, and the library cannot represent 4-decimal unit prices alongside 2-decimal totals. `E1020`/`E10201`/`E10202`/`E10203` are the gateway refusing totals it recomputes differently |

### 3.11 Payment — wholly absent from the library

`docPayment` is mandatory in every example: `docPaymentTypeCode` (code list, e.g. `P11` card, `P12` bank
transfer), `docPaymentTypeDesc`, `docPaymentTypeDueDays`, `docPaymentTypeDueDate`, `docPaymentTerms`,
`docPaymentNote`, `docPaymentInterest`, `docPaymentDiscount`, `docCurrency`, `docCurrencyCode`,
`docCurrencyDate`, `docCurrencyExchRate`. Backed by `GET /api/v1/payment-types` and validated
(`E50074`/`E50075`/`E50078`). **ABSENT FROM THE LIBRARY** in its entirety.

### 3.12 Services the library does not know exist

All under `https://efakturatest.ujp.gov.mk/einvoice_api/`. **ABSENT FROM THE LIBRARY**, every one.

*Code lists (GET, no body):* `/api/v1/server-time`, `/currency`, `/countries`, `/void-reasons`,
`/correction-reason` (+ `/{date}`), `/reject-reason`, `/payment-types`, `/document-statuses`, `/document-types`,
document-types-with-tax-indicators, reference-document types, tax groups, tax indicators, input tax indicators.
*Paged:* `POST /api/v1/countries/list`, taxpayers/companies, subsidiaries.
*Documents:* `POST /api/v1/currency-exchange/rate`; `/documents/purchase-invoice/accept-reject`;
`/documents/sales-invoice/pdf` (base64, privilege `DP`); `/documents/sales-invoice/ids`;
`/documents/sales-invoice/status` (history); `/documents/sales-invoice/current-status`;
`/documents/sales-invoice/changes`; sales-invoice statuses for a period; purchase-invoice EUID lists, statuses,
current status, PDF, change list; purchase VAT totals; JSON retrieval for a sales or purchase invoice, by EUID
list or date range; and the ASiC container.
*Privileges* are granted per operation in `e-ujp.ujp.gov.mk` / `eujptest.ujp.gov.mk/ureg` (D6) — the library's
recorded prerequisite (a KIBS- or Telekom-issued qualified certificate registered there) is **CONFIRMED in
substance**: registration at `eujptest.ujp.gov.mk/ureg` is named, the privilege list is published, and
`E1013 – User is not authorized to sign documents for this company` enforces it. **The issuing CAs are not named
in any document read** — that half stays unconfirmed.

### 3.13 What survives intact

Short list, worth stating plainly: the `seller` / `buyer` / `city` / `postalCode` / `euid` / `message` names; JWS
as the signing envelope; JSON (not UBL) as the format; the EUID-plus-QR-link outcome; the sandbox host; the
positive-amounts rule for credit and debit notes; `UNKNOWN`-as-fallback rather than throwing; not enforcing an ЕДБ
format; not inventing a unit code list; and the three-layer architecture — whose value is about to be tested,
because the corrected shape **does** require `model` changes (see §5).

---

## 4. The four questions

**1. The status-polling endpoint — ANSWERED.** `POST /api/v1/documents/sales-invoice/current-status` on
`einvoice_api`, with a JWS-signed body `{"requestTimestamp": "...", "euid": "..."}`, returning
`{success, createdDate, docStatus: {euid, statusCode, statusName, docNumber, docDate, docDeliveryDate,
docNetAmount, docVatAmount, docGrossAmountR, docAvansAmount, docFinalAmount, sellerTin, buyerTin, senderTin,
receiverTin}}`. Status *history* is `POST /api/v1/documents/sales-invoice/status` with `{requestTimestamp, euid,
dateFrom}`. The library's `GET /JSONReceiver/sales-invoices/status/{euid}` is wrong in verb, path root, path shape
and payload. FUTURE 4 can be closed.

**2. The production base URL — STILL OPEN.** Every URL in every document read is `efakturatest.ujp.gov.mk`. The
spec does not say. `https://efaktura.ujp.gov.mk` remains an unverified inference and must keep its
`@ProvisionalSpec`. Ask УЈП, or read it off the production portal once an account exists.

**3. E5004 and E10001–E10003 — ANSWERED,** from D5, and each code exists exactly where the library placed it:

- `E5004 – Certificate validation failed` (all three APIs; sits with `E5002` expired, `E5003` revoked/OCSP,
  `E5011` no OCSP response, `E5012` serial mismatch)
- `E10001 – Missing TIN during company validation process` (JSON Receiver)
- `E10002 – An incorrect company name was provided` (JSON Receiver)
- `E10003 – An incorrect VAT number was provided for the company` (JSON Receiver)

Two caveats. `E1012` is `Certificate not found`, not the narrower "not pre-registered" the library documents. And
**codes are namespaced per API** — `E1020` is "certificate already exists" in `eInvoiceApi` but "Invalid
calculated totals" in the JSON Receiver, so a flat set of constants is itself a modelling error. FUTURE 5 can be
closed.

**4. Credit and debit notes — ANSWERED.** Not a `documentType` discriminator: `header.docType` carries **`110`**
with `docTypeName` `"Книжно одобрение"` for a credit note and **`120`** with `"Книжно задолжение"` for a debit
note, alongside `100` for an ordinary invoice — one mandatory code-list field on every document, not a field that
appears only on notes. Amounts stay **positive** (confirmed). The document a note relates to travels in
`docReferences[]` as `{docTypeRef, docNameRef, docDateRef, docNumberRef}`, not in a `correctedInvoice` object; in
D3's `110` and `120` examples `docReferences` is `[]`, and `GET /api/v1/document-types` publishes per-type
`hasReference` / `isReferenceMandatory` flags, so whether a note *must* reference is a runtime question for that
service. Distinctly from notes, **cancellation and correction are a separate axis**: `header.docStorno` `0`/`1`/`2`
with negative amounts and a `voidReason` object for a storno, or a replacement document and a `correctionReason`
object for a correction, both referencing the original with `docTypeRef: "ST"`.

---

## 5. What a 0.3.0 verification release would take

**This is not a verification pass. It is a rewrite of the wire layer**, and the honest framing for the owner is
that 0.2.0's serializer produces a document the gateway would reject on its first validation step. Nothing below
was implemented.

**Before any code**, one thing is missing that the wiki does not carry: **the JSON Schema behind the Swagger UI**
(`https://efakturatest.ujp.gov.mk/einvoice_api/swagger-ui/index.html`). `E4013 – JSON schema validation failed`
means the gateway validates against a real schema; D3's examples are illustrations, not a schema, and D4 covers
only code-list fields. A person must fetch it — it is on the API host, which this investigation did not touch.
Promoting field names on the strength of examples alone would repeat 0.2.0's mistake with better evidence.

### Order of work

1. **Pin the sources.** Commit the retrieved documents (or their URLs, revisions, retrieval dates and checksums)
   and rewrite `SPECS.md`'s "Specification inventory" and the README's status section against D1–D6. Record the
   TLS-chain gotcha so the next reader does not conclude the wiki is unreachable. Record that D4 is partial.
2. **Error codes first** — the cheapest, most self-contained win, and mostly additive. Replace the five constants
   with the real catalogue, **namespaced per API** (JSON Receiver vs eInvoiceApi vs Web App), correct `E1012`'s
   gloss, and settle E5004 / E10001–E10003 with citations. Drop those `@ProvisionalSpec` markers.
3. **`model` — the decision that has to be made before anything else is written.** This library's architecture
   rule is that a wire fix reaching into `model` means the abstraction leaked and the design should be
   reconsidered rather than pushed through. It leaked. The corrected shape
   needs, none of which `model` can express: `docStorno`; the `docType` code and name; turnover, delivery and
   period dates; `docId`; notes/header/footer/indicator; a **list** of typed references; party country code and
   name, VAT number, foreign TIN, contact, email, subsidiary; a street **number**; the whole `docPayment` block
   including currency date and exchange rate; per-line number, SKU, sender/receiver codes, per-unit discount,
   per-unit VAT, original price, tax indicator, domestic-product flag, HS code; discount and advance totals;
   rounded-vs-unrounded gross; per-indicator VAT totals with a note; and void/correction reason objects. Decide
   deliberately whether 0.3.0 extends `Invoice` or introduces a UJP-shaped document model beside it — do not let
   the answer emerge from the serializer.
4. **`VatCategory` and tax indicators.** Rate codes become `DDV-A` (18), `DDV-V` (10), `DDV-B` (5), `DDV-G` (0).
   Introduce the tax-indicator concept as its own type (`DDV-A/V/B/G`, `DDV-7-I`, `DDV-8`, `DDV-9`, `DDV-10-13`,
   `DDV-10-14`, `DDV-11-A/V/B`) with `vatImpact` (`STANDARD`, `NULA`, `OSLOBODEN`, `PRENESEN`) driving the
   calculations. `EXEMPT` cannot survive as one constant.
5. **Money and the totals formulas.** Re-derive against D1's published formulas: per-unit VAT first, then
   multiplied by quantity; the four-decimal unit amounts the examples carry; `docNetAmount` vs `docNetAmountDisc`;
   `docGrossAmount` vs `docGrossAmountR` vs `docFinalAmount`; per-indicator totals as Σ of item totals. The
   rounding mode and scale are **not published** — whatever is chosen stays provisional, and `E1020`/`E10201`–
   `E10203` is what a wrong choice looks like in production.
6. **The serializer.** Write the real shape: `{requestTimestamp, document: {header, docReferences, seller, buyer,
   docPayment, docItems, docTotals, vatTotals}}` with `voidReason` / `correctionReason` as siblings of `document`.
   Decide explicit `null` versus omission — the examples write `null` throughout and the gateway's tolerance is
   unpublished; following the examples is the defensible default.
7. **Transport.** Fix the send path to `/JSONReceiver/api/v1/sales-invoices/send`; wrap the JWS in
   `{requestTimestamp, jws}`; add `X-EUJP-ID`, `X-EDB`, `X-SERIAL-NUMBER`, `X-DOC-TYPE-CODE`, which means
   `UjpClient` now needs an EUJP id, an ЕДБ and the certificate serial as configuration; source
   `requestTimestamp` from `GET /api/v1/server-time` (±5 minutes, Skopje time); add a 1 request/second throttle.
8. **Status.** Replace `status(String euid)` with `POST /documents/sales-invoice/current-status` over a signed
   body, plus the history call. Replace `SubmissionStatus` with the gateway's `{statusCode, statusName}` pair —
   and, because the code list is *served* (`GET /api/v1/document-statuses`), prefer carrying the code over a
   closed enum that goes stale.
9. **Submit response.** `qr_link` not `qrLink`; `status` is an integer; add `timestamp` and `e_invoice_user`. The
   error body shape for send is unpublished — keep whatever is parsed marked provisional.
10. **Golden files.** All four are regenerated, and the "an INVOICE serializes byte for byte as in 0.1.0"
    guarantee ends, because `docType` is mandatory on every document. Add per-document-type goldens derived from
    D3 (100, 110, 120, storno, correction, advance) and keep `scripts/verify-jws.py` as the independent check.
11. **Sweep `@ProvisionalSpec`.** Remove each marker with a citation to D1/D3/D4/D5 in the same change.
    **`PRODUCTION_BASE_URL` keeps its marker** — still unstated. So does the RS256 choice, `x5c`, the rounding
    convention, the null-versus-omit choice, and the natural-person buyer shape.
12. **Do not lift the `UjpClient` ban with this release.** Verification against the spec is one of three
    preconditions; a registered qualified certificate and a granted privilege set are the others, and neither is
    in hand. The rule that the submission client is never called until the specification is verified should stay
    exactly as written, in this library and in every consumer that carries it.

### BREAKING for a consumer pinned to 0.2.0

Everything in this list breaks either compilation or the bytes on the wire. A 0.3.0 is not a drop-in.

- **Every serialized field name changes.** Any consumer asserting on serializer output, or holding stored JSON
  produced by 0.2.0, is invalidated. Stored 0.2.0 documents are not convertible without the data `model` never
  captured (street number, payment type, tax indicator, turnover date).
- `UjpClient.Builder` gains **required** configuration (EUJP id, ЕДБ, certificate serial). Existing construction
  code stops compiling.
- `UjpClient.status(String)` changes verb, path and semantics; its return type changes with `SubmissionStatus`.
- **`SubmissionStatus` is removed or replaced** by a code/name pair. `PENDING`/`ACCEPTED`/`REJECTED` map to
  nothing.
- **`SubmissionResult` changes shape** — `qrLink` renamed, `status` no longer an enum, `timestamp` added.
- **`VatCategory` constants change their wire values**, and `EXEMPT` splits into several tax indicators — a
  semantic break that compiles silently if handled carelessly. Guard it: rename or remove `EXEMPT` rather than
  quietly redefining it.
- **`DocumentType`'s wire codes change** (`CREDIT_NOTE` → `110`, `DEBIT_NOTE` → `120`), and `INVOICE` stops being
  expressed by absence.
- **`DocumentReference` / `correctedInvoice` is replaced** by a list of typed `docReferences`.
- `Invoice`'s canonical constructor and builder change with the added mandatory fields; the totals reconciliation
  rule changes with the formulas.
- Golden files change, so any downstream fixture copied from them breaks.

Given the breadth, **0.3.0 is the wrong number for it** — consider 1.0.0, or a parallel package for the verified
shape, so a consumer can migrate deliberately rather than discovering it at the first `E4019`.

---

## 6. Provenance

Everything called CONFIRMED, DIFFERS or ABSENT above rests on D1–D6, official pages and PDFs served by
`efakturawiki.ujp.gov.mk`. No third-party summary, vendor blog or AI-generated description was used as evidence.
Three such sources surfaced during the search (`earhiva.mk`, `efakturawin.mk`, `loginsystems.biz`); they were used
only to locate the official wiki and are **not** evidence for any verdict here. The one claim I take from a
third-party page and label as such: `efakturawin.mk`'s integration manual describes the same send endpoint and
`euid` + `qr_link` + `timestamp` response — consistent with D1, and cited only as corroboration of documents I
read directly.
