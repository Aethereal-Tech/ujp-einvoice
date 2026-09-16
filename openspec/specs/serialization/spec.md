# Serialization

## Purpose

The `serialization` package turns the `model` into UJP's wire shape without the model ever knowing JSON exists: the
`Serializer` interface, and `UjpJsonSerializer`, the one shipped implementation of the reconstructed shape. The
exact field names it writes, and their confidence, are recorded in `specification-status`; this capability records
the serializer's own mechanical guarantees.

## Requirements

### Requirement: Serializer is an interface; UjpJsonSerializer is the one shipped implementation
`serialization` SHALL expose a `Serializer` interface, and `UjpJsonSerializer` SHALL be its one shipped
implementation, of the reconstructed wire shape.

A corrected schema, or an entirely different format (UBL 2.1, say), is a new `Serializer` implementation, not a
change to `model` — see the `ubl-serializer` change.

#### Scenario: A corrected wire shape is a new Serializer implementation
- **WHEN** the wire schema needs correcting
- **THEN** the fix is a new `Serializer` implementation, and `model` is unchanged

### Requirement: field order is fixed and deterministic
Field order SHALL be fixed and deterministic: `Totals.byCategory` follows `VatCategory`'s declaration order via an
`EnumMap`. Golden-file tests depend on it.

#### Scenario: byCategory follows VatCategory's declaration order
- **WHEN** `Totals.byCategory` is serialized
- **THEN** its entries appear in `VatCategory`'s declaration order, produced by an `EnumMap`

### Requirement: absent fields are omitted, never written as "" or null
`UjpJsonSerializer` SHALL omit an absent field from its output rather than writing it as `""` or `null`.

This is this library's own choice — the gateway's tolerance for either is unverified; see `specification-status`.

#### Scenario: An absent optional field does not appear in the JSON at all
- **WHEN** an optional field such as `dueDate`, an `Address` part, or `LineItem.unit` is absent
- **THEN** the serialized JSON omits the field entirely, rather than writing it as `""` or `null`

### Requirement: an INVOICE serializes byte-for-byte as it did in 0.1.0
An `INVOICE` document SHALL serialize byte-for-byte identically to the 0.1.0 shape: `golden/simple-invoice.json`
and `golden/multi-category-invoice.json` are unchanged and pin it.

The document type is expressed by absence — `documentType` is written only for a note — so introducing notes could
not disturb a shape already in the field.

#### Scenario: simple-invoice.json and multi-category-invoice.json are unchanged since 0.1.0
- **WHEN** an `INVOICE` document is serialized
- **THEN** its JSON matches `golden/simple-invoice.json` or `golden/multi-category-invoice.json` byte-for-byte, as
  it did in 0.1.0

### Requirement: documentType and correctedInvoice are written only on a note
`UjpJsonSerializer` SHALL write no `documentType` field at all for `INVOICE`, and SHALL write `documentType` (as
`"CREDIT_NOTE"` or `"DEBIT_NOTE"`) and `correctedInvoice` only for a note.

#### Scenario: An ordinary invoice writes no documentType field
- **WHEN** an `INVOICE` is serialized
- **THEN** the JSON has no `documentType` field at all

#### Scenario: A credit note writes documentType and correctedInvoice
- **WHEN** a `CREDIT_NOTE` is serialized
- **THEN** the JSON carries `"documentType": "CREDIT_NOTE"` and a `correctedInvoice` object
