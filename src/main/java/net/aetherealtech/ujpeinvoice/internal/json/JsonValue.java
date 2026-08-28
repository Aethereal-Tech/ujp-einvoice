package net.aetherealtech.ujpeinvoice.internal.json;

/**
 * A minimal JSON value tree: just enough of RFC 8259 to write and read this library's own fixed
 * wire shapes. Not a general-purpose JSON library — no streaming, no annotations, no reflection —
 * because nothing here ever needs to handle a schema this library did not define itself.
 */
public sealed interface JsonValue permits JsonObject, JsonArray, JsonString, JsonNumber, JsonBoolean, JsonNull {
}
