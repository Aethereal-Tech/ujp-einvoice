package net.aetherealtech.ujpeinvoice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a rule, a wire field name, a code value or a JSON shape that no obtained UJP document
 * settles: the documents conflict with each other or with their own examples, are silent, or leave
 * the choice to a schema nobody has published. The element states what is open and why.
 *
 * <p>See {@code openspec/specs/specification-status/spec.md} for the full list of what carries this
 * marker. Per {@code CLAUDE.md}, nothing marked {@code @ProvisionalSpec} may be promoted to verified
 * without citing the specific section of the official spec that confirms it. A sandbox account
 * behaving as expected is evidence, not confirmation.
 *
 * <p>Source-retention only: this is a documentation aid for readers of this code, not a runtime
 * contract anything reflects on.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE, ElementType.RECORD_COMPONENT})
public @interface ProvisionalSpec {

    /** What, specifically, is unverified about this element. */
    String value() default "Not confirmed against the official UJP e-Faktura API specification.";
}
