# Product boundaries

## Purpose

This capability records what the library deliberately does NOT do, so a rejected idea is not planned twice.

**CUT** — decided against; do not re-propose without new information. **PARKED** — may return, and each states the
condition that reopens it. An entry with neither tag is simply not built yet.

Five requirements below are CUT: a runtime dependency for JSON or JOSE, a code list for `LineItem.unit`, requiring
an address or a city from a natural-person buyer, signed (negative) amounts on a credit note, and enforcing a
13-digit ЕДБ format.

## Requirements

### Requirement: a runtime dependency for JSON or JOSE
The library SHALL NOT take a runtime dependency for JSON parsing/writing or for JOSE (JWS/JWT) handling.

**Tag.** CUT.

**Why it waits.** It does not — Jackson or Nimbus would add a transitive surface, a version to track and a CVE feed
to watch, for work this library already does in a few hundred lines it owns end to end.

**Prerequisites (owner).** None; do not re-propose while the wire shape stays a closed object graph and signing
stays RS256-only. Reconsider only if a corrected spec needs general-purpose JSON — arbitrary nesting, unknown
fields — which is a fact about the verification pass, not a preference.

#### Scenario: A JSON or JOSE library is proposed again
- **WHEN** adding Jackson, Nimbus, or a similar runtime dependency for JSON or JOSE is proposed again
- **THEN** it is declined unless a corrected spec (from the verification pass) needs general-purpose JSON handling
  this library's own writer cannot provide

### Requirement: a code list for LineItem.unit
`LineItem.unit` SHALL NOT be restricted to a code list (such as UN/ECE Rec 20).

**Tag.** CUT.

**Why it waits.** It does not — nothing says UJP expects a unit at all, let alone a code from a list. A code list
would refuse units a seller legitimately writes.

**Prerequisites (owner).** None; do not re-propose without a spec section naming a required code list.

#### Scenario: A code list for LineItem.unit is proposed again
- **WHEN** restricting `LineItem.unit` to a fixed code list is proposed again
- **THEN** it is declined without a spec section naming one

### Requirement: requiring an address, or a city, from a natural-person buyer
A natural-person buyer SHALL NOT be required to supply an address, or a city within one.

**Tag.** CUT.

**Why it waits.** It does not — private individuals frequently have neither on file, and refusing them would
refuse invoices that are perfectly legal.

**Prerequisites (owner).** None; do not re-propose without a spec section. If the spec turns out to require more,
that is a refusal for the consumer to raise by name before it calls this library.

#### Scenario: Requiring a natural person's address is proposed again
- **WHEN** requiring an address, or a city, from a natural-person buyer is proposed again
- **THEN** it is declined without a spec section requiring it, and any such requirement is left for the consumer to
  enforce before calling this library

### Requirement: signed (negative) amounts on a credit note
A credit or debit note SHALL NOT carry signed (negative) amounts.

**Tag.** CUT.

**Why it waits.** It does not — the document type carries the direction. Signing the amounts as well would state
the same fact twice, let the two disagree, and force `LineItem`'s positive-quantity rule open for every document.

**Prerequisites (owner).** None; do not re-propose.

#### Scenario: Signed amounts on a credit note are proposed again
- **WHEN** signed (negative) amounts on a credit or debit note are proposed again
- **THEN** it is declined because the document type already carries the direction, and signing the amounts would
  force `LineItem`'s positive-quantity rule open for every document

### Requirement: enforcing a 13-digit ЕДБ format
`Party.taxId` SHALL NOT be validated against a 13-digit ЕДБ format.

**Tag.** CUT.

**Why it waits.** It does not — presence is required on a business; the format is not checked, so legitimate
cross-border counterparties and fixtures are not rejected by a rule this library cannot fully confirm.

**Prerequisites (owner).** None; do not re-propose.

#### Scenario: 13-digit ЕДБ format validation is proposed again
- **WHEN** validating `Party.taxId` against a 13-digit format is proposed again
- **THEN** it is declined because presence, not format, is what this library can confirm
