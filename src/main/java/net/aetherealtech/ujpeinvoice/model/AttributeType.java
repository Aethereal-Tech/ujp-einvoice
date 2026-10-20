package net.aetherealtech.ujpeinvoice.model;

/** The format of an attribute's value (UJP07-26-03, UJP12-01-03). */
public enum AttributeType {
    /** Text. */
    STR,
    /** A decimal number. */
    DEC,
    /** A whole number. */
    INT,
    /** A date, {@code YYYY-MM-DD}. */
    DATE
}
