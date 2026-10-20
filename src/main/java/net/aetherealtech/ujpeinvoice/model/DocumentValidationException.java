package net.aetherealtech.ujpeinvoice.model;

/**
 * A document failed a rule UJP states for its kind: a mandatory field is missing, a value is longer
 * than the page allows, or an amount the model derives would disagree with what was given. The
 * message names the field and cites its UJP code (for example "UJP01-06 turnoverDate is required on
 * a 100"), so a caller can find the rule on the type page. Thrown from the constructor that would
 * otherwise let the invalid object exist, before anything is serialized or signed.
 */
public final class DocumentValidationException extends RuntimeException {

    public DocumentValidationException(String message) {
        super(message);
    }
}
