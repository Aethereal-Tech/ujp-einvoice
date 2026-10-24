# Document model

## Purpose

The `model` package is the domain of a UJP sales document: `Document`, built per `DocumentKind`, with its `Header`,
`Mode`, `Reference`s, `Party`s, `Payment`, `Line`s, `Advance`, `Adjustments` and `Attribute`s, and the figures the model
derives from them — `DocumentTotals` and `VatTotal`. It follows UJP's own concepts (the type pages of 100, 110, 120, 130
and 310, as of 2026-10-14/16), and names no wire field and knows no JSON. Every rule here is enforced by a constructor,
never left to a serializer or a caller to notice, and `DocumentValidationException` is how each is refused, naming the
field and citing its UJP code. It replaces 0.2.0's `Invoice`, `LineItem`, `Totals`, `CategoryTotal`, `VatCategory`,
`DocumentType` and `DocumentReference`, which went in the same change.

## Requirements

### Requirement: a Document is built for one kind, from facts the caller gives, and derives every figure it carries
`Document(DocumentKind kind, Header header, Mode mode, List<Reference> references, Party seller, Party buyer, Payment
payment, List<Line> lines, Advance advance, Adjustments adjustments, List<Attribute> attributes)` is built through
`Document.builder(kind)`. Its totals (`totals()`) and its per-indicator VAT totals (`vatTotals()`) SHALL be derived from
the lines on every call and SHALL NOT be accepted from the caller, nor any figure a line derives.

#### Scenario: A document's totals follow its lines
- **WHEN** a document of two lines of 100 at 18% is built
- **THEN** `totals()` reads net 200, VAT 36, gross 236 and a final amount of 236, with nothing supplied by the caller

### Requirement: a missing or wrong field is refused by name, with the UJP code of the field
Every refusal SHALL be a `DocumentValidationException` whose message names the field and starts with its UJP code
(`UJP01-06 turnoverDate is required on a 100`). The code is a citation of the type page and not a wire name.

#### Scenario: A missing turnover date is refused on an invoice
- **WHEN** a `DocumentKind.INVOICE` is built without a turnover date
- **THEN** it is refused with `UJP01-06 turnoverDate is required on a 100`

### Requirement: DocumentKind holds the code and the rules of each shipped kind
`DocumentKind` SHALL be `INVOICE` 100, `CREDIT_NOTE` 110, `DEBIT_NOTE` 120, `ADVANCE_INVOICE` 130 and `CASH_TURNOVER`
310, each with `code()` and `ujpName()`, and its rules read from the type pages:

| Rule | 100 | 110 | 120 | 130 | 310 |
|---|---|---|---|---|---|
| turnover date required (UJP01-06) | yes | no | no | no | yes |
| payment group allowed (UJP10) | yes | no | no | yes | yes |
| payment type required (UJP10-01) | no | — | — | yes | yes |
| advance deduction allowed (UJP08-06/-08) | yes | no | no | no | no |
| attributes allowed (UJP07-26, UJP12-01) | yes | no | no | no | no |
| currency date and rate listed (UJP10-10/-11) | no | — | — | yes | yes |
| unit required (UJP07-07) | no | no | no | yes | yes |
| `docId` length (UJP01-11) | 30 | 30 | 30 | 30 | 12 |

Party names, street, street number and subsidiary name are also shorter on 310 (80, 60, 10, 100 against 150, 80, 20,
150). A kind beyond this set is a new constant and not a new type; none ships with rules.

#### Scenario: A note refuses a payment
- **WHEN** a payment is given to a credit note or a debit note
- **THEN** it is refused with `UJP10 payment is not on the page of a 110`

#### Scenario: A 130 and a 310 require a payment type
- **WHEN** a payment without a type, or none, is given to an advance invoice or a cash turnover
- **THEN** it is refused with `UJP10-01 payment.type is required on a 130` (or 310)

#### Scenario: A cash turnover has a shorter id
- **WHEN** a 310 has a 13-character id
- **THEN** it is refused with `UJP01-11 id is at most 12 characters on a 310`

### Requirement: the references of a note are reported as mandatory and not enforced
`DocumentKind.requiresReferences()` SHALL be true for 110 and 120, where UJP02-01 is marked mandatory, and the model
SHALL NOT refuse an empty list on them: the 110 and 120 examples send `[]`, and the page's own note makes the four
sub-fields conditional on a reference being used. The accessor is `@ProvisionalSpec` and reports the marker for a caller
that wants to enforce it. A reference needs all four parts once it is used.

#### Scenario: A credit note without references is accepted
- **WHEN** a credit note is built with no reference
- **THEN** it is accepted, as the 110 example is

### Requirement: Mode is the docStorno axis, independent of the kind
`Mode` SHALL be a sealed interface of `Ordinary`, `Storno(VoidReason reason, String comment)` and
`Correction(CorrectionReason reason, String comment)`. A storno SHALL carry exactly one reference of type `ST` naming the
original and none of type `KR`; a correction exactly one of type `KR` and none of type `ST`; an ordinary document
neither. The 22.10.2026 changelog split the one reference type into `ST Сторно` and `KR Корекција` ("И кај двете мора да
се прати валиден EUID", both must carry a valid EUID), so a type is recognised by its code and `ST`'s name is now
"Сторно" (it was "Сторно/Корекција"). A reason whose code list entry is "Друго"
(`S-5`, `C08`) requires a comment (`@ProvisionalSpec`: the list asks for free text). `VoidReason`, `CorrectionReason`,
`RefDocumentType` and `PaymentType` are code-plus-name values: the gateway serves the lists, so any pair is accepted and
the known ones are constants (the reference types as of 16.10, including the eleven added on 15.10). `docStorno` has no
row on the 110, 120 and 130 pages, so `Mode` is `@ProvisionalSpec` there and the model does not refuse a storno on them.

#### Scenario: A storno without its original is refused
- **WHEN** a storno is built without a reference of type `ST`
- **THEN** it is refused with `UJP02-01-01 references needs exactly one reference of type ST naming the original, to be stornoed`

#### Scenario: A correction without its original is refused
- **WHEN** a correction is built with a reference of type `ST`, or without any
- **THEN** it is refused with `UJP02-01-01 references needs exactly one reference of type KR naming the original, to be corrected`

#### Scenario: A storno names its original with ST and a correction with KR
- **WHEN** a storno also carries a reference of type `KR`, or a correction also one of type `ST`
- **THEN** it is refused, citing the 22.10.2026 changelog

#### Scenario: An ordinary document names no original
- **WHEN** an ordinary document is built with a reference of type `ST` or `KR`
- **THEN** it is refused

### Requirement: the number of an ST or KR reference is an EUID-shaped string
`Reference(type, date, number)` SHALL refuse a number that is not shaped like a UUID in its textual form, 8-4-4-4-12
hexadecimal digits in either case, when the type is `ST` or `KR`: the check is `RefDocumentType.namesAnOriginal`. It
does not pin the version (the published EUIDs are version 7, and no document says the gateway pins it) and it cannot
know whether the document exists, which only the gateway can. Any other type may carry a number that is not an EUID
(UJP02-01-04: "EUID или друг број").

#### Scenario: An order number on a storno reference is refused
- **WHEN** a reference of type `ST` or `KR` carries the number `100-2625364731/2026`
- **THEN** it is refused with `UJP02-01-04 reference.number must be the EUID of the original (8-4-4-4-12 hexadecimal digits) on type ST (changelog 22.10.2026)`

#### Scenario: Another type keeps a free number
- **WHEN** a reference of type `NAR` carries the number `100-2625364731/2026`
- **THEN** it is accepted

### Requirement: a storno negates every quantity itself, and nothing else is negative
The caller SHALL describe the original's lines with positive quantities; `Document.lines()` returns them as given and
`Document.effectiveLines()` returns them with every quantity negated on a storno, so a caller cannot negate one line
and forget another. Unit prices and unit VAT stay positive and every total follows the sign. A line stated with a
negative quantity SHALL be refused. This is the only place a negative amount exists; a credit or debit note carries
positive amounts, the kind carrying the direction.

#### Scenario: A storno of one line
- **WHEN** a storno is built from a line of 1 at 95.2381 in group DDV-B
- **THEN** its effective line has quantity -1, unit VAT 4.7619, total VAT -4.7619 and total with VAT -100, and its
  document gross is -100

#### Scenario: A negative quantity is refused
- **WHEN** a line with a negative quantity is given to any document
- **THEN** it is refused with `UJP07-08 lines[n].quantity must be positive: …`

### Requirement: the header needs a number, an id and a date, and the kind decides the rest
`Header(number, id, date, turnoverDate, deliveryDate, delivery, periodStart, periodEnd, notes, headerText, footerText,
indicator, alternativeMode)` SHALL require `number` (UJP01-09), `id` (UJP01-11) and `date` (UJP01-03); a period SHALL
NOT end before it starts. `DocIndicator` holds the six published values and the kinds each is valid on: `KONECNA` and
`KOMISIONA` on 100, and `PN`, `PI`, `TUR`, `DOBRA` on none of the shipped kinds. `alternativeMode` is a boolean in the
model whatever `docAlt` is on the wire.

#### Scenario: An indicator the kind does not list is refused
- **WHEN** a credit note carries `KONECNA`
- **THEN** it is refused with `UJP01-15 indicator is not valid on a 110: KONECNA`

### Requirement: a seller is an identified business, and a buyer is a business or a non-resident
Both parties SHALL have a name and a complete `Address` (street, number, postal code and city, each mandatory). The
seller SHALL have a `tin`. The buyer SHALL have a `tin`, or a `foreignTin` and a country other than `MK`. A natural
person (`Party.naturalPerson`) SHALL be refused as a buyer on every shipped kind, because kind 170 is not in 1.0. The
ЕДБ is required and not format-checked. `Country` defaults to MK, Северна Македонија, in Latin letters as the examples
write it. A `Subsidiary` (code, name, address) needs at least one part.

#### Scenario: A natural-person buyer is refused
- **WHEN** a document is built with `Party.naturalPerson` as its buyer
- **THEN** it is refused with `UJP05-03 buyer.tin is required: a natural-person buyer is kind 170, which this library does not ship`

#### Scenario: A tax id that is not 13 digits is accepted
- **WHEN** a business party's `tin` does not have 13 digits
- **THEN** it is accepted, because the format is deliberately not checked

### Requirement: a Line takes quantity, price, discount and tax, and derives every other figure
`Line` SHALL take `sku`, `senderCode`, `receiverCode`, `description`, `unit`, `quantity`, `unitPrice`, `unitDiscount`,
`vatGroup`, `taxIndicator`, `domesticProduct`, `basePrice` and `attributes`. `quantity`, `unitPrice` and `unitDiscount`
SHALL have at most four decimals and are refused beyond that rather than rounded; the unit price and the discount
SHALL NOT be negative and the discount SHALL NOT exceed the price. The line number is the position, from 1. Derived,
at four decimals, `HALF_UP`: `unitPriceAfterDiscount = unitPrice - unitDiscount`; `unitVat = unitPriceAfterDiscount ×
rate`, the group's rate when the indicator's impact is `STANDARD` and 0 otherwise; `totalBeforeDiscount = quantity ×
unitPrice`; `total = quantity × unitPriceAfterDiscount`; `totalVat = quantity × unitVat`; `totalWithVat = total +
totalVat`. These are the formulas of the gateway's API description. `unit` is free text and never a code from a list;
it is required on 130 and 310 only.

#### Scenario: A line is derived to four decimals
- **WHEN** a line of 1 at 254.24 in group DDV-A is built
- **THEN** its unit VAT is 45.7632 and its total with VAT is 300.0032

#### Scenario: A fifth decimal is refused
- **WHEN** a line has a unit price of 0.00001
- **THEN** it is refused with `UJP07-09 unitPrice has at most 4 decimals; the model refuses rather than rounds`

### Requirement: a BasePrice must add up to the unit price
`BasePrice(base, add, deduct, type)` SHALL satisfy `base + add - deduct = unitPrice`, an absent `add` or `deduct`
counting as 0, or the line is refused. UJP07-24 (`deduct`) is not on the 100 table but is named by the 14.09 changelog;
it is written for every kind.

#### Scenario: A base price that does not add up is refused
- **WHEN** a line's base 80 plus 30 minus 10 is not its unit price
- **THEN** it is refused with `UJP07-22 basePrice base + add - deduct must equal the unit price …`

### Requirement: VatGroup is the four rate groups, and the tax indicator is a value with a dated catalogue
`VatGroup` SHALL be `DDV_A` 18, `DDV_V` 10, `DDV_B` 5 and `DDV_G` 0. `TaxIndicator(code, vatImpact, group, note)` SHALL
carry the impact (`STANDARD`, `NULA`, `OSLOBODEN`, `PRENESEN`, and `MARGINALNA` from the gateway's `TaxIndicatorDto`),
the rate group the catalogue pairs it with (`null` when it offers several) and a note, which is required unless the
impact is `STANDARD`. A line's group SHALL agree with its indicator's. `TaxIndicators.SNAPSHOT_2026_08_27` SHALL hold
the 90 sales indicators of `/danocni-indikatori`, retrieved 2026-10-19, looked up by code; an indicator outside it is
constructed by stating its impact. The calculation is driven by the impact and not by the code. A margin-scheme
(`MARGINALNA`) indicator is refused on a line, because no obtained document says how the gateway computes it.

The page carries no impact column, so every entry's impact except `DDV-A`, `DDV-V`, `DDV-B` and `DDV-G` (which the
gateway's own examples settle) is the library's reading of its name, and the snapshot is `@ProvisionalSpec`. The page
prints `DDV-10-14-4-6` twice; the second row is stored as `DDV-10-14-6`, the code the 02.10 changelog names. Four codes
contain a Cyrillic letter and are stored exactly so.

#### Scenario: An exempt line carries no VAT
- **WHEN** a line of 1111 carries the exempt indicator `DDV-9-1` in group `DDV-G`
- **THEN** its VAT is 0 and its total with VAT is 1111

#### Scenario: An unknown indicator is refused by name
- **WHEN** the snapshot is asked for a code it does not hold
- **THEN** it is refused with `UJP07-19 taxIndicator <code> is not in the snapshot of 2026-10-19; state its vatImpact explicitly`

### Requirement: the document totals are summed unrounded and rounded once
`DocumentTotals` SHALL be `net` (Σ `totalBeforeDiscount`), `discount` (Σ quantity × unit discount),
`netAfterDiscount` (Σ `total`), `vat` (Σ `totalVat`) and `gross` (Σ `totalWithVat`), each rounded once to two decimals
from the unrounded sum; `grossRounded` the unrounded gross rounded to a whole number in MKD and to two decimals in a
foreign currency (`@ProvisionalSpec`, below); and `advanceAmount`, `increase` and `decrease` as given, whole in MKD and
at most two decimals otherwise. `finalAmount` SHALL be `grossRounded - advanceAmount + increase - decrease`, so it is
whole in MKD too. The currency is the payment's, and MKD where the document has no payment group (110, 120) or the
payment names none. `gross` is the
rounded sum of the lines' gross and not the rounded net plus the rounded VAT; the two can differ by a cent, inside the
gateway's tolerance of 0.1, which the library never leans on.

#### Scenario: Four groups in one invoice
- **WHEN** an invoice has four lines whose unrounded gross values are 105, 2000.0001, 1222.1 and 90
- **THEN** `gross` is 3417.10, `grossRounded` 3417, `vat` 421.18 and `net` 2995.92, as UJP's example prints them

#### Scenario: A cash turnover rounds its unrounded gross
- **WHEN** a line's gross is 300.0032
- **THEN** `gross` is 300, `vat` is 45.76 and `grossRounded` is 300

#### Scenario: A foreign currency keeps two decimals
- **WHEN** a document paid in EUR has a line of 100.40 at 18%, an advance of 10.50, an increase of 1.10 and a decrease of 0.35
- **THEN** `gross` and `grossRounded` are 118.47 and `finalAmount` is 108.72, where the same document in MKD would
  round `grossRounded` to 118 and refuse the fractions

The foreign-currency `grossRounded` is `@ProvisionalSpec`. The 19.10.2026 changelog makes `docGrossAmountR`
Decimal 15.2 and says decimals are for foreign currencies only; it states no rounding rule for them. The two EUR
examples of the 21.10 set (kind 510, and the one on page 76) have a whole gross, 300 and 295, so they show the same
number whether the field is rounded to 0 or kept at 2. The library keeps two decimals (`Money.grossRounded`).

### Requirement: the VAT totals are one entry per indicator and group, in first-appearance order
`VatTotal(indicator, group, taxable, vat, total)` SHALL be derived, one per tax indicator and rate group, in the order
they first appear among the lines, from unrounded sums rounded once to two decimals. Under a `PRENESEN` indicator the
lines carry no VAT and the document totals exclude it, but the entry SHALL show the VAT the group's rate puts on the
base, with the indicator's note, as the API description says it appears "кај тоталите по даночен индикатор". That
amount is `@ProvisionalSpec`: the description does not state it, and the art. 32 example that shows it is not
internally consistent (its line prices do not reproduce its totals).

#### Scenario: A transferred tax appears only in the per-indicator totals
- **WHEN** a line of 1000 carries `DDV-11-A-G` in group `DDV-A`
- **THEN** the document's VAT is 0 and its gross 1000, and the VAT total shows taxable 1000, VAT 180 and total 1180

### Requirement: an Advance and Adjustments are Decimal 15.2, whole in MKD, and refused rather than rounded
`Advance(date, description, amount)` and `Adjustments(increase, decrease)` SHALL take amounts of zero or more with at
most two decimals, as UJP08-08, UJP08-11 and UJP08-12 are typed since the 19.10.2026 changelog (Integer before it), and
refuse a negative amount or a third decimal. `Document` SHALL refuse a fraction in MKD, citing the entry's note:
"decimal values are used only for foreign currencies; in MKD the sum is a whole number with decimals ,00 (e.g. 102,00)".
`102.00` is whole and accepted. The check sits in `Document`, which knows the currency, and not in the two records.
An advance is refused on a kind whose page does not list it (all but 100). An absent adjustment counts as 0.

#### Scenario: A fractional advance in MKD is refused
- **WHEN** an advance of 10.50 is given to an invoice paid in MKD
- **THEN** it is refused with `UJP08-08 advance.amount must be a whole number in MKD: decimal values are used only for foreign currencies (changelog 19.10.2026)`

#### Scenario: A fractional advance in EUR is accepted
- **WHEN** an advance of 10.50 is given to an invoice paid in EUR
- **THEN** it is accepted, and `docAvansAmount` is 10.5

#### Scenario: A third decimal is refused in any currency
- **WHEN** an advance of 100.505 is given
- **THEN** it is refused with `UJP08-08 advance.amount has at most 2 decimals; the model refuses rather than rounds`

### Requirement: Payment defaults to MKD and an Attribute is checked against its type
`Payment(type, dueDays, dueDate, terms, interest, discount, note, currency)` SHALL bound `dueDays` to 0..9999 and
default an absent currency to MKD (`@ProvisionalSpec`). `Attribute(name, value, type)` SHALL check `value` against
`STR`, `DEC`, `INT` or `DATE`, at document level and on a line, and only on a kind that lists the group (100).

#### Scenario: An attribute value that does not fit its type is refused
- **WHEN** an attribute of type `DEC` has the value `abc`
- **THEN** it is refused with `UJP12-01-02 attribute.value is not a valid DEC: abc`

### Requirement: a line is carried unrounded to four decimals, and its values are summed before they are rounded
A line's derived values SHALL be computed at four decimals, `RoundingMode.HALF_UP`, through `internal.Money`, and SHALL
NOT be rounded further. VAT totals and document totals SHALL be summed from those unrounded line values and rounded
once, to two decimals. The five amounts of UJP08-08 to UJP08-12 (`docAvansAmount`, `docFinalAmount`,
`docGrossAmountR`, `docAmountIncrease`, `docAmountDecrease`) SHALL be whole numbers in MKD and carry two decimals in a
foreign currency (19.10.2026).

This is the rule UJP's `/json-struktura` page states under "Заокружување". Rounding a line's net before computing its
VAT, as 0.2.0 did, moves a total by a cent as soon as several lines are summed, and the gateway recomputes the
totals itself. A test generates documents and holds the library to sum-then-round, and shows per-line rounding would
differ.

#### Scenario: Totals are summed before they are rounded
- **WHEN** the lines' unrounded VAT values are 4.7619, 305.0848 and 111.1
- **THEN** the document's VAT is their sum, 421.1848, rounded once to 421.18

#### Scenario: Rounding each line first would give another total
- **WHEN** three lines each have a gross of 0.3953
- **THEN** the document gross is 1.19, where rounding each line to 0.40 first would give 1.20
