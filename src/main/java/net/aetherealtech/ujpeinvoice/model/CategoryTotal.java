package net.aetherealtech.ujpeinvoice.model;

import java.math.BigDecimal;

/** The net, VAT and gross subtotal for every line sharing one {@link VatCategory} on an invoice. */
public record CategoryTotal(VatCategory category, BigDecimal net, BigDecimal vat, BigDecimal gross) {

    public CategoryTotal {
        if (category == null) {
            throw new InvoiceValidationException("CategoryTotal.category must not be null");
        }
        if (net == null || vat == null || gross == null) {
            throw new InvoiceValidationException("CategoryTotal amounts must not be null");
        }
    }
}
