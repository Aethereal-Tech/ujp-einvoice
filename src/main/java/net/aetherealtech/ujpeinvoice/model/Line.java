package net.aetherealtech.ujpeinvoice.model;

import net.aetherealtech.ujpeinvoice.internal.Money;

import java.math.BigDecimal;
import java.util.List;

/**
 * One line of a document (UJP07). The caller gives the quantity, the price before discount, the
 * discount per unit and the tax; every other figure on the line is derived here and never accepted,
 * so a line cannot disagree with its own arithmetic. Derived values are carried to four decimals and
 * are not rounded further: they are summed unrounded into the totals (see {@link Money}).
 *
 * <p>The formulas are those of the gateway's API description ("Валидација на износи и тотали"):
 * the unit VAT is the unit price after discount times the group's rate, and a total is the quantity
 * times its unit value. The unit VAT is zero unless the tax indicator's impact is
 * {@link VatImpact#STANDARD}.
 *
 * <p>The quantity is positive wherever a caller states it. Only a storno writes a negative one, and
 * {@link Document} does that for every line at once.
 */
public record Line(String sku, String senderCode, String receiverCode, String description, String unit,
                   BigDecimal quantity, BigDecimal unitPrice, BigDecimal unitDiscount, VatGroup vatGroup,
                   TaxIndicator taxIndicator, Boolean domesticProduct, BasePrice basePrice,
                   List<Attribute> attributes) {

    public Line {
        sku = Check.optional(sku, "UJP07-02", "sku", 30);
        senderCode = Check.optional(senderCode, "UJP07-03", "senderCode", 30);
        receiverCode = Check.optional(receiverCode, "UJP07-04", "receiverCode", 30);
        description = Check.required(description, "UJP07-06", "description", 300);
        unit = Check.optional(unit, "UJP07-07", "unit", 10);
        Check.present(quantity, "UJP07-08", "quantity");
        Check.that(quantity.signum() != 0, "UJP07-08", "quantity", "must not be zero");
        Check.decimals(quantity, Money.LINE_SCALE, "UJP07-08", "quantity");
        Check.present(unitPrice, "UJP07-09", "unitPrice");
        Check.that(unitPrice.signum() >= 0, "UJP07-09", "unitPrice", "must not be negative; a storno negates the quantity");
        Check.decimals(unitPrice, Money.LINE_SCALE, "UJP07-09", "unitPrice");
        unitDiscount = unitDiscount == null ? BigDecimal.ZERO : unitDiscount;
        Check.that(unitDiscount.signum() >= 0, "UJP07-10", "unitDiscount", "must not be negative");
        Check.that(unitDiscount.compareTo(unitPrice) <= 0, "UJP07-10", "unitDiscount",
                "must not exceed the unit price");
        Check.decimals(unitDiscount, Money.LINE_SCALE, "UJP07-10", "unitDiscount");
        Check.present(vatGroup, "UJP07-14", "vatGroup");
        Check.present(taxIndicator, "UJP07-19", "taxIndicator");
        Check.that(taxIndicator.group() == null || taxIndicator.group() == vatGroup, "UJP07-14", "vatGroup",
                "is " + vatGroup.code() + " but the tax indicator " + taxIndicator.code() + " belongs to "
                        + (taxIndicator.group() == null ? null : taxIndicator.group().code()));
        Check.that(taxIndicator.vatImpact() != VatImpact.MARGINALNA, "UJP07-19", "taxIndicator",
                taxIndicator.code() + " is a margin-scheme indicator; no obtained document says how the gateway "
                        + "computes it, so the model refuses it");
        Check.that(basePrice == null || basePrice.unitPrice().compareTo(unitPrice) == 0, "UJP07-22", "basePrice",
                "base + add - deduct must equal the unit price " + unitPrice.toPlainString());
        attributes = attributes == null ? List.of() : List.copyOf(attributes);
    }

    /** UJP07-11: the unit price after the unit discount. */
    public BigDecimal unitPriceAfterDiscount() {
        return Money.line(unitPrice.subtract(unitDiscount));
    }

    /** UJP07-12: the VAT on one unit; zero unless the indicator's impact is STANDARD. */
    public BigDecimal unitVat() {
        if (taxIndicator.vatImpact() != VatImpact.STANDARD) {
            return Money.line(BigDecimal.ZERO);
        }
        return Money.line(unitPriceAfterDiscount().multiply(vatGroup.percent()).movePointLeft(2));
    }

    /** UJP07-15: quantity times the unit price before discount. */
    public BigDecimal totalBeforeDiscount() {
        return Money.line(quantity.multiply(unitPrice));
    }

    /** The discount on the whole line (quantity times the unit discount), summed into UJP08-02. */
    public BigDecimal totalDiscount() {
        return quantity.multiply(unitDiscount);
    }

    /** UJP07-16: the tax base of the line, quantity times the unit price after discount. */
    public BigDecimal total() {
        return Money.line(quantity.multiply(unitPriceAfterDiscount()));
    }

    /** UJP07-17: quantity times the unit VAT. */
    public BigDecimal totalVat() {
        return Money.line(quantity.multiply(unitVat()));
    }

    /** UJP07-18: the tax base plus the VAT. */
    public BigDecimal totalWithVat() {
        return total().add(totalVat());
    }

    /**
     * The VAT the group's rate would put on this line, whatever the impact. Under PRENESEN the line
     * carries none, but the per-indicator totals show this figure.
     */
    BigDecimal notionalVat() {
        return Money.line(total().multiply(vatGroup.percent()).movePointLeft(2));
    }

    Line negated() {
        return new Line(sku, senderCode, receiverCode, description, unit, quantity.negate(), unitPrice,
                unitDiscount, vatGroup, taxIndicator, domesticProduct, basePrice, attributes);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Builds a {@link Line}; the description, quantity, unit price, group and indicator are required. */
    public static final class Builder {
        private String sku;
        private String senderCode;
        private String receiverCode;
        private String description;
        private String unit;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal unitDiscount;
        private VatGroup vatGroup;
        private TaxIndicator taxIndicator;
        private Boolean domesticProduct;
        private BasePrice basePrice;
        private List<Attribute> attributes;

        private Builder() {
        }

        public Builder sku(String value) {
            sku = value;
            return this;
        }

        public Builder senderCode(String value) {
            senderCode = value;
            return this;
        }

        public Builder receiverCode(String value) {
            receiverCode = value;
            return this;
        }

        public Builder description(String value) {
            description = value;
            return this;
        }

        public Builder unit(String value) {
            unit = value;
            return this;
        }

        public Builder quantity(String value) {
            quantity = new BigDecimal(value);
            return this;
        }

        public Builder quantity(BigDecimal value) {
            quantity = value;
            return this;
        }

        public Builder unitPrice(String value) {
            unitPrice = new BigDecimal(value);
            return this;
        }

        public Builder unitPrice(BigDecimal value) {
            unitPrice = value;
            return this;
        }

        public Builder unitDiscount(String value) {
            unitDiscount = new BigDecimal(value);
            return this;
        }

        public Builder unitDiscount(BigDecimal value) {
            unitDiscount = value;
            return this;
        }

        /** A plain rate group: its own indicator ({@code DDV-A}, {@code DDV-V}, {@code DDV-B}). */
        public Builder standardRate(VatGroup group) {
            vatGroup = group;
            taxIndicator = TaxIndicator.standard(group);
            return this;
        }

        /** A group with an explicit indicator, such as an exemption in group {@code DDV-G}. */
        public Builder tax(VatGroup group, TaxIndicator indicator) {
            vatGroup = group;
            taxIndicator = indicator;
            return this;
        }

        public Builder domesticProduct(boolean value) {
            domesticProduct = value;
            return this;
        }

        public Builder basePrice(BasePrice value) {
            basePrice = value;
            return this;
        }

        public Builder attributes(List<Attribute> value) {
            attributes = value;
            return this;
        }

        public Line build() {
            return new Line(sku, senderCode, receiverCode, description, unit, quantity, unitPrice, unitDiscount,
                    vatGroup, taxIndicator, domesticProduct, basePrice, attributes);
        }
    }
}
