package net.aetherealtech.ujpeinvoice.model;

/**
 * A tax indicator (UJP07-19, UJP09-01-01): the code, how it affects the VAT calculation, the rate
 * group the catalogue pairs it with, and the note the gateway wants printed with it. About 90 sales
 * indicators exist and the list grows with the changelog, so this is a value and not an enum: look
 * one up in {@link TaxIndicators}, or state an indicator the snapshot does not know by its impact.
 *
 * <p>{@code group} is {@code null} for an indicator the catalogue offers with several groups (for
 * example {@code DDV-14-4}); the line's own group then decides the rate. A note is required unless
 * the impact is {@link VatImpact#STANDARD}: the gateway refuses an invalid note (E1032), and every
 * non-standard indicator in the catalogue carries one.
 */
public record TaxIndicator(String code, VatImpact vatImpact, VatGroup group, String note) {

    public TaxIndicator {
        code = Check.required(code, "UJP07-19", "taxIndicator.code", 30);
        Check.present(vatImpact, "UJP07-19", "taxIndicator.vatImpact");
        note = Check.optional(note, "UJP09-01-02", "vatTaxIndicatorNote", 200);
        Check.that(vatImpact == VatImpact.STANDARD || note != null, "UJP09-01-02", "vatTaxIndicatorNote",
                "is required for " + code + ", whose impact is " + vatImpact);
    }

    /** The plain indicator of a rate group: {@code DDV-A}, {@code DDV-V}, {@code DDV-B}. */
    public static TaxIndicator standard(VatGroup group) {
        return new TaxIndicator(group.code(), VatImpact.STANDARD, group, null);
    }
}
