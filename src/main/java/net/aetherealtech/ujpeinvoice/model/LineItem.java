package net.aetherealtech.ujpeinvoice.model;

import net.aetherealtech.ujpeinvoice.internal.Money;

import java.math.BigDecimal;

/**
 * One invoice line: a described quantity of something at a unit price, taxed at one VAT category,
 * optionally stating what the quantity is counted in.
 *
 * <p>{@code unit} is free text as the seller writes it — „ком.", „м²", „час", {@code "kg"} — not a
 * code from a list. There is no evidence UJP expects a UN/ECE Rec 20 code, and inventing a code
 * list here would refuse units a seller legitimately uses. Absent means absent: the field is
 * omitted from the wire rather than written as an empty string.
 *
 * <p>{@link #netAmount()}, {@link #vatAmount()} and {@link #grossAmount()} are derived, not stored —
 * a line's money always follows from quantity, price and category, so there is no field for it to
 * disagree with. Each is rounded to two decimals, half-up, independently: net first, then VAT
 * computed from the rounded net. That order matches how the amounts are meant to appear on a
 * printed invoice line (each figure correct on its own), at the cost of the tiny second-order drift
 * against a single unrounded computation that {@link Totals#compute} then has to reconcile against.
 */
public record LineItem(String description, BigDecimal quantity, BigDecimal unitPrice, VatCategory vatCategory,
                        String unit) {

    public LineItem {
        if (description == null || description.isBlank()) {
            throw new InvoiceValidationException("LineItem.description must not be blank");
        }
        if (quantity == null || quantity.signum() <= 0) {
            throw new InvoiceValidationException("LineItem.quantity must be positive");
        }
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new InvoiceValidationException("LineItem.unitPrice must not be negative");
        }
        if (vatCategory == null) {
            throw new InvoiceValidationException("LineItem.vatCategory must not be null");
        }
        if (unit != null && unit.isBlank()) {
            throw new InvoiceValidationException("LineItem.unit must not be blank; omit it instead");
        }
    }

    /** A line that does not state its unit of measure. */
    public LineItem(String description, BigDecimal quantity, BigDecimal unitPrice, VatCategory vatCategory) {
        this(description, quantity, unitPrice, vatCategory, null);
    }

    /** Quantity times unit price, rounded to two decimals, half-up. */
    public BigDecimal netAmount() {
        return Money.round(quantity.multiply(unitPrice));
    }

    /** {@link #netAmount()} times the category rate, rounded to two decimals, half-up. */
    public BigDecimal vatAmount() {
        return Money.round(netAmount().multiply(vatCategory.rate()));
    }

    /** Net plus VAT. Both operands are already rounded, so this sum needs no further rounding. */
    public BigDecimal grossAmount() {
        return netAmount().add(vatAmount());
    }
}
