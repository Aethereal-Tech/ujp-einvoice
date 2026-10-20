package net.aetherealtech.ujpeinvoice.transport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * A submitted document's current status: the {@code docStatus} of the answer to
 * {@code POST /documents/sales-invoice/current-status} (the API description's {@code InvoiceStatus}, and D1
 * „Тековен статус на излезна фактура").
 *
 * <p>The status is the pair the gateway served, {@link #code()} and {@link #name()}, and never a closed enum:
 * the vocabulary is a code list the gateway serves and grows (statuses {@code 12} and {@code 13} were added on
 * 11.09.2026), so a code this library has not met passes through unchanged. The ten codes of UJP's statuses
 * page (efakturawiki.ujp.gov.mk, {@code /status}, {@code updated-at} 2026-10-15, retrieved 2026-10-19) are
 * named constants for convenience; {@link #isKnown()} says whether a code is one of them. {@link #name()} is the
 * name as served, in Macedonian.
 *
 * <p>The amounts are as the gateway served them: {@code docNetAmount}, {@code docVatAmount},
 * {@code docGrossAmountR} (rounded gross), {@code docAvansAmount} (advance) and {@code docFinalAmount}. Every
 * member may be {@code null}: a member that is missing, or of another type, or a date that does not parse, reads
 * as {@code null} rather than failing the poll. {@code responseBody} is the body exactly as received.
 */
public record SubmissionStatus(String code, String name, String euid, String docNumber, String docType,
        String docTypeName, LocalDate docDate, LocalDate docDeliveryDate, BigDecimal netAmount, BigDecimal vatAmount,
        BigDecimal grossAmountRounded, BigDecimal advanceAmount, BigDecimal finalAmount, String sellerTin,
        String buyerTin, String senderTin, String receiverTin, String comment, String cancelComment,
        StatusReason voidReason, StatusReason rejectReason, StatusReason correctionReason, String responseBody) {

    /** 00 Нацрт: created, not final, not sent to the recipient. */
    public static final String DRAFT = "00";
    /** 01 Испратена (Нова): issued, signed, validated and stored. */
    public static final String SENT = "01";
    /** 03 Прифатена: accepted by the recipient. */
    public static final String ACCEPTED = "03";
    /** 04 Автоматски прифатена: accepted automatically when the deadline passed. */
    public static final String AUTO_ACCEPTED = "04";
    /** 05 Одбиена: rejected by the recipient, with a reason. */
    public static final String REJECTED = "05";
    /** 07 Сторнирана: cancelled by the issuer. */
    public static final String VOIDED = "07";
    /** 09 Корегирана: corrected by the issuer. */
    public static final String CORRECTED = "09";
    /** 10 Евидентирана: recorded; the recipient cannot accept or reject (a foreign company or a natural person). */
    public static final String RECORDED = "10";
    /** 12 Сторно: a storno document, sent to cancel an issued one. */
    public static final String STORNO = "12";
    /** 13 Корективна: a corrective document, sent to replace a wrong one. */
    public static final String CORRECTIVE = "13";

    private static final Set<String> KNOWN = Set.of(DRAFT, SENT, ACCEPTED, AUTO_ACCEPTED, REJECTED, VOIDED,
            CORRECTED, RECORDED, STORNO, CORRECTIVE);

    /** Whether {@link #code()} is one of the ten codes of the statuses page. */
    public boolean isKnown() {
        return KNOWN.contains(code);
    }

    /** Whether the served code is {@code expected}, for example {@code status.is(SubmissionStatus.ACCEPTED)}. */
    public boolean is(String expected) {
        return expected != null && expected.equals(code);
    }
}
