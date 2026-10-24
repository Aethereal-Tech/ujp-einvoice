# Serialization

## Purpose

The `serialization` package turns the `model` into UJP's wire shape without the model ever knowing JSON exists: the
`Serializer` interface, and `UjpJsonSerializer`, the one shipped implementation. It writes the shape the type pages and
the worked examples of primer_za_json_21.10 describe, from the corrected `Document`. This capability records its
mechanical guarantees and, member by member, what it writes and what it leaves out; the confidence of each name is
recorded in `specification-status`. **The 0.1.0 byte-for-byte guarantee ended with the model**: no document written by
0.1.0 or 0.2.0 reproduces, and the goldens of that shape are gone.

## Requirements

### Requirement: Serializer is an interface; UjpJsonSerializer is the one shipped implementation
`serialization` SHALL expose `Serializer`, whose `serialize(Document, LocalDateTime requestTimestamp)` returns the wire
bytes and whose `contentType()` names their MIME type, and `UjpJsonSerializer` SHALL be its one shipped implementation.
The request timestamp is an input: the gateway refuses one more than five minutes from its own clock, so the transport
sources it from the server-time service. An entirely different format (UBL 2.1, say) is a new `Serializer`
implementation.

#### Scenario: The timestamp keeps its seconds
- **WHEN** the request timestamp is 09:05:00
- **THEN** it is written `2026-10-20T09:05:00`, not `2026-10-20T09:05`

### Requirement: the envelope nests the document and puts the reason beside it
The output SHALL be `{requestTimestamp, document: {header, docReferences, seller, buyer, docPayment, docItems,
docTotals, vatTotals, docAttributes}}`, for every kind, with `voidReason` (`voidCode`, `voidReason`, `voidComment`) on
a storno and `correctionReason` (`correctionReasonCode`, `correctionReason`, `correctionReasonComment`) on a correction
as siblings of `document`, and only the one that applies. `docPayment` SHALL be left out on 110 and 120, which have no
payment group, and `docAttributes` and line `attributes` on every kind but 100. A line's `attributes` is written only
when the line has some: the 21.10 examples carry the member on the one line that has an attribute and on no other,
while `docAttributes` is `[]` on every 100. The 21.10 examples print the bare body on 110, 120, 130 and the ordinary
100s, a `document` with its reason and no timestamp on the 100 storno and correction, and the full envelope on 310
alone, which writes both reasons as `null`; the envelope on every kind and the single reason are `@ProvisionalSpec`.

#### Scenario: A storno writes voidReason beside the document
- **WHEN** a storno is serialized
- **THEN** the top-level members are `requestTimestamp`, `document` and `voidReason`, in that order

#### Scenario: A credit note has no payment group
- **WHEN** a 110 is serialized
- **THEN** `document` has no `docPayment` member

### Requirement: an absent scalar or object is null and an empty list is []
An absent scalar or object SHALL be written as `null` and an empty list as `[]`, as every example writes them. The
choice stays `@ProvisionalSpec` until the gateway's schema is read: the guide's one explicit rule (empty or not sent
counts as 0) covers amounts only. A member of an optional group that no example carries (a party's subsidiary, a line's
base price) is left out when absent and not written as `null`. An absent note on a standard indicator is `null`, as
every 21.10 example writes it (the 08.10 examples of 100 wrote `""`).

#### Scenario: An absent delivery date is null
- **WHEN** a header has no delivery date
- **THEN** `docDeliveryDate` is written as `null`

### Requirement: no member the tables do not document is written
The serializer SHALL NOT write `source`, a trailing `requestTimestamp`, `sellerEinvoiceUser`, `buyerEinvoiceUser`,
`sellerEntityType`, `buyerEntityType`, `docGlobalDiscountAmount`, `docItemUnitItemDiscountAmount` or `docItemHs`. They
appear in some of the examples and in no table; the two `*EntityType` members are new in the 21.10 correction example.
An invoice-level discount is spread over the lines by the caller and sent per line, as the 15.10 changelog says: the
21.10 discount example prints 10 in `docItemUnitDiscountAmount` and `docDiscountAmount`, and also in
`docGlobalDiscountAmount`, which is not written. They are open questions for the schema, not decisions.

#### Scenario: The 310 example's source member is not written
- **WHEN** the cash turnover of UJP's example is serialized
- **THEN** the output has no `source` member and no second `requestTimestamp`

### Requirement: the currency date and rate follow the kind
`docCurrencyDate` and `docCurrencyExchRate` SHALL be left out on 100 (removed on 15.10; the 08.10 examples still
carried them and the 21.10 ones do not) and written on 130 and 310, whose tables list them: the document's own date and a rate of 1 beside MKD, as
the examples print, and `null` for both beside another currency, the model holding no rate. 110 and 120 have no payment
group. `docCurrency` and `docCurrencyCode` carry the currency code; the 21.10 examples of 100 print `docCurrency`
alone while the table lists `docCurrencyCode` (UJP10-09), so both are written. This is `@ProvisionalSpec`.

#### Scenario: Kind 100 writes no currency date
- **WHEN** an invoice with a payment is serialized
- **THEN** `docPayment` has `docCurrency` and `docCurrencyCode` and no `docCurrencyDate` or `docCurrencyExchRate`

#### Scenario: Kind 310 writes the date and a rate of one
- **WHEN** a cash turnover in MKD is serialized
- **THEN** `docCurrencyDate` is the document date and `docCurrencyExchRate` is 1

### Requirement: docAlt is a JSON boolean and is written for every kind
`docAlt` SHALL be written after `docType` as a JSON boolean, `false` unless the header sets alternative mode. The 14.09
changelog types it Boolean, the 08.10 tables Integer(4), and every 100 example of 21.10 sends `false` (the 110, 120,
130 and 310 examples omit it); the tables mark it mandatory on all five kinds. It is `@ProvisionalSpec` until the schema settles its type.

#### Scenario: docAlt is false by default
- **WHEN** any document is serialized
- **THEN** `header.docAlt` is `false`

### Requirement: numbers are written without trailing zeros, in a fixed member order
Every number SHALL be written as a JSON number without trailing zeros, so 100.0000 is written `100`, 3417.10 `3417.1`
and 4.7619 `4.7619`: how every example prints its figures. That includes the five amounts that became Decimal 15.2
on 19.10.2026: the 21.10 examples print an MKD sum as `300`, `668`, `-35` or `154`, never `300.00`, and a fractional
one as `288.2`, so the entry's ",00" (e.g. 102,00) is a display convention the JSON does not carry, and a foreign
amount of 118.470 is written `118.47`. Member order within each object is fixed, following the
tables' UJP codes, because the examples disagree among themselves (`docStorno` is first in some and last in others) and
the signed bytes are ours either way. The same document serializes to the same bytes.

#### Scenario: A gross of 3417.10 is written 3417.1
- **WHEN** a document whose gross is 3417.10 is serialized
- **THEN** `docGrossAmount` is `3417.1` and `docGrossAmountR` is `3417`

### Requirement: the worked examples are goldens, and every derived amount equals the number the PDF prints
For each shipped kind and mode — 100 ordinary, 100 storno and its original, 100 correction and its original, 110, 120,
130 and 310 — and for each feature the 21.10 set added (the two-group invoice, a reference document, a discount, an
increase and a decrease of the amount to pay, a document attribute and a line attribute), a test SHALL rebuild the
document from the values of UJP's example (primer_za_json_21.10, 87 pages, pages cited in the test), assert every
amount the model derives (each line's unit VAT and totals, the document totals including the increase and the decrease,
the VAT totals) equals the number the PDF prints, and compare the output with a golden file this library produced under
`src/test/resources/golden`. UJP's own JSON is not committed. The 110, 120, 130 and 310 examples are unchanged since
08.10, and their goldens are byte-identical to the ones the 08.10 set produced. Where the output differs from the PDF
on purpose (the members above, the envelope, `docCurrencyCode` on 100, `docAlt` on the kinds that omit it, the
increase and decrease on the kinds that omit them, the reference name of SYS), a test names the difference and the
decision behind it. The four-group invoice of the superseded 08.10 set stays as a model test (`RoundingTest`), not a
golden. A golden is regenerated deliberately, after reading the diff, with `-Dgolden.regenerate=true`.

#### Scenario: Every printed amount is reproduced
- **WHEN** the sixteen examples are rebuilt through the model
- **THEN** every derived amount equals the number the PDF prints for it, and none differs

#### Scenario: A golden changes only deliberately
- **WHEN** the serialized output of an example differs from its golden
- **THEN** the test fails, and the golden is regenerated only after the difference is read
