package net.aetherealtech.ujpeinvoice.model;

import net.aetherealtech.ujpeinvoice.ProvisionalSpec;

/**
 * The document kinds this library ships rules for, each with the code UJP gives it and the rules
 * its type page states. The pages are near copies with silent drift, so a rule is stated per kind
 * rather than assumed to carry across. Kinds beyond this set (170, 200, 220, ...) are a new constant
 * when one is needed, not a new type.
 *
 * <p>Rules are read from the pages as of 2026-10-14 (110, 120, 130, 310) and 2026-10-16 (100); the
 * UJP code of each field is cited in {@link DocumentValidationException} messages, and the
 * conflicts between a page and its own example are listed in {@code specification-status}.
 */
public enum DocumentKind {

    INVOICE("100", "Фактура"),
    CREDIT_NOTE("110", "Книжно одобрение"),
    DEBIT_NOTE("120", "Книжно задолжение"),
    ADVANCE_INVOICE("130", "Авансна фактура"),
    CASH_TURNOVER("310", "Фактура за регистриран готовински промет");

    private final String code;
    private final String ujpName;

    DocumentKind(String code, String ujpName) {
        this.code = code;
        this.ujpName = ujpName;
    }

    /** UJP01-01: the code, e.g. {@code 100}. */
    public String code() {
        return code;
    }

    /** UJP01-02: the name the code list gives the kind, e.g. {@code Фактура}. */
    public String ujpName() {
        return ujpName;
    }

    /** UJP01-06 is mandatory on 100 and 310 and has no row on 110, 120 and 130. */
    public boolean requiresTurnoverDate() {
        return this == INVOICE || this == CASH_TURNOVER;
    }

    /**
     * UJP02-01 is marked mandatory on 110 and 120, optional on the others. The model does not refuse
     * an empty list on 110 or 120: the 110 and 120 examples send {@code []}, and the page's own note
     * makes the four sub-fields conditional on a reference being used at all. The accessor reports
     * the marker for a caller that wants to enforce it.
     */
    @ProvisionalSpec("UJP02-01 is З on 110 and 120 but their examples (unchanged from 08.10 to 21.10) "
            + "send an empty docReferences; the model reports the marker and does not enforce it")
    public boolean requiresReferences() {
        return this == CREDIT_NOTE || this == DEBIT_NOTE;
    }

    /** 110 and 120 have no payment group (UJP10) in their table or example. */
    public boolean allowsPayment() {
        return this != CREDIT_NOTE && this != DEBIT_NOTE;
    }

    /** UJP10-01 and UJP10-02 are ЗШ and ЗП on 130 and 310, optional on 100. */
    public boolean requiresPaymentType() {
        return this == ADVANCE_INVOICE || this == CASH_TURNOVER;
    }

    /** UJP08-06 to UJP08-08, the advance deducted, are on 100's table only. */
    public boolean allowsAdvanceDeduction() {
        return this == INVOICE;
    }

    /** Attribute groups (UJP07-26, UJP12-01) were added to 100 on 15.10 and to no other kind. */
    public boolean allowsAttributes() {
        return this == INVOICE;
    }

    /**
     * UJP10-10 and UJP10-11 are listed on 130 and 310. The 15.10 changelog removed them from 100,
     * and 110 and 120 have no payment group.
     */
    public boolean listsCurrencyRate() {
        return this == ADVANCE_INVOICE || this == CASH_TURNOVER;
    }

    /** UJP07-07 is mandatory on 130 and 310, optional on 100, 110 and 120. */
    public boolean requiresUnit() {
        return this == ADVANCE_INVOICE || this == CASH_TURNOVER;
    }

    /** UJP01-11: 30, and 12 on 310. */
    public int docIdMaxLength() {
        return this == CASH_TURNOVER ? 12 : 30;
    }

    /** UJP03-06 and UJP05-06: 150, and 80 on 310. */
    public int partyNameMaxLength() {
        return this == CASH_TURNOVER ? 80 : 150;
    }

    /** UJP03-07-01 and UJP05-07-01: 80, and 60 on 310. */
    public int streetMaxLength() {
        return this == CASH_TURNOVER ? 60 : 80;
    }

    /** UJP03-07-02 and UJP05-07-02: 20, and 10 on 310. */
    public int streetNumberMaxLength() {
        return this == CASH_TURNOVER ? 10 : 20;
    }

    /** UJP03-09 and UJP05-09: 150, and 100 on 310. */
    public int subsidiaryNameMaxLength() {
        return this == CASH_TURNOVER ? 100 : 150;
    }
}
