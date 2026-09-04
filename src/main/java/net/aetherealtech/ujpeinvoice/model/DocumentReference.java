package net.aetherealtech.ujpeinvoice.model;

import java.time.LocalDate;

/**
 * The document a credit or debit note corrects, named the way a recipient can find it again: its
 * number and the date it was issued.
 *
 * <p>This library cannot check that the referenced document exists — it has never seen it. A
 * consumer that keeps its own invoices should check before building the note, so the refusal
 * reaches the user with something useful to say.
 */
public record DocumentReference(String number, LocalDate issueDate) {

    public DocumentReference {
        if (number == null || number.isBlank()) {
            throw new InvoiceValidationException("DocumentReference.number must not be blank");
        }
        if (issueDate == null) {
            throw new InvoiceValidationException("DocumentReference.issueDate must not be null");
        }
    }
}
