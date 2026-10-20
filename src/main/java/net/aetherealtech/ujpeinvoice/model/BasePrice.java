package net.aetherealtech.ujpeinvoice.model;

import java.math.BigDecimal;

/**
 * The price before the amounts article 17 of the VAT law adds to the tax base, such as an excise
 * duty (UJP07-22 to UJP07-25). {@code base + add - deduct} must equal the line's unit price; a line
 * that does not add up is refused. An absent {@code add} or {@code deduct} counts as 0.
 */
public record BasePrice(BigDecimal base, BigDecimal add, BigDecimal deduct, String type) {

    public BasePrice {
        Check.present(base, "UJP07-22", "basePrice.base");
        add = add == null ? BigDecimal.ZERO : add;
        deduct = deduct == null ? BigDecimal.ZERO : deduct;
        Check.that(base.signum() >= 0, "UJP07-22", "basePrice.base", "must not be negative");
        Check.that(add.signum() >= 0, "UJP07-23", "basePrice.add", "is a positive number");
        Check.that(deduct.signum() >= 0, "UJP07-24", "basePrice.deduct", "is a positive number");
        Check.decimals(base, 4, "UJP07-22", "basePrice.base");
        Check.decimals(add, 4, "UJP07-23", "basePrice.add");
        Check.decimals(deduct, 4, "UJP07-24", "basePrice.deduct");
        type = Check.optional(type, "UJP07-25", "basePrice.type", 30);
    }

    BigDecimal unitPrice() {
        return base.add(add).subtract(deduct);
    }
}
