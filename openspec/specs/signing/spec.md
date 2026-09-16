# Signing

## Purpose

Compact JWS, RS256, built from `java.security` and this library's own JSON writer — no JOSE library — plus loading
the private key and certificate chain from a PKCS#12 keystore or a PKCS#11 hardware token. The overriding
constraint is per-organization isolation: a process serving many organizations must be able to build one signer per
organization without any of them able to disturb another.

## Requirements

### Requirement: JwsRs256Signer produces compact JWS using SHA256withRSA, with no JOSE library
`JwsRs256Signer` SHALL produce compact JWS using RS256 (`SHA256withRSA`), built from `java.security` and this
library's own JSON writer, with no JOSE library dependency.

#### Scenario: A payload is signed
- **WHEN** `JwsRs256Signer.signCompact(byte[] payload)` is called
- **THEN** it returns a compact JWS signed with RS256 (`SHA256withRSA`), produced without any JOSE library

### Requirement: a supplied certificate chain goes in the header's x5c claim; otherwise the header is bare
When a certificate chain is supplied, `JwsRs256Signer` SHALL place it in the header's `x5c` claim (RFC 7515
§4.1.6); without one, the header SHALL be a bare `{"alg":"RS256"}`.

Whether UJP expects `x5c` on this endpoint is unconfirmed — see `specification-status`.

#### Scenario: A signer is built without a certificate chain
- **WHEN** `new JwsRs256Signer(PrivateKey privateKey)` is used with no certificate chain
- **THEN** the JWS header is the bare `{"alg":"RS256"}`

#### Scenario: A signer is built with a certificate chain
- **WHEN** `new JwsRs256Signer(PrivateKey privateKey, List<X509Certificate> certificateChain)` is used
- **THEN** the certificate chain appears in the header's `x5c` claim

### Requirement: JwsRs256Signer holds no static or JVM-global state
`JwsRs256Signer` SHALL declare no non-final static field, and two signers built from two keystores in one JVM SHALL
sign with their own keys without disturbing each other.

This is the shape a multi-organization consumer needs, where the failure mode is an invoice signed by the wrong
taxpayer. `PerOrganizationSignerTest` pins it three ways: cross-verification (each token verifies under its own
certificate and fails under the other's), byte-identical re-signing after another signer is constructed, and a
reflective check that `JwsRs256Signer` declares no non-final static field.

#### Scenario: Two signers are built from two different organizations' keystores
- **WHEN** two `JwsRs256Signer` instances are built from two different organizations' credentials in one JVM
- **THEN** each verifies under its own certificate and fails under the other's

#### Scenario: A second signer is constructed
- **WHEN** a second `JwsRs256Signer` is constructed after a first one already exists
- **THEN** the first signer re-signs the same payload byte-identically to before

#### Scenario: JwsRs256Signer's fields are inspected reflectively
- **WHEN** `JwsRs256Signer`'s fields are inspected reflectively
- **THEN** no non-final static field is found

### Requirement: SigningCredential is one identity read in a single pass under a single password
`record SigningCredential(PrivateKey privateKey, List<X509Certificate> certificateChain)` SHALL represent one
identity as it came out of one keystore, read in a single pass under a single password.

A caller holding many organizations' credentials never parses and unlocks the same bytes twice.

#### Scenario: A keystore is loaded once
- **WHEN** a PKCS#12 keystore is loaded
- **THEN** it is read in a single pass under a single password, producing one `SigningCredential`

### Requirement: Pkcs12KeyStores.load(byte[], char[]) is the entry point to reach for
`Pkcs12KeyStores.load(byte[] pkcs12, char[] password)` SHALL be the entry point to reach for.

A caller keeping one keystore per organization typically holds them encrypted in object storage; nothing here
writes a temporary file, and a decrypted keystore never touches disk.

#### Scenario: A keystore is loaded from bytes held in memory
- **WHEN** `Pkcs12KeyStores.load(byte[] pkcs12, char[] password)` is called
- **THEN** the decrypted keystore never touches disk

### Requirement: Pkcs12KeyStores.load(InputStream, char[]) reads to the end and does not close its stream
`Pkcs12KeyStores.load(InputStream pkcs12, char[] password)` SHALL read its stream to the end and SHALL NOT close
it — the caller owns what it opened.

#### Scenario: A keystore is loaded from a stream
- **WHEN** `Pkcs12KeyStores.load(InputStream pkcs12, char[] password)` is called
- **THEN** the stream is read to the end and is left open for its caller to close

### Requirement: Pkcs12KeyStores.load enumerates and requires exactly one private key entry
The `load` methods SHALL enumerate the keystore's entries themselves and expect exactly one private key entry, so a
caller never stores an alias alongside the bytes. Zero or more than one private key entry SHALL raise a
`SigningException` naming how many were found, and, for more than one, their aliases.

#### Scenario: A keystore has no private key entry
- **WHEN** a PKCS#12 keystore passed to `load` has zero private key entries
- **THEN** a `SigningException` is raised naming that zero were found

#### Scenario: A keystore has more than one private key entry
- **WHEN** a PKCS#12 keystore passed to `load` has more than one private key entry
- **THEN** a `SigningException` is raised naming how many were found and their aliases

### Requirement: the keystore password is a char[] and is never copied into a String
`Pkcs12KeyStores.load`'s password parameter SHALL be a `char[]` and SHALL NEVER be copied into a `String`.

Nothing outlives the call, so a caller may zero the array as soon as the method returns, and no exception message
can carry it.

#### Scenario: The password array is zeroed after the call returns
- **WHEN** a caller zeroes the `char[]` password immediately after `load` returns
- **THEN** nothing in `Pkcs12KeyStores` still depends on those characters

### Requirement: the alias-addressed Path methods are the unchanged 0.1.0 surface
`Pkcs12KeyStores.loadPrivateKey(Path pkcs12File, char[] password, String alias)` and
`loadCertificateChain(Path pkcs12File, char[] password, String alias)` SHALL remain the 0.1.0 surface, unchanged,
for a keystore that genuinely holds several identities. They SHALL keep the JDK's own contract, including reporting
a wrong PKCS#12 password as an `IOException`.

#### Scenario: A wrong password is supplied to the alias-addressed Path methods
- **WHEN** `loadPrivateKey(Path, char[], String)` or `loadCertificateChain(Path, char[], String)` is called with the
  wrong PKCS#12 password
- **THEN** it reports the failure as an `IOException`, matching the JDK's own contract

### Requirement: Pkcs12KeyStores raises named refusals for every failure mode
`Pkcs12KeyStores` SHALL raise a refusal by name for: a wrong password, truncated or non-PKCS#12 bytes, a stream
that fails mid-read, a missing file, zero or several key entries, a key entry that is not a private key, and a key
protected by a password other than the keystore's.

#### Scenario: A key entry is protected by a different password than the keystore's
- **WHEN** a PKCS#12 key entry is protected by a password other than the keystore's own
- **THEN** it is refused by name rather than surfacing a generic failure

### Requirement: Pkcs11KeyStores addresses a hardware token's entry by alias, never by enumeration
`Pkcs11KeyStores.loadPrivateKey(Path pkcs11ConfigFile, char[] pin, String alias)` and
`loadCertificateChain(Path pkcs11ConfigFile, char[] pin, String alias)` SHALL address a qualified certificate on its
issuing USB token by alias.

A token holds what it holds, and enumeration would be guessing on someone else's hardware. `Pkcs11KeyStores` is
excluded from the coverage gate: it needs a physical hardware token and a vendor-supplied native module that no CI
runner has — see `testing-and-gates`.

#### Scenario: A PKCS#11 token is addressed
- **WHEN** `Pkcs11KeyStores.loadPrivateKey` or `loadCertificateChain` is called
- **THEN** the entry is addressed by the caller-supplied alias, never discovered by enumeration
