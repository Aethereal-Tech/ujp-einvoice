package net.aetherealtech.ujpeinvoice.transport;

import java.util.Objects;

/**
 * One entry of D5, the official error catalogue (efakturawiki.ujp.gov.mk, {@code /error-codes}, page
 * updated 2026-10-05, retrieved 2026-10-19). {@code meaning} is the page's own text, verbatim and
 * untranslated — including its spelling — because it is a quotation of what UJP published, not this
 * library's description of it.
 */
public record UjpErrorCode(ErrorNamespace namespace, String code, String meaning) {

    public UjpErrorCode {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(meaning, "meaning");
    }
}
