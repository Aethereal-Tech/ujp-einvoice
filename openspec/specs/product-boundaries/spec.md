# Product boundaries

## Purpose

This capability records what the library deliberately does NOT do, so a rejected idea is not planned twice.

**CUT** — decided against; do not re-propose without new information. **PARKED** — may return, and each states the
condition that reopens it. An entry with neither tag is simply not built yet.

Five requirements below are CUT: a runtime dependency for JSON or JOSE, a code list for `Line.unit`, requiring
an address or a city from a natural-person buyer, signed (negative) amounts on a credit note, and enforcing a
13-digit ЕДБ format. One is PARKED: kind 170, the invoice to a natural person.

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

### Requirement: a code list for Line.unit
`Line.unit` SHALL NOT be restricted to a code list (such as UN/ECE Rec 20).

**Tag.** CUT.

**Why it waits.** It does not — the type pages (UJP07-07) type `docItemMUnit` as free text ("пар, кг, м2…", ten characters),
and no code list is published. A code list would refuse units a seller legitimately writes. The field is required on
130 and 310 and optional on the others; that is presence and not a list.

**Prerequisites (owner).** None; do not re-propose without a spec section naming a required code list.

#### Scenario: A code list for Line.unit is proposed again
- **WHEN** restricting `Line.unit` to a fixed code list is proposed again
- **THEN** it is declined without a spec section naming one

### Requirement: requiring an address, or a city, from a natural-person buyer
A natural-person buyer SHALL NOT be required to supply an address, or a city within one.

**Tag.** CUT.

**Why it waits.** It does not — private individuals frequently have neither on file, and refusing them would
refuse invoices that are perfectly legal. On every shipped kind the cut stands, and it stands without effect: a
natural-person buyer is refused outright, because kind 170 is not in 1.0 (see the PARKED entry for 170 below). **If
170 enters scope this cut is re-opened for 170 only**, because its page marks every part of `buyerAddress` mandatory
(`ЗШ`); on every other kind it stays cut.

**Prerequisites (owner).** None on the shipped kinds; do not re-propose without a spec section. For 170, the owner
putting it in scope.

#### Scenario: Requiring a natural person's address is proposed again
- **WHEN** requiring an address, or a city, from a natural-person buyer is proposed again for a shipped kind
- **THEN** it is declined without a spec section requiring it, and any such requirement is left for the consumer to
  enforce before calling this library

#### Scenario: Kind 170 enters scope
- **WHEN** the owner puts kind 170 in scope
- **THEN** the address of its buyer is required on 170 by its own page, and the cut is re-opened for 170 only

### Requirement: signed (negative) amounts on a credit note
A credit or debit note SHALL NOT carry signed (negative) amounts.

**Tag.** CUT.

**Why it waits.** It does not — the document kind carries the direction. Signing the amounts as well would state
the same fact twice and let the two disagree. **A storno is not a credit note**: the one place a negative amount
exists is a storno, whose quantities `Document` negates itself, so a caller can never negate one line and forget
another. The positive-quantity rule on a caller's lines stands for every document.

**Prerequisites (owner).** None; do not re-propose.

#### Scenario: Signed amounts on a credit note are proposed again
- **WHEN** signed (negative) amounts on a credit or debit note are proposed again
- **THEN** it is declined because the document kind already carries the direction

#### Scenario: A caller negates a line of a storno
- **WHEN** a line stated with a negative quantity is given to a storno
- **THEN** it is refused, because the model negates the quantities itself

### Requirement: enforcing a 13-digit ЕДБ format
`Party.tin` SHALL NOT be validated against a 13-digit ЕДБ format.

**Tag.** CUT.

**Why it waits.** It does not — presence is required on a business; the format is not checked, so legitimate
cross-border counterparties and fixtures are not rejected by a rule this library cannot fully confirm.

**Prerequisites (owner).** None; do not re-propose.

#### Scenario: 13-digit ЕДБ format validation is proposed again
- **WHEN** validating `Party.tin` against a 13-digit format is proposed again
- **THEN** it is declined because presence, not format, is what this library can confirm

### Requirement: kind 170, the invoice to a natural person
Kind 170 SHALL NOT be modelled, and a natural-person buyer SHALL be refused by name on every shipped kind.

**Tag.** PARKED.

**Why it waits.** The owner kept it out of 1.0 (2026-10-19): its type page prints its code as "????" and the `ЗШ*`
asterisk has no footnote, and no example shows a natural person. Billing a private individual is a different
document, not a buyer with fewer fields.

**Prerequisites (owner).** The owner putting 170 in scope, and UJP's answer on the code and the asterisk. It then
re-opens the natural-person address cut for 170 only.

#### Scenario: A natural person is billed
- **WHEN** a consumer needs to bill a private individual
- **THEN** the library refuses the buyer by name and states that the document is kind 170, which it does not ship
