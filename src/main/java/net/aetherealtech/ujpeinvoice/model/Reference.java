package net.aetherealtech.ujpeinvoice.model;

import java.time.LocalDate;

/**
 * A document this one refers to (UJP02-01): a delivery note, an order, or — for a storno or a
 * correction — the original. Once a reference is used all four parts are mandatory, which the number
 * of a reference to an e-invoice (its EUID, 36 characters) fits.
 */
public record Reference(RefDocumentType type, LocalDate date, String number) {

    public Reference {
        Check.present(type, "UJP02-01-01", "reference.type");
        Check.present(date, "UJP02-01-03", "reference.date");
        number = Check.required(number, "UJP02-01-04", "reference.number", 36);
    }
}
