package net.aetherealtech.ujpeinvoice.transport;

/**
 * A reason the gateway attaches to a document's status: a void, a rejection or a correction. {@code code} is
 * from the reason catalogues the gateway serves (for example {@code S-1}, {@code C01}); {@code text} is the
 * reason's name and {@code comment} the free text, which the gateway serves for a void and a correction and
 * not for a rejection (the API description's {@code InvoiceStatus} has no reject comment). Every member may be
 * {@code null}.
 */
public record StatusReason(String code, String text, String comment) {
}
