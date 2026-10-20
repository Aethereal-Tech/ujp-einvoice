package net.aetherealtech.ujpeinvoice.serialization;

import net.aetherealtech.ujpeinvoice.model.Document;

import java.time.LocalDateTime;

/**
 * Turns a {@link Document} into the bytes a gateway expects on the wire.
 *
 * <p>One implementation ships with this library: {@link UjpJsonSerializer}, the shape UJP's type
 * pages and worked examples describe. An entirely different wire format, such as UBL 2.1, would be
 * a new implementation of this interface, which {@code signing} and {@code transport} depend on
 * without knowing the JSON underneath.
 */
public interface Serializer {

    /**
     * Renders {@code document} as wire bytes in this serializer's format.
     *
     * @param requestTimestamp the request's timestamp, Skopje local time; the gateway refuses one more
     *                         than five minutes from its own clock, so the transport sources it from the
     *                         gateway's server-time service
     */
    byte[] serialize(Document document, LocalDateTime requestTimestamp);

    /** The MIME content type of {@link #serialize}'s output (e.g. for an HTTP {@code Content-Type}). */
    String contentType();
}
