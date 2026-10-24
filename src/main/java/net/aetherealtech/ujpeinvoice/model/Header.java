package net.aetherealtech.ujpeinvoice.model;

import java.time.LocalDate;

/**
 * The general information of a document (UJP01). The rules that depend on the document's kind (the
 * turnover date, the length of the id, which indicator is valid) are checked by {@link Document}.
 *
 * <p>{@code alternativeMode} is a boolean here whatever the field is on the wire: the gateway's
 * changelog types it Boolean (14.09), the type pages Integer (08.10), and every 21.10 example of
 * 100 sends {@code false}.
 */
public record Header(String number, String id, LocalDate date, LocalDate turnoverDate, LocalDate deliveryDate,
                     String delivery, LocalDate periodStart, LocalDate periodEnd, String notes,
                     String headerText, String footerText, DocIndicator indicator, boolean alternativeMode) {

    public Header {
        number = Check.required(number, "UJP01-09", "number", 30);
        Check.required(id, "UJP01-11", "id", 30);
        Check.present(date, "UJP01-03", "date");
        delivery = Check.optional(delivery, "UJP01-05", "delivery", 30);
        Check.that(periodStart == null || periodEnd == null || !periodEnd.isBefore(periodStart), "UJP01-08",
                "periodEnd", "must not precede periodStart (UJP01-07)");
        notes = Check.optional(notes, "UJP01-10", "notes", 300);
        headerText = Check.optional(headerText, "UJP01-12", "headerText", 300);
        footerText = Check.optional(footerText, "UJP01-13", "footerText", 300);
    }

    /** A header of the three mandatory facts; the turnover date is added where the kind needs it. */
    public static Header of(String number, String id, LocalDate date) {
        return new Header(number, id, date, null, null, null, null, null, null, null, null, null, false);
    }

    public Header withTurnoverDate(LocalDate value) {
        return new Header(number, id, date, value, deliveryDate, delivery, periodStart, periodEnd, notes,
                headerText, footerText, indicator, alternativeMode);
    }

    public Header withDelivery(LocalDate deliveryDateValue, String deliveryValue) {
        return new Header(number, id, date, turnoverDate, deliveryDateValue, deliveryValue, periodStart,
                periodEnd, notes, headerText, footerText, indicator, alternativeMode);
    }

    public Header withPeriod(LocalDate start, LocalDate end) {
        return new Header(number, id, date, turnoverDate, deliveryDate, delivery, start, end, notes,
                headerText, footerText, indicator, alternativeMode);
    }

    public Header withNotes(String value) {
        return new Header(number, id, date, turnoverDate, deliveryDate, delivery, periodStart, periodEnd, value,
                headerText, footerText, indicator, alternativeMode);
    }

    public Header withTexts(String headerValue, String footerValue) {
        return new Header(number, id, date, turnoverDate, deliveryDate, delivery, periodStart, periodEnd, notes,
                headerValue, footerValue, indicator, alternativeMode);
    }

    public Header withIndicator(DocIndicator value) {
        return new Header(number, id, date, turnoverDate, deliveryDate, delivery, periodStart, periodEnd, notes,
                headerText, footerText, value, alternativeMode);
    }

    public Header withAlternativeMode(boolean value) {
        return new Header(number, id, date, turnoverDate, deliveryDate, delivery, periodStart, periodEnd, notes,
                headerText, footerText, indicator, value);
    }
}
