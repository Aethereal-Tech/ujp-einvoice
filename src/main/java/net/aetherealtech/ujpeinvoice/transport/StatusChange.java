package net.aetherealtech.ujpeinvoice.transport;

import java.time.LocalDateTime;

/**
 * One entry of a document's status history: the API description's {@code DocumentStatusChange}, of the
 * response to {@code POST /documents/sales-invoice/status}. The old and new status are the served code and name
 * ({@link SubmissionStatus} documents why the pair is not an enum); the reasons are as for
 * {@link SubmissionStatus}. Every member may be {@code null}.
 */
public record StatusChange(String euid, String oldStatusCode, String oldStatusName, String newStatusCode,
        String newStatusName, LocalDateTime timestamp, String comment, String cancelComment, StatusReason voidReason,
        StatusReason rejectReason) {
}
