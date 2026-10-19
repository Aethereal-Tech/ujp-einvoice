# Specification comparison, 2026-10-19 — what moved since 2026-09-13

A freshness check of the official documentation against `comparison-2026-09-13.md`. Read that one first: this file
records only what changed, what the earlier pass missed, and what is still open. It changes no conclusion of the
earlier one — the wire layer is still wrong at the root, and every step of `changes/wire-layer-correction` still
applies — but it adds fields, statuses, document types and rules that step list did not know about.

**No request of any kind was sent to `efakturatest.ujp.gov.mk`, `efaktura.ujp.gov.mk` or any API or Swagger URL.**
Everything below was read from `https://efakturawiki.ujp.gov.mk` (pages and `/downloads/*.pdf`) and, for news only,
`https://www.ujp.gov.mk`, fetched with `curl` on 2026-10-19. The copies, their hashes and HTTP metadata are kept
outside this repository, as the 09-13 copies were; this file is the record.

Source shorthand follows the 09-13 comparison: **D1** the „API Спецификација" page (`/тест_апи`), **D3'** the
08.10.2026 JSON examples (`primer_za_json_08.10.pdf`, which replaces D3), **D4** the code lists, **D5**
`/error-codes`, **D6** the access and certificate page; plus the per-document-type pages (`/100-faktura` … `/999-…`),
`/json-struktura`, `/status`, `/danocni-indikatori`, the FAQ `/ЧПП` and the site's own changelog `/известувања`.

---

## 1. TLS state

Unchanged in kind, renewed in detail.

- Server still sends **only the leaf certificate** (`openssl s_client -showcerts` prints 1 cert). `verify error:num=20`
  then `num=21: unable to verify the first certificate`, `Verify return code: 21`.
- Leaf: `CN=efakturawiki.ujp.gov.mk`, issuer `C=US, O=Let's Encrypt, CN=YR1` (baseline: YR2),
  `notBefore=Sep 14 08:25:13 2026 GMT`, **`notAfter=Dec 13 08:25:12 2026 GMT`** (baseline leaf expired 2026-09-23;
  it was renewed 2026-09-14 and the chain was NOT fixed).
- `curl` with the macOS trust store: `http=200 ssl_verify=0`, remote IP 146.255.93.226. No `-k` used anywhere.
- Next expiry 2026-12-13. If it renews without the intermediate, the same symptom returns.

## 2. Wiki engine and how change was detected

The host is **Wiki.js** (nginx; pages carry `updated-at`, `page-id`, ETag but no `Last-Modified`). Page history
(`/h/...`), source (`/s/...`) and `?action=history` all return the ~2 KB login shell, so per-page history is not
public. `Special:RecentChanges` is 404, `/sitemap.xml` 404, `robots.txt` empty. The substitute is the **public
changelog on `/известувања`** (dated entries, 02.01.2026 to 16.10.2026), plus each page's `updated-at` and each PDF's
`Last-Modified`/metadata. Probes for `api-documentation-public9.pdf`, `-public10.pdf`, `api-documentation.pdf`: 404.

The Wiki.js sidebar now has 42 items (links, headers, dividers). New since baseline: **23 per-document-type pages** under
"Упатство по Тип на Документ" (`/100-faktura` ... `/999-povrzani-dokumenti`), `/status`, `/danocni-indikatori`,
`/известувања`, `/ЧПП`, `/почетокнакористење`, `/browser-extension`, `/eFaktura-WebAPP`, `/desktop-app`. Most
document-type pages were last edited **2026-10-14**.

## 3. Documents: baseline vs today

| # | Document | Baseline (09-13) | Today (10-19) | Verdict |
|---|---|---|---|---|
| D1 | `/тест_апи` API Спецификација | no date; examples to 2026-09-01 | `updated-at` **2026-10-16T14:36:59Z**, 109,961 B, sha256 `02b2eb9b...` | **CHANGED** (the same wire contract; link now to `primer_za_json_08.10.pdf`; wording "Линк за упатства: api-documentation.pdf" is link text only, href is `-public8`) |
| D2 | `downloads/api-documentation-public8.pdf` | 35 pp, Title `API-Documentation-Public8` | 35 pp, same Title, Last-Modified 2026-08-20, 470,109 B | **UNCHANGED** (same revision label; the 09-13 pass recorded no hash to compare). Lags D1: lacks `/purchase-tax-indicators*`, says `/reject-reasons` where D1 says `/reject-reason` |
| D3 | `primer_za_json_01.09.pdf` | 71 pp, 01.09.2026 | still served (Last-Modified 2026-09-07), 71 pp | **SUPERSEDED** |
| D3' | **`primer_za_json_08.10.pdf`** | n/a | **NEW**, 71 pp, Last-Modified 2026-10-08, 621,167 B, sha256 `1011fc23...`; now the only one D1 links | **NEW** (see section 4.6 for what differs from 01.09) |
| - | `primer_za_json_3.pdf` | not in baseline | 23 pp, CreationDate 2026-07-29, 271,144 B; linked from `/известувања` | **NEW to the record**, interim (older than 01.09) |
| D4 | `sifrarnici_24042026.pdf` + `/шифрарници` | 8 pp, CreationDate 2026-04-28 | PDF identical metadata (8 pp, 2026-04-28, Last-Modified 2026-06-13). **HTML page `updated-at` 2026-10-16T07:52Z** | PDF **UNCHANGED**, HTML **CHANGED** |
| D5 | `/error-codes` | no date | `updated-at` **2026-10-05T21:17Z** | **CHANGED** (new codes, section 4.5) |
| D6 | `/ДоделувањепристапвоеУЈП` + `pristap_eujp.pdf` | not stated | page `updated-at` 2026-10-14; PDF 4 pp, Last-Modified 2026-06-13 | page **CHANGED** (privilege list), PDF **UNCHANGED** |
| D7 | `primer_za_json_2.pdf` | 16 pp, CreationDate 2026-04-29 | 16 pp, 2026-04-29 | **UNCHANGED** |
| - | `upatstvo_efaktura_opsta_24042026.pdf` ("Верзија 2 (24.4.2026)") | not listed | 16 pp, Last-Modified 2026-06-13 | existed before baseline; **not in the baseline inventory** |
| - | `danocni_indikatori_27072026.pdf`, `upatstvo_za_danocni_indikatori_27072026.pdf` ("Верзија 3 (27/07/2026)") | not listed | 1 pp / 4 pp | exist; HTML `/danocni-indikatori` (updated 2026-08-27) is newer than the PDFs |
| - | `/ЧПП` FAQ | not listed | `updated-at` 2026-06-09 | pre-baseline page the baseline missed (carries the JWS algorithm, section 5) |
| - | `/status` | not listed | `updated-at` 2026-10-15 | not in baseline inventory (the 11.09 changelog entry already links it), 10 statuses |
| - | WebApp manual, desktop/browser-extension guides | not listed | PDFs dated Jun-Aug 2026 | not wire-relevant; the desktop client link points to an `.exe` on `accent.mk` (not fetched) |

`www.ujp.gov.mk` (news only): front page and `/mk/javnost` through 2026-10-19 contain no e-Faktura item (tourism
campaign and a SEE fiscal-reform meeting only).

## 4. What changed (dated by the site's own changelog, `/известувања`, p. "Промени во системот еФактура")

Entries from 11.09 onward that the baseline does not reflect (11.09 itself predates the 09-13 baseline date, and the baseline lacks it):

| Date | Change (paraphrased) | Wire impact |
|---|---|---|
| 11.09 | Statuses **12 Сторно** and **13 Корективна** added | `/status` now lists 00,01,03,04,05,07,09,10,12,13 |
| 14.09 | New header field `docAlt` (UJP01-16); item fields `docItemBasePriceWoVat`, `...Add`, `...Deduct`, `docItemBasePriceType` (UJP07-22..25); totals `docAmountIncrease`/`docAmountDecrease` (UJP08-11/12); new `docIndicator` values | Formula added: `docGrossAmountR + docAmountIncrease - docAmountDecrease = docFinalAmount` and `base + add - deduct = docItemUnitOriginalPriceWoVat` (also in D1) |
| 14.09 | New `docIndicator` values: `PN`, `PI` (200/210), `TUR`, `DOBRA` (660), `KONECNA` (100), `KOMISIONA` (100/900) | |
| 15.09 | New tax indicators (not listed on the page) | |
| 02.10 | Browser plugin 5.6, NativeApp 2.9, "Нови error codes" | |
| 06.10 | New privilege "Регистрација на интернет продавници (IRS)" | |
| 08.10 | **Document-level `docAttributes` and item-level `attributes`**: `{name, value, type}`, type `STR`/`DEC`/`INT`/`DATE`; "Преглед на причина за одбивање" in sent-docs report; field sizes changed (`docTypeName` 100, `docAlt` Integer 4, `docTypeRef` 20, `postalCode` 10, `docItemVat` Decimal 4.2, `vatPercent` 4.2 ...); `swagger.json` download promised | New optional arrays in the payload; error codes `E5018`/`E5019`/`E1035` (the 02.10 "new error codes" are not itemised) |
| 15.10 | Attributes group added to 100; **`docCurrencyDate` and `docCurrencyExchRate` removed from 100**; 11 new reference-document types (`RMP`, `ZPPUJN`, `PPPNS`, `ZNED`, `ZNPD`, `PPPV`, `ZPNUJN`, `PNNS`, `DPPN`, `RPNKV`, `PIP`); invoice-level discount "се распределува на сите ставки подеднакво" (spread over items, sent per item) | Currency block shrinks for type 100 |
| 15-16.10 | Browser plugin 5.8, NativeApp 3.0 | |

Entries dated **before** the baseline but relevant to the contract (the baseline did not record them): 28.04.2026
(`requestTimestamp` moved inside the signed JWS, document in a `document` object, `docGrossAmountR` etc. become integers),
27.05 (`/server-time`; `requestTimestamp` format `2026-01-05T12:00:00`, Skopje zone; data services become POST with a
`{"jws": ...}` body; `.../sales-invoice/cancel` added), 24.06 (cancel service **removed**; `X-SERIAL-NUMBER` mandatory wherever a JWS is
used; `current-status`, `payload/list`, `payload/date-range` added; `e_invoice_user` in the send response;
`docItemTotalOriginalPriceWoVat` no longer mandatory), 28.07 (110/120/130/150/160/200/240/620/630/900 types, correction
reasons, purchase tax indicators, `docStorno=2`), 27.08 (500/510/610/650/910/999/310/170/660/220/230/320 types; ~100
new tax indicators; desktop app).

### 4.1 Document types
Pages exist for 100, 110, 120, 130, 150, 160, 170, 200, 210, 220, 230, 240, 310, 320, 500, 510, 610, 620, 630, 650,
900, 910, 999. The changelog also names **660** (no page) and the 08.10 PDF carries a **640 "Порамнување за ДДВ"**
example (no page, not in the changelog).

### 4.2 Field matrix, now published per type
Each type page has the full table: code `UJPnn-nn`, name, type, length, `З/О`, and the legend appears on
`/json-struktura`: "З = задолжително поле / О = опционално поле / Ш = шифра / П = назив" (ЗШ, ЗП, ОШ, ОП combos).
The same legend is in `upatstvo_efaktura_opsta_24042026.pdf` (v2, 24.4.2026), so the legend predates the baseline;
the baseline had not read that PDF. Notable markers on the 100 page (2026-10-16): `docAlt` З, `docIndicator` З,
`docAttributes` З (but the group text says the fields are optional), `docPaymentTypeCode` ОШ, `docCurrencyCode` ОШ,
`buyerTin` **ЗШ\*** (the asterisk has no footnote in the extracted text), `buyerAddress` З.

### 4.3 Rounding (was "not published")
`/json-struktura` now states it: "Заокружувањето е со нормално ROUND_HALF_UP" (items up to 4 decimals unrounded; item
values summed unrounded into tax totals and rounded to **2 decimals HALF_UP**; document totals the same;
`UJP08-08/09/10` are integers; tolerance "до 0.1"; empty or absent amount field "се третира како 0"). **Internal
contradiction:** the 28.04 changelog says `docGrossAmountR` is "банкарско заокружување" (banker's rounding) of
`docGrossAmount`. The changelog dates the first rounding explanation to **06.02.2026**, so this is a baseline gap
more likely than a post-baseline change.

### 4.4 VAT codes and tax indicators
Rate groups unchanged: `DDV-A` 18, `DDV-V` 10, `DDV-B` 5, `DDV-G` 0 (service `/api/v1/tax-groups`, page
`/danocni-indikatori`). `vatImpact` still `STANDARD`/`NULA`/`OSLOBODEN`/`PRENESEN`. What grew: the indicator list is
now **~104 codes** on the page (`DDV-7-I`, `DDV-8-2`...`DDV-8-Z`, `DDV-9-1`...`DDV-9-DR`, `DDV-10-13-*`,
`DDV-10-14-*`, `DDV-11-*`, `DDV-12-4/5`, `DDV-14-4/5`, `DDV-30-P/D`, `DDV-34/35/36` for purchase deduction). New
indicator fields `currencyType` (1 MKD only, 2 foreign only, 3 all) and `vatType` (1 VAT payers, 2 non-payers, 3
all). Per-indicator `note` text is required in `vatTaxIndicatorNote` (E1032).

### 4.5 Error codes (`/error-codes`, three namespaces, still not merged)
Present now and not recorded in the baseline (some may predate it): `E1033 Invalid DocIndicator`, `E1034 Invalid DocAlt`, `E1035 Invalid
Attribute`, `E500719 docIndicator mismatch`, `E500720 docAlt mismatch` (JSON Receiver); `E5018 Attribute value
validation failed`, `E5019 Attribute type is required`, `E5014`-`E5017` (purchase VAT totals, ASiC) (eInvoiceApi).
Confirmed unchanged: `E1012 Certificate not found`, `E5004 Certificate validation failed`,
`E10001 Missing TIN...`, `E10002`, `E10003`, `E1020`/`E10201-E10203` (invalid totals), `E4016`/`E4020`-`E4023`
(headers), `E40122 ... greater than 5 minutes`, `E4007 Rate limit exceeded`, `E1023 Invoice already exists for that ID`,
`E1024`-`E1031`. Still no body shape for an error (section 6).

### 4.6 `primer_za_json_08.10.pdf` vs `01.09.pdf`
Same 71 pages, same table of contents (incl. a 640 example). A normalised diff shows **one example replaced**: the
"Пример за ослободен од данок" (p. 10-11) now uses `DDV-8-2`, buyer = Министерство за финансии/УЈП (`buyerTin`
4030000404218, empty `buyerVatNumber`), `docPaymentTypeCode: null`, and carries fields that appear in **no field
table anywhere**: `sellerEinvoiceUser`, `buyerEinvoiceUser`, `docItemUnitItemDiscountAmount`, `docGlobalDiscountAmount`,
plus `docAlt: false` (boolean, while the table says Integer 4), `docIndicator: null`, `docItemHs: 1`,
`docAmountIncrease/Decrease: 0`. This example is wrapped differently: `document`, then `voidReason`, `correctionReason`,
`"source": "WebApp"` and `requestTimestamp` **after** `document`. `"source"` also appears in the 01.09 PDF; neither
document-level text nor any table explains it. **`docAttributes`/`attributes` appear in no PDF example**, although the
08.10 changelog introduces them in the same entry as the extended examples.

## 5. Status of each baseline "NOT published" item

| Baseline item | Today | Evidence |
|---|---|---|
| Machine-readable schema on the docs host | **Still absent.** The changelog (08.10) promises "Можност за download на swagger.json", but no link or file exists on `efakturawiki`; the only Swagger URL is still `efakturatest.../swagger-ui/index.html` (API host, not touched) | D1 API Documentation section; `/известувања` 08.10 |
| Field optionality matrix | **PUBLISHED, changed.** Full per-type tables with З/О and a legend (23 pages, edited 2026-10-14). Caveats: `ЗШ*` asterisks unexplained; D4 PDF still the partial 2026-04-28 one; 170 page lists `docType` code as "????" and `buyerName` as `ЗШ` (template slips) | `/100-faktura` etc. |
| Production base URL | **Still absent.** Every URL on the host is `efakturatest` / `eujptest`. Also no "production" wording at all; the programme is described as test "Трета фаза" (web app, ~200 selected testers) | `/известувања`, `/почетокнакористење` |
| Error response body shape | **Still absent.** All JSON examples show `"errorStatus": null`; no non-null example, no `errorCode` field named anywhere | grep over every fetched page and PDF |
| B2C / natural-person buyer | **Shape now published, example still absent.** Type **170 "Фактура кон физичко лице"**: no `buyerTin`, no `buyerCCode`; `buyerName` ("име/презиме на корисникот"), `buyerAddress` (street, number, postalCode, city all mandatory) and optional `buyerEmail`. Status **10 Евидентирана** exists "за која примачот не е во можност да ја прифати/одбие (... физичко лице)". Type **510** (foreign person) has no `buyerTin` but a `buyerForeignTin` marked `З*`. No 170 example in any PDF; its `docType` code is still printed as "????" | `/170-faktura-kon-fizicko-lice`, `/status` |
| TLS chain problem | **Still present** (section 1) | |

## 6. Status of each wire finding

| Finding | Today |
|---|---|
| Envelope `{requestTimestamp, jws}` | **Confirmed same.** D1: `"requestTimestamp"` ... `"jws": "string"`. Status/data services now documented as a JWS in a single parameter `{"jws": "..."}` (27.05) |
| `requestTimestamp` format/skew | **Same.** Only `2026-01-05T12:00:00` Skopje time, `E40122` > 5 minutes. `GET /api/v1/server-time` present |
| Headers | **Same.** `X-EUJP-ID`, `X-EDB` everywhere; `X-SERIAL-NUMBER` wherever a JWS is used; `X-DOC-TYPE-CODE` on send. 1 request/second/user |
| JWS algorithm / x5c | **Algorithm now stated** (FAQ `/ЧПП`, updated 2026-06-09, i.e. pre-baseline): header `{"alg":"RS256","typ":"JWT"}`, "signature = RSA-SHA256(...)". `x5c` still not mentioned; the certificate is identified by the registered cert + `X-SERIAL-NUMBER` (`E5012`). Certificates: "сите валидни ... издадени од даватели на квалификувана доверлива услуга" in North Macedonia (software or USB token); the FAQ names no specific CA |
| Nested payload | **Same.** `document{header, docReferences, seller, buyer, docPayment, docItems, docTotals, vatTotals}`, `voidReason`/`correctionReason` siblings. New: optional `docAttributes`; item `attributes`; the examples add an undocumented `source` and trailing `requestTimestamp` (4.6). Examples are inconsistently wrapped (the 110 example starts at `header` with no `document`/`requestTimestamp`) |
| Submit response | **Same plus detail.** `euid`, `message`, `qr_link`, `status` (integer 200), `e_invoice_user`, `timestamp` |
| Status polling | **Same endpoint**, `POST /api/v1/documents/sales-invoice/current-status` (JWS body `{requestTimestamp, euid}`), history `.../status` with `dateFrom`; `docStatus` carries totals (24.06). Added `.../invoices-status`; `.../cancel` came and **went** (27.05 then 24.06) |
| Status code list | **Grew:** 00 Нацрт, 01 Испратена (Нова), 03 Прифатена, 04 Автоматски прифатена, 05 Одбиена, 07 Сторнирана, 09 Корегирана, 10 Евидентирана, 12 Сторно, 13 Корективна |
| docType 100/110/120 | **Same** (`Фактура`, `Книжно одобрение`, `Книжно задолжение`) plus 130 Авансна фактура and 20 more codes. `docReferences` is now marked **З (1..n)** on 110 and 120 while the 110 example sends `[]` (contradiction) and is О on 100 |
| docStorno / docReferences | **Same.** 0/1/2; storno = negative amounts + `voidReason`; correction = replacement document + `correctionReason`; both reference the original with `docTypeRef: "ST"` (`E1029` forbids `ST` on some types). Status 12/13 now name the storno/correction documents |
| VAT category codes | **Same** (section 4.4) |
| Currency block | **Changed for 100:** `docCurrencyDate`/`docCurrencyExchRate` removed (15.10) though the 08.10 PDF examples still carry them; `docCurrencyCode` already non-mandatory and "ќе се исфрли" in future (28.07) |
| New endpoints | None dated after the baseline. The post-baseline entries add fields, statuses, codes, attributes, no routes. (Routes added earlier, not in baseline's list: `purchase-tax-indicators`, `correction-reason`, `payload/list`, `payload/date-range`, `invoices-status`, `purchase-vat-totals`) |
| Receiving/purchase invoices, credit notes, cancellation | Purchase side: accept/reject, PDF, payload, ids, changes, statuses, VAT-deduction totals all documented. Credit/debit notes: 110/120 pages exist. Cancellation: only via storno (`docStorno=1`); the `/cancel` service was removed 24.06 |
| Go-live / mandatory dates / phases | **No dates.** Three test phases only (1: ERP integration test, 2: API pull/accept/reject/storno services, 3: web-app test with ~200 companies). Home page: the draft e-Faktura law is on ENER for public consultation; no effective date anywhere |
| Certificate / access requirements | **Same, more detail.** Registration at `eujptest.ujp.gov.mk/ureg`; 18 privileges incl. new IRS; certificate registered per company through the web app; signer must be authorised (`E1013`); FAQ says the cert may be issued to a natural person without a company |

## 7. Still open

1. Production base URL (and the production Swagger/receiver paths).
2. Machine-readable schema: promised as `swagger.json` download (08.10) but only on the API host; a person must fetch it.
3. Error response body for `/JSONReceiver/.../send` and for `einvoice_api` (non-null `errorStatus`).
4. Natural-person buyer: the `docType` code for 170 ("????"), a worked example, and whether the `ЗШ*` asterisks mean "conditional" and on what.
5. Null versus omitted keys (only the numeric amount rule "празно или не се прати" is stated).
6. Rounding contradiction: "банкарско" (changelog 28.04) vs `ROUND_HALF_UP` (guide).
7. Undocumented payload members: `source`, trailing `requestTimestamp`, `sellerEinvoiceUser`/`buyerEinvoiceUser`, `docItemUnitItemDiscountAmount`, `docGlobalDiscountAmount`; `docAlt` type (Boolean in changelog, Integer 4 in tables, `false` in example).
8. `docAttributes` / `attributes`: no PDF example; 08.10 changelog says there is one.
9. `docReferences` mandatory on 110/120 versus `[]` in the example.
10. `x5c` / certificate-chain placement in the JWS header; named issuing CAs.
11. 660 and 640 document types: no page; 15.09 and later tax indicators not on the page.
12. Go-live date and mandatory-use phases (law still in consultation).
13. TLS leaf expires 2026-12-13 with chain still incomplete.

## 8. Provenance

Everything above is read from `efakturawiki.ujp.gov.mk` pages and PDFs fetched 2026-10-19 (and `www.ujp.gov.mk` for
news); nothing was executed from them. Where the baseline
and today differ the check could not always tell "changed since 09-13" from "missed on 09-13" (the legend, the RS256 FAQ and
the rounding rule are of the second kind); the site's changelog dates are the only history available. `/diagrams/prakjanje_json.png` (the D1 flow chart) was read: on failed validation it shows only "Враќање на грешка", no body shape. The check did not
download `EInvoiceNativeAppSetup.exe` or the browser extensions linked on the wiki.
