package net.aetherealtech.ujpeinvoice.model;

import java.util.Set;

/**
 * The additional indicator of a document (UJP01-15) and the kinds each value is valid on, from the
 * 14.09.2026 changelog. The final invoice that closes an advance and a commission sale are valid on a
 * 100; the others belong to kinds this library does not ship (200, 210, 660), so they are valid on
 * none of its kinds. No indicator is the common case.
 */
public enum DocIndicator {

    /** A final invoice that closes an advance. */
    KONECNA(Set.of(DocumentKind.INVOICE)),
    /** A commission sale (also valid on a 900 dispatch note). */
    KOMISIONA(Set.of(DocumentKind.INVOICE)),
    /** Forced collection (200 and 210). */
    PN(Set.of()),
    /** Forced execution (200 and 210). */
    PI(Set.of()),
    /** Tour operator (660). */
    TUR(Set.of()),
    /** Second-hand goods (660). */
    DOBRA(Set.of());

    private final Set<DocumentKind> validOn;

    DocIndicator(Set<DocumentKind> validOn) {
        this.validOn = validOn;
    }

    public boolean validOn(DocumentKind kind) {
        return validOn.contains(kind);
    }
}
