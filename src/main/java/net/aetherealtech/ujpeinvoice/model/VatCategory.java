package net.aetherealtech.ujpeinvoice.model;

import java.math.BigDecimal;

/**
 * North Macedonian VAT categories, per the Law on Value Added Tax: the standard rate, the two
 * reduced rates, the zero rate, and the VAT-exempt category.
 *
 * <p>The rates themselves (18%, 10%, 5%, 0%) are public tax law, not part of the unverified UJP
 * e-Faktura wire format — see the "Specification inventory" section of {@code SPECS.md}. What is
 * genuinely unverified is how a category is <em>named</em> on the wire; {@link #code()} is this
 * library's best-effort reconstruction and is marked accordingly in {@code UjpJsonSerializer}.
 */
public enum VatCategory {

    /** Standard rate, 18%. Applies to most goods and services. */
    STANDARD_18("18", new BigDecimal("0.18")),

    /** Reduced rate, 10%. Applies to a defined list of goods and services (e.g. certain foodstuffs). */
    REDUCED_10("10", new BigDecimal("0.10")),

    /** Reduced rate, 5%. Applies to a narrower defined list (e.g. basic foodstuffs, medicines, books). */
    REDUCED_5("5", new BigDecimal("0.05")),

    /** Zero rate, 0%. Taxable, VAT-registered, but charged at 0% (e.g. exports). */
    ZERO("0", BigDecimal.ZERO),

    /** Exempt from VAT entirely (e.g. certain financial, medical, or educational services). */
    EXEMPT("EXEMPT", BigDecimal.ZERO);

    private final String code;
    private final BigDecimal rate;

    VatCategory(String code, BigDecimal rate) {
        this.code = code;
        this.rate = rate;
    }

    /** The reconstructed wire code for this category. See the class javadoc's caveat. */
    public String code() {
        return code;
    }

    /** The VAT rate as a fraction (e.g. {@code 0.18} for 18%), never negative, at most 1. */
    public BigDecimal rate() {
        return rate;
    }
}
