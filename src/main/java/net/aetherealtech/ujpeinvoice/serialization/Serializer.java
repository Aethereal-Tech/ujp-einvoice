package net.aetherealtech.ujpeinvoice.serialization;

import net.aetherealtech.ujpeinvoice.model.Invoice;

/**
 * Turns an {@link Invoice} into the bytes a gateway expects on the wire.
 *
 * <p>One implementation ships with this library: {@link UjpJsonSerializer}, whose javadoc and the
 * project README's "Specification status" section explain exactly which parts of its output are
 * confirmed against the official UJP schema and which are this library's best reconstruction.
 *
 * <p>This interface is the seam that keeps that uncertainty from spreading. A corrected schema, or
 * an entirely different wire format such as UBL 2.1, is a new implementation of {@code Serializer} —
 * not a change to {@link Invoice} or to anything in {@code net.aetherealtech.ujpeinvoice.signing} or
 * {@code .transport}, both of which depend only on this interface and never on the JSON shape
 * underneath it.
 */
public interface Serializer {

    /** Renders {@code invoice} as wire bytes in this serializer's format. */
    byte[] serialize(Invoice invoice);

    /** The MIME content type of {@link #serialize}'s output (e.g. for an HTTP {@code Content-Type}). */
    String contentType();
}
