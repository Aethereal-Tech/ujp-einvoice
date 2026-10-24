# Specification comparison, 2026-10-24 — what moved since 2026-10-19

A freshness check of the official documentation against `comparison-2026-10-19.md`, which ended at the changelog entry of
16.10.2026. Read that file and `comparison-2026-09-13.md` first: this one records only the three changelog entries dated
19.10, 21.10 and 22.10, the example set they replaced, and what each changed in the library. It changes no conclusion of
the earlier ones — the envelope, the headers, the nested `document`, the status calls and the four VAT groups are
untouched — and it completes task 9.3 of `changes/wire-layer-correction`.

**Nothing here touches `efakturatest.ujp.gov.mk`, `efaktura.ujp.gov.mk` or any API or Swagger URL.** The sources are three
documents of `https://efakturawiki.ujp.gov.mk`, fetched on 2026-10-24: the changelog page `/известувања`, the API
specification page `/тест_апи` (D1) and `downloads/primer_za_json_21.10.pdf`. The copies, their hashes and the extracted JSON
are kept outside this repository, as the earlier ones were; this file is the record. Nothing fetched was executed.

Source shorthand follows the earlier comparisons: **D1** the „API Спецификација" page, **D3'** the 08.10.2026 JSON
examples, **D3''** the 21.10.2026 set that replaces them.

---

## 1. Documents: 2026-10-19 against today

| # | Document | 10-19 | Today (10-24) | Verdict |
|---|---|---|---|---|
| D1 | `/тест_апи` | `updated-at` 2026-10-16T14:36:59Z, 109,961 B, sha256 `02b2eb9b...` | `updated-at` **2026-10-22T14:52:33Z**, 109,961 B, sha256 `54fbf9b2...` | **CHANGED**; the size is the same, and the one difference read is the example link, now `json_primeri_21.10.2026.pdf` (the page was not diffed further). D1's sample code list still shows only `SYS`, so it says nothing of `ST` or `KR` |
| D3' | `primer_za_json_08.10.pdf` | 71 pp, `Last-Modified` 2026-10-08 | not re-fetched | **SUPERSEDED** by D3'' |
| D3'' | **`primer_za_json_21.10.pdf`** | n/a | **NEW**, 87 pp, `Last-Modified` 2026-10-22T14:52:11Z, 839,104 B, sha256 `60cdaa1a...2f26ac28`; the only set D1 links | **NEW** (sections 3 and 4) |
| - | `/известувања` | entries to 16.10 | `updated-at` 2026-10-22T15:04:25Z; entries 19.10, 21.10 and 22.10 are new | **CHANGED** (section 2) |
| - | the 23 type pages, `/json-struktura`, `/status`, `/danocni-indikatori`, `/error-codes`, the FAQ | as in the 10-19 file | **not re-fetched** | The type pages retrieved on 10-19 still type the five amounts below as Integer; they may have changed since 19.10 |

## 2. The three entries

| Date | Entry (paraphrased) | Wire impact | What changed in the code |
|---|---|---|---|
| 19.10 | `docAvansAmount`, `docFinalAmount`, `docGrossAmountR`, `docAmountIncrease` and `docAmountDecrease` change from Integer to Decimal 15.2. A note adds that decimals are used only for foreign currencies, and that in MKD the sum is a whole number written with decimals ",00" (the entry's example is 102,00) | Five amounts of UJP08-08 to UJP08-12 may carry a fraction in a foreign currency. In MKD the value is unchanged | `Advance` and `Adjustments` take at most two decimals; `Document` refuses a fraction in MKD by name, citing the note; `DocumentTotals` keeps `docGrossAmountR` whole in MKD and at two decimals in another currency (`Money.grossRounded`, provisional); the serializer is unchanged (below) |
| 21.10 | The PDF of JSON examples is replaced | See sections 3 and 4: the 100 examples were rebuilt and six features have examples for the first time | Goldens re-pointed to D3'', six new ones added |
| 22.10 | The reference type for storno and correction is split into `ST Сторно` and `KR Корекција`; both must carry a valid EUID. The same entry adds the parameter `docFilteredBy` to the sales and purchase `invoices-status` calls, with the values `docDate` (filter by the document date) and `docReceivedTimestamp` (filter by the date of receipt) | `docTypeRef` of a storno stays `ST`; a correction's becomes `KR`. `docNameRef` is `Сторно` / `Корекција` | `Mode.Storno` requires an `ST` reference and none of `KR`, `Mode.Correction` a `KR` one and none of `ST`; `RefDocumentType.KR` added and `ST` renamed; `Reference` refuses an ST or KR number that is not EUID-shaped. `docFilteredBy` is **out of scope**: the client implements neither `invoices-status` call |

### 2.1 The rounding of the five amounts

The 19.10 entry types the five fields Decimal 15.2 and gives no rounding rule for the decimals. The library keeps them
at two decimals in a foreign currency and at none in MKD:

- `docAvansAmount`, `docAmountIncrease` and `docAmountDecrease` are the caller's: at most two decimals anywhere, a whole
  number in MKD, refused rather than rounded. `102.00` is whole and accepted. The check sits in `Document`, which knows the
  payment's currency (MKD where there is no payment group or no currency).
- `docFinalAmount` is derived, so it follows.
- `docGrossAmountR` is derived: the unrounded gross rounded to 0 decimals in MKD, as before, and to 2 in another currency.
  **That second half is `@ProvisionalSpec`.** The set has two EUR documents (kind 510, page 63; kind 630, page 76) and
  both have a whole gross (300 and 295), so neither shows whether UJP rounds a foreign gross to a whole number or keeps it
  at two decimals. They also print `docCurrencyExchRate` as a string (`"61.53"`, `"55"`), where the 310 example prints the
  number 1; no scoped kind is affected.
- `Money.whole`'s question (banking against half-up) is **narrowed, not settled**: in a foreign currency the field now
  keeps decimals, so only the MKD rounding remains in doubt. The 21.10 multi-item prints a gross of 667.5 (from 667.5032)
  and a `docGrossAmountR` of 668; 668 is also the banking-rounded value of 667.5, so it settles nothing. No other example
  value is a tie.

### 2.2 How the examples print the amounts

Every MKD amount is a bare integer in the 21.10 JSON: `300`, `668`, `-35`, `154`, `400`, `200`; never `300.00`. A
fractional amount prints its digits: `288.2`, `667.5`, `600.01`. The entry's ",00" is therefore a display and entry
convention (the web app shows `102,00 ден.`), and the JSON number `102` equals `102.00`. The serializer already wrote every
number without trailing zeros, so it needed no change; a test pins `102`, `118` and a foreign `118.47`, `10.5`, `1.1`.

### 2.3 What the split does to the references

A reference of type `ST` or `KR` must carry a valid EUID. The library checks the **shape**: 8-4-4-4-12 hexadecimal digits,
either case, which is what the published EUIDs are (UUID version 7 in the 21.10 examples). It does not pin the version, and
it cannot know whether the document exists. Any other type keeps a free number, as UJP02-01-04 says ("EUID или друг број").
A type is recognised by its code, so a caller holding the old pair `ST` / `Сторно/Корекција` still names an original.
The error catalogue's E1029 ("docTypeRef cannot be 'ST' for this document type", `/error-codes` of 2026-10-05) predates the
split and was not re-fetched; whether it extends to `KR` is unknown.

---

## 3. `primer_za_json_21.10.pdf` against `08.10`

The set grew from 71 to 87 pages and the table of contents now matches the physical pages. Eighteen examples were
extracted (the scoped kinds and modes, the originals of the storno and the correction, the six new ones, and the
unscoped multi-item, art. 32 and exempt ones); the JSON, the page of each, the two string repairs and a path-level diff of
every scoped example are in `ujp-docs-2026-10-24/matrices/examples.md` outside this repository.

**Unchanged, byte for byte in value:** the 110, 120, 130 and 310 examples. Their goldens come out identical.

**Rebuilt:** every kind-100 example. In summary:

- the ordinary 100 is now the **bare body** (08.10: `{requestTimestamp, document}`); the storno and the correction are
  `{document, voidReason}` and `{document, correctionReason}` with **no `requestTimestamp`**; only 310 keeps the full
  envelope;
- new document numbers (`100-2625359277/2026`), dates 2026-10-21 (turnover 2026-10-06 on the ordinary set), a new line
  (`TEST OC 0 0 1`, 254.24 in DDV-A; the storno pair uses `Wireless Mouse`, 29.99 in DDV-A; 08.10 was `Opis5432`, 95.2381
  in DDV-B), payment P10 instead of P11, and every total follows;
- members added to 100: `docReferences` and `docAttributes` (`[]`), `docAlt` (`false`), `docIndicator`, `docDelivery*`,
  `docPeriod*` (null), `docAmountIncrease` / `docAmountDecrease` (0), `docItemUnitItemDiscountAmount` (0),
  `docGlobalDiscountAmount` (0, and 10 in the discount example), `docItemHs` (ordinary set), `sellerEinvoiceUser` /
  `buyerEinvoiceUser`, and `sellerEntityType` / `buyerEntityType` (1, in the correction only);
- members removed from 100: `docCurrencyCode`, `docCurrencyDate` and `docCurrencyExchRate` in `docPayment`
  (`docCurrency` remains); `vatTaxIndicatorNote` is `null` where 08.10 wrote `""`;
- the storno's reference is `ST` / `Сторно` and the correction's `KR` / `Корекција`, each with an EUID-shaped number;
- the correction is reason `C02` with the comment `test` and a quantity of 2 (08.10: `C01`); the storno pair's payment type
  is `null`.

**New, and with an example for the first time:** a reference document (`SYS`), a discount, an increase of the amount to pay,
a decrease of the amount to pay, a document attribute and a line attribute. This closes item 8 of the 10-19 list
(attributes had no PDF example).

## 4. The goldens

Sixteen examples are rebuilt through the model and compared with a golden: the ordinary invoice, the two-group invoice, the
six new ones, the storno and its original, the correction and its original, and the 110, 120, 130 and 310.

**Every printed figure was reproduced.** Each line's unit VAT and four totals, the document totals (including the increase
and the decrease), the VAT totals and `docFinalAmount` equal the number the PDF prints, in all sixteen. There is no
mismatch to report and no expected-failure note. Two lines are worth reading: the correction prints a line VAT of
91.5264, a document VAT of 91.53, a gross of 600.01 (from 600.0064) and a `docGrossAmountR` of 600, and the storno
prints 5.3982, 5.4, 35.39 (negated) and 35. The art. 32 example, which is not a golden, is still not consistent with
itself (`docVatAmount` 0 beside a VAT total of 45.76 under `DDV-11-A-G`), as in 08.10.

Where the library's output differs from the PDF on purpose (a test names each):

| Difference | Why |
|---|---|
| the envelope, with a timestamp, on every kind | the signed payload nests under `document` and carries the timestamp (D1); the examples are bare, or lack the timestamp, on all kinds but 310 |
| `docCurrencyCode` written beside `docCurrency` on 100 | the table lists UJP10-09; the 21.10 examples of 100 print `docCurrency` alone |
| `docAlt` written on 110, 120, 130 and 310 | the tables mark it mandatory; those examples omit it |
| `docAmountIncrease` / `docAmountDecrease` written on 110, 120, 130 and 310 | the 100 table lists them; those examples omit them |
| `sellerEinvoiceUser`, `buyerEinvoiceUser`, `sellerEntityType`, `buyerEntityType`, `docGlobalDiscountAmount`, `docItemUnitItemDiscountAmount`, `docItemHs` not written | in some examples and in no table |
| a line's `attributes` is not written when empty | the 21.10 examples carry the member only on the line that has an attribute (08.10 had no example; the library wrote `[]`); `docAttributes` is `[]` on every 100 |
| `docNameRef` of the `SYS` reference is `Од систем` | the example prints `Фактура`; D1's code list names the type `Од систем`, and the table says the name is the type's according to the list |
| the discount is in `docItemUnitDiscountAmount` and `docDiscountAmount` and not in `docGlobalDiscountAmount` | the 15.10 entry spreads an invoice-level discount over the lines and sends it per line; the PDF prints the 10 a third time in the global member, which is undocumented |

The four-group invoice of 08.10 (the only worked document with all four VAT groups and unrounded line sums that differ from
the rounded ones) is gone from the new set. Its figures stay as a model test (`RoundingTest`), not as a golden.

## 5. Markers

**None removed, one added.** Nothing in the three entries settles a marker: the schema is still unpublished, no example is
a rounding tie and none carries a foreign gross with a fraction.

- **Added:** `Money.grossRounded` (a foreign `docGrossAmountR` at two decimals).
- **Narrowed:** `Money.whole` (MKD only; see 2.1).
- **Text revised, marker kept:** the serializer's class marker (the 21.10 envelope shapes, `*EntityType`, a line's empty
  `attributes`), `header()` (`docAlt`), `appendCurrencyRate`, `totals` and `vatTotal` (the `""` against `null` conflict is
  gone from the examples; whether `null` is the right choice is the schema's), and `DocumentKind.requiresReferences` (the
  110 and 120 examples still send `[]` in 21.10).

## 6. Still open

Items 1-6, 9-13 of the 10-19 list stand. Changed or new:

- **Item 7 (undocumented members)** grows by `sellerEntityType` and `buyerEntityType`, and `docGlobalDiscountAmount` now
  carries a value (10) in the discount example, which suggests it is the invoice-level discount the entry describes. The
  schema decides whether it is sent beside the per-line discount or instead of it.
- **Item 8** is closed: the 21.10 set has a document-attribute and a line-attribute example.
- **The reference name of `SYS`**: `Фактура` in the example, `Од систем` in the code list. The library follows the list.
- **`docCurrencyExchRate`** is a string in the 510 and 630 examples and a number in the 310 one.
- **The foreign `docGrossAmountR`** (2.1).
- **The error catalogue, the type pages and `/json-struktura`** were not re-read on 2026-10-24.

## 7. Provenance

The changelog, D1 and the PDF were fetched on 2026-10-24 from `efakturawiki.ujp.gov.mk` (the PDF's response is dated
Sat, 24 Oct 2026 21:06:10 GMT); nothing was executed from them. The PDF text was read with `pdftotext -layout`, and every
extracted JSON was parsed with the standard library and found verbatim in two further extractions of the same PDF.
