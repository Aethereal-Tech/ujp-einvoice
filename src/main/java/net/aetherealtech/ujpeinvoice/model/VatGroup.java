package net.aetherealtech.ujpeinvoice.model;

import java.math.BigDecimal;

/**
 * The four tax-rate groups (the code list for UJP07-14 and UJP09-01-03). The letters are not
 * alphabetical by rate: {@code DDV-V} is 10 and {@code DDV-B} is 5. The rates themselves are public
 * tax law, independent of the wire format.
 */
public enum VatGroup {

    DDV_A("DDV-A", 18),
    DDV_V("DDV-V", 10),
    DDV_B("DDV-B", 5),
    DDV_G("DDV-G", 0);

    private final String code;
    private final BigDecimal percent;

    VatGroup(String code, int percent) {
        this.code = code;
        this.percent = BigDecimal.valueOf(percent);
    }

    /** The group's code as UJP's code list spells it, e.g. {@code DDV-A}. */
    public String code() {
        return code;
    }

    /** The rate as a percentage: 18, 10, 5 or 0 (UJP07-13, UJP09-01-04). */
    public BigDecimal percent() {
        return percent;
    }

    /** The group with this code, or {@code null} when the code names none. */
    public static VatGroup fromCode(String code) {
        for (VatGroup group : values()) {
            if (group.code.equals(code)) {
                return group;
            }
        }
        return null;
    }
}
