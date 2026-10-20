package net.aetherealtech.ujpeinvoice.model;

import java.time.LocalDate;
import java.util.Currency;

/**
 * How and when a document is paid (UJP10). The type is optional on a 100 and mandatory on 130 and
 * 310; {@link Document} enforces that per kind. The currency defaults to MKD, the currency of every
 * example.
 */
public record Payment(PaymentType type, Integer dueDays, LocalDate dueDate, String terms, String interest,
                      String discount, String note, Currency currency) {

    public static final Currency MKD = Currency.getInstance("MKD");

    public Payment {
        Check.that(dueDays == null || (dueDays >= 0 && dueDays <= 9999), "UJP10-03", "dueDays",
                "is a number of days from 0 to 9999");
        terms = Check.optional(terms, "UJP10-05", "terms", 200);
        interest = Check.optional(interest, "UJP10-06", "interest", 100);
        discount = Check.optional(discount, "UJP10-07", "discount", 100);
        note = Check.optional(note, "UJP10-12", "note", 100);
        currency = currency == null ? MKD : currency;
    }

    /** Paid by this type, in MKD, with nothing else stated. */
    public static Payment of(PaymentType type) {
        return new Payment(type, null, null, null, null, null, null, MKD);
    }
}
