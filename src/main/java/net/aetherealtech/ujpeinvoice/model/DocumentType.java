package net.aetherealtech.ujpeinvoice.model;

/**
 * What a document is, in North Macedonian VAT practice: an invoice („фактура"), a credit note
 * („книжно одобрение") lowering what an earlier invoice charged, or a debit note
 * („книжно задолжување") raising it. The two note types always name the invoice they correct — see
 * {@link DocumentReference} and the check in {@link Invoice}'s constructor.
 *
 * <h2>Amounts on a note stay positive</h2>
 * <p>A credit note for 1.000 MKD carries a line of 1.000, never −1.000: the type carries the
 * direction, the amounts carry the magnitude. Signing the amounts instead would state the same fact
 * twice and let the two disagree, and it would force {@link LineItem}'s "quantity must be positive"
 * rule open for notes — after which an ordinary typo on an ordinary invoice would stop being
 * caught. A consumer that needs a signed figure applies the sign at the point it needs it, from
 * this type.
 *
 * <p>How a type is <em>named</em> on the wire is unverified and lives in the serializer, not here:
 * this model does not know JSON exists.
 */
public enum DocumentType {

    /** An ordinary sales invoice, correcting nothing. */
    INVOICE,

    /** „Книжно одобрение" — reduces what the referenced invoice charged. */
    CREDIT_NOTE,

    /** „Книжно задолжување" — increases what the referenced invoice charged. */
    DEBIT_NOTE;

    /** Whether a document of this type corrects an earlier one, and so must reference it. */
    public boolean corrects() {
        return this != INVOICE;
    }
}
