package net.aetherealtech.ujpeinvoice.model;

/**
 * A domain object failed a structural or arithmetic invariant: a blank required field, a negative
 * quantity, or totals that do not reconcile with their line items. Thrown eagerly, from the
 * constructor that would otherwise let the invalid object exist, so nothing downstream (serializer,
 * signer, transport) ever has to re-check what this layer already guarantees.
 */
public final class InvoiceValidationException extends RuntimeException {

    public InvoiceValidationException(String message) {
        super(message);
    }
}
