package net.aetherealtech.ujpeinvoice.model;

import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * A document this one refers to (UJP02-01): a delivery note, an order, or — for a storno or a
 * correction — the original. Once a reference is used all four parts are mandatory, which the number
 * of a reference to an e-invoice (its EUID, 36 characters) fits.
 *
 * <p>On type ST or KR the number must be a valid EUID (changelog 22.10.2026). The check is the
 * shape of the textual UUID, 8-4-4-4-12 hexadecimal digits: the published EUIDs are version 7, but
 * no document says the gateway pins the version, and only the gateway knows whether the document
 * exists. Any other type may carry a number that is not an EUID (UJP02-01-04).
 */
public record Reference(RefDocumentType type, LocalDate date, String number) {

    private static final Pattern EUID = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    public Reference {
        Check.present(type, "UJP02-01-01", "reference.type");
        Check.present(date, "UJP02-01-03", "reference.date");
        number = Check.required(number, "UJP02-01-04", "reference.number", 36);
        Check.that(!type.namesAnOriginal() || EUID.matcher(number).matches(), "UJP02-01-04", "reference.number",
                "must be the EUID of the original (8-4-4-4-12 hexadecimal digits) on type " + type.code()
                        + " (changelog 22.10.2026)");
    }
}
