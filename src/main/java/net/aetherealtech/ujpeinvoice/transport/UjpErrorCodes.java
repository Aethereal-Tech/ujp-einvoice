package net.aetherealtech.ujpeinvoice.transport;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The error catalogue as D5 published it: efakturawiki.ujp.gov.mk, {@code /error-codes}, page
 * {@code updated-at} 2026-10-05, retrieved 2026-10-19 (sha256 of the retrieved HTML
 * {@code 246fda344dd256e511337b88956839cc8cc1ba378db872032ddc2f7e2a769d50}). Every entry on the page is here, in
 * the page's order, under the section it appears in; meanings are the page's own text.
 *
 * <p>A code is identified by its {@link ErrorNamespace} and its text together. E1020 is "This certificate for
 * this company already exists!" in eInvoiceApi and "Invalid calculated totals" in the JSON Receiver, and E1019
 * is a missing status-history list in one and an excessive date range in another; {@link #lookup} therefore
 * never searches across namespaces.
 *
 * <p>A code the page does not list is not in the catalogue, whatever else is known about it: the OpenAPI
 * description's {@code ErrorStatusBean} enum names 17 eInvoiceApi codes that D5 does not (E1003, E1005, E1007,
 * E1008, E4006, E4017, E4018, E4019, E4020, E4021, E4022, E4024, E4027, E5009, E5010, E5012 and E5013), and no
 * meaning is invented for them. E4018–E4022 and E4024 and E5012 are listed by D5 under the JSON Receiver only.
 * An unlisted code resolves to {@link Optional#empty()}, which is the honest answer.
 *
 * <p>This is a snapshot: UJP adds codes without notice (the 2026-10-02 changelog entry announced new ones), so
 * an empty lookup means "not in the 2026-10-05 catalogue", not "not an error code".
 */
public final class UjpErrorCodes {

    private static final Map<ErrorNamespace, List<UjpErrorCode>> BY_NAMESPACE = catalogue();
    private static final Map<ErrorNamespace, Map<String, UjpErrorCode>> INDEX = index();

    private UjpErrorCodes() {
    }

    /** The entry D5 lists under {@code namespace} for {@code code}, or empty if it lists none. */
    public static Optional<UjpErrorCode> lookup(ErrorNamespace namespace, String code) {
        if (namespace == null || code == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(INDEX.get(namespace).get(code));
    }

    /** Every entry D5 lists under {@code namespace}, in the page's order. */
    public static List<UjpErrorCode> all(ErrorNamespace namespace) {
        return BY_NAMESPACE.get(namespace);
    }

    private static Map<ErrorNamespace, List<UjpErrorCode>> catalogue() {
        Map<ErrorNamespace, List<UjpErrorCode>> catalogue = new EnumMap<>(ErrorNamespace.class);
        catalogue.put(ErrorNamespace.EINVOICE_API, einvoiceApi());
        catalogue.put(ErrorNamespace.JSON_RECEIVER, jsonReceiver());
        catalogue.put(ErrorNamespace.WEB_APP, webApp());
        return catalogue;
    }

    private static Map<ErrorNamespace, Map<String, UjpErrorCode>> index() {
        Map<ErrorNamespace, Map<String, UjpErrorCode>> index = new EnumMap<>(ErrorNamespace.class);
        BY_NAMESPACE.forEach((namespace, entries) -> {
            Map<String, UjpErrorCode> byCode = new HashMap<>();
            entries.forEach(entry -> byCode.put(entry.code(), entry));
            index.put(namespace, byCode);
        });
        return index;
    }

    private static List<UjpErrorCode> einvoiceApi() {
        ErrorNamespace ns = ErrorNamespace.EINVOICE_API;
        return List.of(
                // Business / Domain Errors (E1XXX)
                new UjpErrorCode(ns, "E1000", "Company not found"),
                new UjpErrorCode(ns, "E1001", "Country not found"),
                new UjpErrorCode(ns, "E1002", "Reject reason not found"),
                new UjpErrorCode(ns, "E1004", "Tax group not found"),
                new UjpErrorCode(ns, "E1006", "Void reason not found"),
                new UjpErrorCode(ns, "E1009", "Tax indicator not found"),
                new UjpErrorCode(ns, "E1010", "User not found"),
                new UjpErrorCode(ns, "E1011", "Privilege not found"),
                new UjpErrorCode(ns, "E1012", "Certificate not found"),
                new UjpErrorCode(ns, "E1013", "User is not authorized to sign documents for this company"),
                new UjpErrorCode(ns, "E1014", "Tax payer not found"),
                new UjpErrorCode(ns, "E1015", "Status History not found."),
                new UjpErrorCode(ns, "E1016", "Document is not in status NEW. Cannot be accepted, rejected, or cancelled."),
                new UjpErrorCode(ns, "E1017", "Document type not found"),
                new UjpErrorCode(ns, "E1018", "Document Status not found"),
                new UjpErrorCode(ns, "E1019", "Status History List not found."),
                new UjpErrorCode(ns, "E1020", "This certificate for this company already exists!"),
                new UjpErrorCode(ns, "E1021", "Document LTV data for this euid '%s' not found"),
                new UjpErrorCode(ns, "E1022", "Document status cannot be changed"),
                new UjpErrorCode(ns, "E1023", "Report date range exceeds allowed maximum limit"),
                new UjpErrorCode(ns, "E1024", "Purchase VAT taxable amount cannot be greater than document taxable amount"),
                // Client / Request Errors (E4XXX)
                new UjpErrorCode(ns, "E4000", "Bad request"),
                new UjpErrorCode(ns, "E4001", "Error binding request parameters"),
                new UjpErrorCode(ns, "E4002", "Missing required request parameter"),
                new UjpErrorCode(ns, "E4003", "Missing required path variable"),
                new UjpErrorCode(ns, "E4004", "No handler found for HTTP method and URL"),
                new UjpErrorCode(ns, "E4005", "HTTP method not supported for this endpoint"),
                new UjpErrorCode(ns, "E4007", "Rate limit exceeded"),
                new UjpErrorCode(ns, "E4008", "Requested resource not found"),
                new UjpErrorCode(ns, "E4009", "Asynchronous request timeout occurred"),
                new UjpErrorCode(ns, "E4010", "Invalid value type provided in request"),
                new UjpErrorCode(ns, "E4011", "Required request body is missing"),
                new UjpErrorCode(ns, "E4012", "Request timestamp validation failed"),
                new UjpErrorCode(ns, "E4013", "JSON schema validation failed"),
                new UjpErrorCode(ns, "E4014", "Allowed date range exceeded (maximum 1 month)"),
                new UjpErrorCode(ns, "E4015", "Invalid signature"),
                new UjpErrorCode(ns, "E4016", "Missing required request headers"),
                new UjpErrorCode(ns, "E4023", "Notification with ID was not found"),
                new UjpErrorCode(ns, "E4025", "Unable to validate certificate using CRL"),
                // Server / Processing Errors (E5XXX)
                new UjpErrorCode(ns, "E5000", "Internal server error"),
                new UjpErrorCode(ns, "E5001", "Internal error occurred while processing the request"),
                new UjpErrorCode(ns, "E5002", "Certificate expired"),
                new UjpErrorCode(ns, "E5003", "Certificate revoked or unverified (OCSP failure)"),
                new UjpErrorCode(ns, "E5004", "Certificate validation failed"),
                new UjpErrorCode(ns, "E5005", "JWS validation failed"),
                new UjpErrorCode(ns, "E5006", "Tax payer validation failed"),
                new UjpErrorCode(ns, "E5007", "Document validation failed"),
                new UjpErrorCode(ns, "E5008", "Failed to fetch TimeStampToken"),
                new UjpErrorCode(ns, "E5011", "Invalid value for field"),
                new UjpErrorCode(ns, "E5014", "The allowed period for modifying Purchase VAT totals has expired"),
                new UjpErrorCode(ns, "E5015", "Duplicate purchase VAT total records found. The combination must be unique."),
                new UjpErrorCode(ns, "E5016", "Operation is not allowed for documents with status"),
                new UjpErrorCode(ns, "E5017", "Failed to generate ASiC container. Invalid LTV data."),
                new UjpErrorCode(ns, "E5018", "Attribute value validation failed."),
                new UjpErrorCode(ns, "E5019", "Attribute type is required.")
        );
    }

    private static List<UjpErrorCode> jsonReceiver() {
        ErrorNamespace ns = ErrorNamespace.JSON_RECEIVER;
        return List.of(
                // Business / Domain Errors (E1XXX)
                new UjpErrorCode(ns, "E1000", "Company not found"),
                new UjpErrorCode(ns, "E10001", "Missing TIN during company validation process"),
                new UjpErrorCode(ns, "E10002", "An incorrect company name was provided"),
                new UjpErrorCode(ns, "E10003", "An incorrect VAT number was provided for the company"),
                new UjpErrorCode(ns, "E10004", "An incorrect city was provided for the company"),
                new UjpErrorCode(ns, "E10005", "An incorrect postal code was provided for the company"),
                new UjpErrorCode(ns, "E10006", "An incorrect company name was provided"),
                new UjpErrorCode(ns, "E10007", "An incorrect VAT number was provided for the company"),
                new UjpErrorCode(ns, "E10008", "VatNumber missing!"),
                new UjpErrorCode(ns, "E1001", "Country not found"),
                new UjpErrorCode(ns, "E10011", "A non-existent country was provided for the company"),
                new UjpErrorCode(ns, "E10012", "An incorrect country code was provided for the company"),
                new UjpErrorCode(ns, "E1002", "Reject reason not found"),
                new UjpErrorCode(ns, "E1004", "Tax group not found"),
                new UjpErrorCode(ns, "E1006", "Void reason not found"),
                new UjpErrorCode(ns, "E1009", "Tax indicator not found"),
                new UjpErrorCode(ns, "E1010", "User not found"),
                new UjpErrorCode(ns, "E1011", "Privilege not found"),
                new UjpErrorCode(ns, "E1012", "Certificate not found"),
                new UjpErrorCode(ns, "E1013", "User is not authorized to sign documents for this company"),
                new UjpErrorCode(ns, "E1014", "Tax payer not found"),
                new UjpErrorCode(ns, "E1015", "Status History not found."),
                new UjpErrorCode(ns, "E1016", "Document is not saved as NEW. Cannot be cancelled."),
                new UjpErrorCode(ns, "E1017", "Document type not found"),
                new UjpErrorCode(ns, "E1018", "Document Status not found"),
                new UjpErrorCode(ns, "E1019", "Status History List not found."),
                new UjpErrorCode(ns, "E1020", "Invalid calculated totals"),
                new UjpErrorCode(ns, "E10201", "Invalid calculated docTotals"),
                new UjpErrorCode(ns, "E10202", "Invalid calculated vatTotals"),
                new UjpErrorCode(ns, "E10203", "Invalid calculations for doc item"),
                new UjpErrorCode(ns, "E1021", "Document reference not found"),
                new UjpErrorCode(ns, "E10211", "Document reference name mismatch"),
                new UjpErrorCode(ns, "E1022", "Document already has been refrenced"),
                new UjpErrorCode(ns, "E1023", "Invoice already exists for that ID"),
                new UjpErrorCode(ns, "E1024", "Voided document must refrence a document"),
                new UjpErrorCode(ns, "E1025", "Invalid void request"),
                new UjpErrorCode(ns, "E1026", "Referenced document is voided"),
                new UjpErrorCode(ns, "E1027", "Corrected document must refrence a document"),
                new UjpErrorCode(ns, "E1028", "Referenced document is corrected"),
                new UjpErrorCode(ns, "E1029", "docTypeRef cannot be 'ST' for this document type"),
                new UjpErrorCode(ns, "E1030", "Correction documents must reference an existing document"),
                new UjpErrorCode(ns, "E1031", "Invalid correction request"),
                new UjpErrorCode(ns, "E1032", "Invalid Vat Tax Indicator Note"),
                new UjpErrorCode(ns, "E1033", "Invalid DocIndicator"),
                new UjpErrorCode(ns, "E1034", "Invalid DocAlt"),
                new UjpErrorCode(ns, "E1035", "Invalid Attribute"),
                // Client / Request Errors (E4XXX)
                new UjpErrorCode(ns, "E4000", "Bad request"),
                new UjpErrorCode(ns, "E4001", "Error binding request parameters"),
                new UjpErrorCode(ns, "E4002", "Missing required request parameter"),
                new UjpErrorCode(ns, "E4003", "Missing required path variable"),
                new UjpErrorCode(ns, "E4004", "No handler found for HTTP method and URL"),
                new UjpErrorCode(ns, "E4005", "HTTP method not supported for this endpoint"),
                new UjpErrorCode(ns, "E4007", "Rate limit exceeded"),
                new UjpErrorCode(ns, "E4008", "The requested resource could not be found"),
                new UjpErrorCode(ns, "E4009", "The request took too long to process.Asynchronous request timeout occurred."),
                new UjpErrorCode(ns, "E4010", "Invalid value type provided in request"),
                new UjpErrorCode(ns, "E4011", "Required request body is missing"),
                new UjpErrorCode(ns, "E4012", "Request timestamp validation failed"),
                new UjpErrorCode(ns, "E40121", "Problem parsing requestTimestamp"),
                new UjpErrorCode(ns, "E40122", "The requestTimestamp difference is greater than 5 minutes"),
                new UjpErrorCode(ns, "E4013", "JSON schema validation failed"),
                new UjpErrorCode(ns, "E40131", "Badly formatted JSON in JWS"),
                new UjpErrorCode(ns, "E4014", "The allowed date range is 1 month."),
                new UjpErrorCode(ns, "E4015", "Invalid signature"),
                new UjpErrorCode(ns, "E4016", "Missing required request headers"),
                new UjpErrorCode(ns, "E4018", "JSON serialization error"),
                new UjpErrorCode(ns, "E4019", "Missing required fields"),
                new UjpErrorCode(ns, "E4020", "Request header mismatch"),
                new UjpErrorCode(ns, "E4021", "Invalid X-SERIAL-NUMBER"),
                new UjpErrorCode(ns, "E4022", "Invalid X-EUJP-ID"),
                new UjpErrorCode(ns, "E4023", "Invalid X-EDB"),
                new UjpErrorCode(ns, "E4024", "Invalid request body"),
                // Server / Processing Errors (E5XXX)
                new UjpErrorCode(ns, "E5000", "Internal server error"),
                new UjpErrorCode(ns, "E5001", "Internal error occurred while processing the request"),
                new UjpErrorCode(ns, "E5002", "Certificate has expired."),
                new UjpErrorCode(ns, "E5003", "Certificate has been revoked or unverified according to OCSP response"),
                new UjpErrorCode(ns, "E5004", "Certificate validation failed"),
                new UjpErrorCode(ns, "E5005", "JWS validation failed"),
                new UjpErrorCode(ns, "E50051", "Invalid JWS signature (without validation)"),
                new UjpErrorCode(ns, "E5006", "Tax payer validation failed"),
                new UjpErrorCode(ns, "E5007", "Document validation failed"),
                new UjpErrorCode(ns, "E50071", "docDeliveryDate is not before docDate"),
                new UjpErrorCode(ns, "E50072", "docTurnoverDate is not before docDate"),
                new UjpErrorCode(ns, "E50073", "docDateRef is not before docDate"),
                new UjpErrorCode(ns, "E50074", "Payment type not found"),
                new UjpErrorCode(ns, "E50075", "Payment type description mismatch"),
                new UjpErrorCode(ns, "E50076", "Currency not found"),
                new UjpErrorCode(ns, "E50077", "Currency name mismatch"),
                new UjpErrorCode(ns, "E50078", "docPayment don't match"),
                new UjpErrorCode(ns, "E50079", "Document type mismatch"),
                new UjpErrorCode(ns, "E500710", "Document date mismatch"),
                new UjpErrorCode(ns, "E500711", "docDeliveryDate mismatch"),
                new UjpErrorCode(ns, "E500712", "docDelivery mismatch"),
                new UjpErrorCode(ns, "E500713", "docTurnoverDate mismatch"),
                new UjpErrorCode(ns, "E500714", "docPeriodStartDate mismatch"),
                new UjpErrorCode(ns, "E500715", "docPeriodEndDate mismatch"),
                new UjpErrorCode(ns, "E500716", "docNotes mismatch"),
                new UjpErrorCode(ns, "E500717", "docHeader mismatch"),
                new UjpErrorCode(ns, "E500718", "docFooter mismatch"),
                new UjpErrorCode(ns, "E500719", "docIndicator mismatch"),
                new UjpErrorCode(ns, "E500720", "docAlt mismatch"),
                new UjpErrorCode(ns, "E5008", "Fetching TimeStampToken failed"),
                new UjpErrorCode(ns, "E5011", "No OCSP responses for certificate"),
                new UjpErrorCode(ns, "E5012", "Certificate serial mismatch")
        );
    }

    private static List<UjpErrorCode> webApp() {
        ErrorNamespace ns = ErrorNamespace.WEB_APP;
        return List.of(
                // Domain Errors (E1XXX)
                new UjpErrorCode(ns, "E1000", "Company not found"),
                new UjpErrorCode(ns, "E1010", "User not found"),
                new UjpErrorCode(ns, "E1013", "User is not authorized to sign documents for this company"),
                new UjpErrorCode(ns, "E1014", "Tax payer not found"),
                new UjpErrorCode(ns, "E1017", "Document type not found"),
                new UjpErrorCode(ns, "E1018", "Unsigned document not found"),
                new UjpErrorCode(ns, "E1019", "Selected date range exceeds allowed maximum limit of days"),
                // Request Errors (E4XXX)
                new UjpErrorCode(ns, "E4000", "Bad request"),
                new UjpErrorCode(ns, "E4001", "Error binding request parameters"),
                new UjpErrorCode(ns, "E4002", "Missing required request parameter"),
                new UjpErrorCode(ns, "E4003", "Missing required path variable"),
                new UjpErrorCode(ns, "E4004", "No handler found for HTTP method and URL"),
                new UjpErrorCode(ns, "E4005", "HTTP method not supported for this endpoint"),
                new UjpErrorCode(ns, "E4007", "Rate limit exceeded"),
                new UjpErrorCode(ns, "E4008", "Requested resource not found"),
                new UjpErrorCode(ns, "E4009", "Asynchronous request timeout occurred"),
                new UjpErrorCode(ns, "E4010", "Invalid value type provided in request"),
                new UjpErrorCode(ns, "E4011", "Request body is missing or contains an invalid JSON payload"),
                new UjpErrorCode(ns, "E4012", "Request timestamp validation failed"),
                new UjpErrorCode(ns, "E4013", "JSON schema validation failed"),
                new UjpErrorCode(ns, "E4014", "Allowed date range exceeded (maximum 1 month)"),
                new UjpErrorCode(ns, "E4015", "Invalid signature"),
                new UjpErrorCode(ns, "E4016", "Missing required request headers"),
                // Processing Errors (E5XXX)
                new UjpErrorCode(ns, "E5000", "Internal server error"),
                new UjpErrorCode(ns, "E5001", "Internal error occurred while processing the request"),
                new UjpErrorCode(ns, "E5002", "Certificate has expired."),
                new UjpErrorCode(ns, "E5003", "Certificate revoked or unverified (OCSP failure)"),
                new UjpErrorCode(ns, "E5004", "Certificate validation failed"),
                new UjpErrorCode(ns, "E5005", "JWS validation failed"),
                new UjpErrorCode(ns, "E5006", "Tax payer validation failed"),
                new UjpErrorCode(ns, "E5007", "Document validation failed"),
                new UjpErrorCode(ns, "E5008", "Failed to fetch TimeStampToken")
        );
    }
}
