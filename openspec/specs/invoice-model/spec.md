# Invoice model

## Purpose

The `model` package is the serialization-agnostic invoice domain — `Invoice`, `Party`, `Address`, `LineItem`,
`VatCategory`, `Totals`, `CategoryTotal`, `DocumentType` and `DocumentReference`. Every rule here is enforced by a
constructor, never left to a serializer or a caller to notice, and `InvoiceValidationException` is how each is
refused.

## Requirements

### Requirement: Invoice requires an invoice number, issue date, currency, seller, buyer, line items and reconciling totals
`Invoice`'s canonical constructor — `Invoice(String invoiceNumber, LocalDate issueDate, LocalDate dueDate, Currency
currency, Party seller, Party buyer, List<LineItem> lineItems, Totals totals, DocumentType documentType,
DocumentReference correctedInvoice)` — SHALL require a non-blank `invoiceNumber`, a non-null `issueDate`, a
`currency`, a `seller`, a `buyer`, at least one line item (defensively copied), and `totals` that reconcile with
those line items. An 8-argument constructor omits `documentType` and `correctedInvoice`, meaning `INVOICE` with no
reference. `Invoice.builder()` accumulates line items and computes `Totals` on `build()`.

#### Scenario: A blank invoice number is refused
- **WHEN** an `Invoice` is built with a blank `invoiceNumber`
- **THEN** it is refused

#### Scenario: The 8-argument constructor means INVOICE with no reference
- **WHEN** `Invoice` is built through the 8-argument constructor
- **THEN** `documentType` is `INVOICE` and `correctedInvoice` is absent

### Requirement: dueDate is optional and must not precede issueDate
`dueDate` SHALL be optional; when present it SHALL NOT precede `issueDate`, and when absent it SHALL be omitted
from the wire.

#### Scenario: A due date before the issue date is refused
- **WHEN** an `Invoice`'s `dueDate` precedes its `issueDate`
- **THEN** it is refused

#### Scenario: An absent due date is omitted from the wire
- **WHEN** an `Invoice`'s `dueDate` is absent
- **THEN** it is omitted from the serialized invoice

### Requirement: currency is a java.util.Currency, and the builder also accepts an ISO code string
`currency` SHALL be a `java.util.Currency`; `Invoice.builder()` SHALL also accept an ISO currency code string for
it.

#### Scenario: The builder accepts an ISO currency code string
- **WHEN** `Invoice.builder().currency(...)` is given an ISO currency code string such as `"MKD"`
- **THEN** it resolves to the corresponding `java.util.Currency`

### Requirement: a seller is always a complete business, and a missing part is refused by name
`Invoice.seller` SHALL always be a complete business — name, ЕДБ, street, postal code, city and country — and a
missing part SHALL be refused by name. Only `Party.company` may fill the seller role.

#### Scenario: A seller missing its postal code is refused by field name
- **WHEN** an `Invoice`'s seller has no address postal code
- **THEN** it is refused with `Invoice.seller.address.postalCode must be present: …`

### Requirement: a buyer is a business or a natural person
`Invoice.buyer` SHALL accept either a business (`Party.company`) or a natural person
(`Party.naturalPerson`).

#### Scenario: A natural-person buyer is accepted
- **WHEN** an `Invoice`'s buyer is built with `Party.naturalPerson`
- **THEN** it is accepted

### Requirement: correctedInvoice is required exactly when the document type corrects another
`correctedInvoice` SHALL be required when `documentType.corrects()` is true, and SHALL be refused when it is not.

#### Scenario: A credit note without a corrected-invoice reference is refused
- **WHEN** an `Invoice` has `documentType = CREDIT_NOTE` and no `correctedInvoice`
- **THEN** it is refused

#### Scenario: An ordinary invoice with a corrected-invoice reference is refused
- **WHEN** an `Invoice` has `documentType = INVOICE` and a `correctedInvoice`
- **THEN** it is refused

### Requirement: Party is a company or a natural person
`Party` is `record Party(String name, String taxId, Address address)`. `Party.company(String name, String taxId,
Address address)` builds a business — name, ЕДБ and a complete address, the first missing part refused by field
name. `Party.naturalPerson(String name)` and `Party.naturalPerson(String name, Address address)` build a private
individual, known by name alone or with whatever address is on record. `Party.isNaturalPerson()` SHALL be true
exactly when there is no tax id. `name` is one string either way — a registered organization name, or a person's
name composed however the consumer composes it.

#### Scenario: A company missing its tax id is refused by field name
- **WHEN** `Party.company` is built with no `taxId`
- **THEN** it is refused, naming the missing field

#### Scenario: isNaturalPerson is true exactly when there is no tax id
- **WHEN** a `Party` has no `taxId`
- **THEN** `Party.isNaturalPerson()` is true

### Requirement: a natural-person buyer needs only a name; tax id, address, and every address part are optional
For a natural-person buyer, the tax id SHALL be absent, street, postal code and city SHALL all be optional, an
absent country SHALL read as `MK`, and a `name` string SHALL be the only requirement.

The library does not demand a city, or any address at all, from a private individual — refusing one would refuse
invoices that are perfectly legal, ahead of a specification nobody has read.

#### Scenario: A natural person with only a name is accepted
- **WHEN** `Party.naturalPerson(String name)` is built with no address at all
- **THEN** it is accepted

#### Scenario: A natural person's absent country reads as MK
- **WHEN** a natural person's address has no `country`
- **THEN** it reads as `MK`

### Requirement: taxId is the ЕДБ, required on a business and not format-checked
`taxId` SHALL be the ЕДБ: required on a business, and not format-checked.

A 13-digit rule would reject legitimate cross-border counterparties and fixture cases this library cannot confirm.

#### Scenario: A tax id that is not 13 digits is accepted
- **WHEN** a business `Party`'s `taxId` does not have 13 digits
- **THEN** it is accepted, because the format is deliberately not checked

### Requirement: Address fields are all nullable, and country defaults to MK
`Address` is `record Address(String street, String city, String postalCode, String country)`. Every field SHALL be
nullable, and `country` SHALL default to `Address.DEFAULT_COUNTRY` (`"MK"`) when absent.

#### Scenario: An address with no country defaults to MK
- **WHEN** an `Address` is built with no `country`
- **THEN** it defaults to `Address.DEFAULT_COUNTRY` (`"MK"`)

### Requirement: an absent Address field is null and omitted; a blank one is refused
A `null` Address field SHALL be an absent field, omitted from the wire; a blank string SHALL be refused.

Absent is not blank: a blank string is a missing value pretending to be present, and would force the serializer to
choose between writing `""` and silently dropping it.

#### Scenario: A blank street is refused
- **WHEN** an `Address.street` is set to a blank string
- **THEN** it is refused with `Address.street must not be blank; omit it instead`

#### Scenario: A null street is omitted from the wire
- **WHEN** an `Address.street` is `null`
- **THEN** it is omitted from the serialized address

### Requirement: LineItem requires a non-blank description, a strictly positive quantity, a non-negative unit price and a VAT category
`LineItem` is `record LineItem(String description, BigDecimal quantity, BigDecimal unitPrice, VatCategory
vatCategory, String unit)`, plus a 4-argument constructor for a line that states no unit. `description` SHALL be
non-blank, `quantity` SHALL be strictly positive, `unitPrice` SHALL be non-negative (zero allowed), and
`vatCategory` SHALL be non-null.

#### Scenario: A zero or negative quantity is refused
- **WHEN** a `LineItem`'s `quantity` is zero or negative
- **THEN** it is refused

#### Scenario: A zero unit price is accepted
- **WHEN** a `LineItem`'s `unitPrice` is zero
- **THEN** it is accepted

### Requirement: unit is optional free text, not a code from a list
`LineItem.unit` SHALL be optional free text as the seller writes it — „ком.", „м²", „час", `"kg"` — never a code
from a list. A blank `unit` SHALL be refused; an absent one SHALL be omitted from the wire.

Nothing says UJP expects a unit at all, let alone a UN/ECE Rec 20 code, and inventing a list would refuse units a
seller legitimately uses.

#### Scenario: A blank unit is refused
- **WHEN** a `LineItem`'s `unit` is a blank string
- **THEN** it is refused

#### Scenario: An absent unit is omitted from the wire
- **WHEN** a `LineItem`'s `unit` is absent
- **THEN** it is omitted from the serialized line item

### Requirement: netAmount, vatAmount and grossAmount are derived, never stored
`LineItem.netAmount()`, `vatAmount()` and `grossAmount()` SHALL be derived from `quantity`, `unitPrice` and
`vatCategory` on each call, never stored fields.

#### Scenario: Amounts are computed rather than read from a stored field
- **WHEN** `LineItem.netAmount()`, `vatAmount()` or `grossAmount()` is called
- **THEN** the value is computed from `quantity`, `unitPrice` and `vatCategory`, not read from a stored field

### Requirement: DocumentType distinguishes an ordinary invoice from a credit or debit note
`enum DocumentType { INVOICE, CREDIT_NOTE, DEBIT_NOTE }` SHALL expose `corrects()`, true for the two note types.
`INVOICE` is an ordinary sales invoice; `CREDIT_NOTE` is „книжно одобрение" (lowers what an earlier invoice
charged); `DEBIT_NOTE` is „книжно задолжување" (raises it).

#### Scenario: corrects() is true for both note types
- **WHEN** `DocumentType.corrects()` is called on `CREDIT_NOTE` or on `DEBIT_NOTE`
- **THEN** it is true

#### Scenario: corrects() is false for an ordinary invoice
- **WHEN** `DocumentType.corrects()` is called on `INVOICE`
- **THEN** it is false

### Requirement: DocumentReference names the number and issue date of the document a note corrects
`record DocumentReference(String number, LocalDate issueDate)` SHALL require both parts. This library cannot check
that the referenced document exists; a consumer that keeps its own invoices should check before building the note.

#### Scenario: A DocumentReference missing its issue date is refused
- **WHEN** a `DocumentReference` is built with no `issueDate`
- **THEN** it is refused

### Requirement: amounts on a credit or debit note stay positive
A credit note or debit note SHALL carry positive amounts: a credit note for 1.000 MKD carries a line of 1.000,
never −1.000. The document type carries the direction; the amounts carry the magnitude.

Signing the amounts would state the same fact twice and would force `LineItem`'s positive-quantity rule open for
notes, after which an ordinary typo on an ordinary invoice would stop being caught. See the `signed-note-amounts`
requirement recorded as decided against in `product-boundaries`.

#### Scenario: A credit note for 1.000 MKD carries a positive line amount
- **WHEN** a `CREDIT_NOTE` is built for 1.000 MKD
- **THEN** its line carries 1.000, never −1.000

### Requirement: VatCategory enumerates the North Macedonian VAT categories, with verified rates
`VatCategory` SHALL enumerate, per the Law on Value Added Tax: `STANDARD_18` (`code()` `"18"`, rate 0.18),
`REDUCED_10` (`"10"`, 0.10), `REDUCED_5` (`"5"`, 0.05), `ZERO` (`"0"`, 0.00) and `EXEMPT` (`"EXEMPT"`, 0.00).

The rates are public tax law and verified; only the wire `code()` for each is reconstructed — see
`specification-status`.

#### Scenario: STANDARD_18 carries an 18% rate and wire code "18"
- **WHEN** `VatCategory.STANDARD_18`'s `code()` and rate are read
- **THEN** the code is `"18"` and the rate is 0.18

#### Scenario: EXEMPT carries a 0% rate and wire code "EXEMPT"
- **WHEN** `VatCategory.EXEMPT`'s `code()` and rate are read
- **THEN** the code is `"EXEMPT"` and the rate is 0.00

### Requirement: a line rounds its net amount first, then computes VAT from the rounded net
Money math SHALL round a line's net amount first, then compute its VAT amount from that rounded net, through
`internal.Money.round`.

#### Scenario: VAT is computed from the rounded net, not the raw net
- **WHEN** a `LineItem`'s net amount is rounded
- **THEN** its VAT amount is computed from that rounded net, not from the unrounded value

### Requirement: Totals must reconcile with the line items, or the Invoice is refused
`Totals.compute` SHALL reconcile the per-category and invoice-wide sums against the line items, and `Invoice`'s
canonical constructor SHALL refuse totals that do not reconcile.

#### Scenario: Totals that do not match the line items are refused
- **WHEN** an `Invoice` is built with `Totals` that do not reconcile against its `lineItems`
- **THEN** it is refused
