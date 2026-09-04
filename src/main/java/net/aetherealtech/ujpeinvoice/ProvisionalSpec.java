package net.aetherealtech.ujpeinvoice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a wire field name, code value, or JSON shape as RECONSTRUCTED rather than confirmed: it
 * comes from third-party integrator accounts and plausible pattern-matching against the little that
 * is publicly documented, not from having read efakturawiki.ujp.gov.mk's own
 * "API Спецификација" (unreachable outside North Macedonian networks as of this writing).
 *
 * <p>See {@code SPECS.md}'s "Specification inventory" section for the full list of what carries this
 * marker and why. Per {@code CLAUDE.md}, nothing marked {@code @ProvisionalSpec} may be
 * promoted to verified without citing the specific section of the official spec that confirms it.
 *
 * <p>Source-retention only: this is a documentation aid for readers of this code, not a runtime
 * contract anything reflects on.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
public @interface ProvisionalSpec {

    /** What, specifically, is unverified about this element. */
    String value() default "Reconstructed from third-party integrator accounts; not confirmed "
            + "against the official UJP e-Faktura API specification.";
}
