package net.aetherealtech.ujpeinvoice.serialization;

import net.aetherealtech.ujpeinvoice.ProvisionalSpec;
import net.aetherealtech.ujpeinvoice.internal.json.JsonArray;
import net.aetherealtech.ujpeinvoice.internal.json.JsonObject;
import net.aetherealtech.ujpeinvoice.internal.json.JsonValue;
import net.aetherealtech.ujpeinvoice.internal.json.JsonWriter;
import net.aetherealtech.ujpeinvoice.model.Address;
import net.aetherealtech.ujpeinvoice.model.CategoryTotal;
import net.aetherealtech.ujpeinvoice.model.Invoice;
import net.aetherealtech.ujpeinvoice.model.LineItem;
import net.aetherealtech.ujpeinvoice.model.Party;
import net.aetherealtech.ujpeinvoice.model.Totals;
import net.aetherealtech.ujpeinvoice.model.VatCategory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders an {@link Invoice} as the proprietary UJP e-Faktura JSON shape — the one that gets signed
 * as a compact JWS (RS256) and posted to {@code /JSONReceiver/sales-invoices/send}, per hands-on
 * accounts from Macedonian integrators. This is <strong>not</strong> the UBL 2.1 / XAdES shape that
 * generic e-invoicing compliance sites describe for North Macedonia; that appears to be a mismatch
 * between generic vendor marketing and what the actual UJP endpoint accepts.
 *
 * <h2>Specification status</h2>
 * <p>The authoritative source — efakturawiki.ujp.gov.mk's "API Спецификација" — is unreachable
 * outside North Macedonian networks and has not been read by anyone who built this library. Every
 * field name below is this library's best-effort reconstruction from third-party integrator reports
 * (submission responses carrying an EUID and a QR link; error codes such as E1012 and E5004) and is
 * marked {@link ProvisionalSpec}. Treat the exact field names, nesting, and code values as a
 * starting point to validate against a real sandbox account (efakturatest.ujp.gov.mk), not as a
 * confirmed contract. See the project README's "Specification status" section for the complete
 * verified-vs-reconstructed inventory, including what is NOT reconstructed here (the VAT rates
 * themselves and the ЕДБ tax-id format are public North Macedonian tax law, independent of the UJP
 * wire format).
 *
 * <p>If the real schema turns out to differ, only this class (and its golden-file tests) needs to
 * change — {@link Invoice} and everything in {@code .signing} and {@code .transport} depend on
 * {@link Serializer}, not on this shape.
 */
public final class UjpJsonSerializer implements Serializer {

    @ProvisionalSpec("Top-level field name for the invoice document number.")
    private static final String FIELD_INVOICE_NUMBER = "invoiceNumber";

    @ProvisionalSpec("Top-level field name for the issue date; ISO-8601 date format assumed.")
    private static final String FIELD_ISSUE_DATE = "issueDate";

    @ProvisionalSpec("Top-level field name for the due date; ISO-8601 date format assumed.")
    private static final String FIELD_DUE_DATE = "dueDate";

    @ProvisionalSpec("Top-level field name for the ISO 4217 currency code.")
    private static final String FIELD_CURRENCY = "currency";

    @ProvisionalSpec("Top-level field name for the seller party.")
    private static final String FIELD_SELLER = "seller";

    @ProvisionalSpec("Top-level field name for the buyer party.")
    private static final String FIELD_BUYER = "buyer";

    @ProvisionalSpec("Top-level field name for the line item array.")
    private static final String FIELD_LINE_ITEMS = "lineItems";

    @ProvisionalSpec("Top-level field name for the totals summary.")
    private static final String FIELD_TOTALS = "totals";

    @ProvisionalSpec("Party field name for the legal/trade name.")
    private static final String FIELD_PARTY_NAME = "name";

    @ProvisionalSpec("Party field name for the ЕДБ (unique tax number). The identifier format "
            + "itself is public tax law; only this field NAME is reconstructed.")
    private static final String FIELD_PARTY_TAX_ID = "taxId";

    @ProvisionalSpec("Party field name for the postal address.")
    private static final String FIELD_PARTY_ADDRESS = "address";

    @ProvisionalSpec("Address field name for the street line.")
    private static final String FIELD_ADDRESS_STREET = "street";

    @ProvisionalSpec("Address field name for the city.")
    private static final String FIELD_ADDRESS_CITY = "city";

    @ProvisionalSpec("Address field name for the postal code.")
    private static final String FIELD_ADDRESS_POSTAL_CODE = "postalCode";

    @ProvisionalSpec("Address field name for the ISO 3166-1 alpha-2 country code.")
    private static final String FIELD_ADDRESS_COUNTRY = "country";

    @ProvisionalSpec("Line item field name for the free-text description.")
    private static final String FIELD_LINE_DESCRIPTION = "description";

    @ProvisionalSpec("Line item field name for the quantity.")
    private static final String FIELD_LINE_QUANTITY = "quantity";

    @ProvisionalSpec("Line item field name for the per-unit price, before VAT.")
    private static final String FIELD_LINE_UNIT_PRICE = "unitPrice";

    @ProvisionalSpec("Line item field name for the VAT category code; see VatCategory.code().")
    private static final String FIELD_LINE_VAT_CATEGORY = "vatCategory";

    @ProvisionalSpec("Line item field name for the computed net amount.")
    private static final String FIELD_LINE_NET_AMOUNT = "netAmount";

    @ProvisionalSpec("Line item field name for the computed VAT amount.")
    private static final String FIELD_LINE_VAT_AMOUNT = "vatAmount";

    @ProvisionalSpec("Line item field name for the computed gross amount.")
    private static final String FIELD_LINE_GROSS_AMOUNT = "grossAmount";

    @ProvisionalSpec("Totals field name for the per-category subtotal array.")
    private static final String FIELD_TOTALS_BY_CATEGORY = "byCategory";

    @ProvisionalSpec("Totals field name for the invoice-wide net total.")
    private static final String FIELD_TOTALS_NET = "netTotal";

    @ProvisionalSpec("Totals field name for the invoice-wide VAT total.")
    private static final String FIELD_TOTALS_VAT = "vatTotal";

    @ProvisionalSpec("Totals field name for the invoice-wide gross total.")
    private static final String FIELD_TOTALS_GROSS = "grossTotal";

    @ProvisionalSpec("Category subtotal field name for which VAT category the entry covers.")
    private static final String FIELD_CATEGORY = "category";

    @ProvisionalSpec("Category subtotal field name for its net amount.")
    private static final String FIELD_CATEGORY_NET = "net";

    @ProvisionalSpec("Category subtotal field name for its VAT amount.")
    private static final String FIELD_CATEGORY_VAT = "vat";

    @ProvisionalSpec("Category subtotal field name for its gross amount.")
    private static final String FIELD_CATEGORY_GROSS = "gross";

    @Override
    public byte[] serialize(Invoice invoice) {
        return JsonWriter.write(toJson(invoice)).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String contentType() {
        return "application/json";
    }

    private JsonObject toJson(Invoice invoice) {
        JsonObject.Builder builder = JsonObject.builder()
                .put(FIELD_INVOICE_NUMBER, invoice.invoiceNumber())
                .put(FIELD_ISSUE_DATE, invoice.issueDate().toString());
        if (invoice.dueDate() != null) {
            builder.put(FIELD_DUE_DATE, invoice.dueDate().toString());
        }
        return builder
                .put(FIELD_CURRENCY, invoice.currency().getCurrencyCode())
                .put(FIELD_SELLER, toJson(invoice.seller()))
                .put(FIELD_BUYER, toJson(invoice.buyer()))
                .put(FIELD_LINE_ITEMS, lineItemsToJson(invoice.lineItems()))
                .put(FIELD_TOTALS, toJson(invoice.totals()))
                .build();
    }

    private JsonObject toJson(Party party) {
        return JsonObject.builder()
                .put(FIELD_PARTY_NAME, party.name())
                .put(FIELD_PARTY_TAX_ID, party.taxId())
                .put(FIELD_PARTY_ADDRESS, toJson(party.address()))
                .build();
    }

    private JsonObject toJson(Address address) {
        return JsonObject.builder()
                .put(FIELD_ADDRESS_STREET, address.street())
                .put(FIELD_ADDRESS_CITY, address.city())
                .put(FIELD_ADDRESS_POSTAL_CODE, address.postalCode())
                .put(FIELD_ADDRESS_COUNTRY, address.country())
                .build();
    }

    private JsonArray lineItemsToJson(List<LineItem> lineItems) {
        List<JsonValue> items = new ArrayList<>(lineItems.size());
        for (LineItem lineItem : lineItems) {
            items.add(toJson(lineItem));
        }
        return JsonArray.of(items);
    }

    private JsonObject toJson(LineItem lineItem) {
        return JsonObject.builder()
                .put(FIELD_LINE_DESCRIPTION, lineItem.description())
                .put(FIELD_LINE_QUANTITY, lineItem.quantity())
                .put(FIELD_LINE_UNIT_PRICE, lineItem.unitPrice())
                .put(FIELD_LINE_VAT_CATEGORY, lineItem.vatCategory().code())
                .put(FIELD_LINE_NET_AMOUNT, lineItem.netAmount())
                .put(FIELD_LINE_VAT_AMOUNT, lineItem.vatAmount())
                .put(FIELD_LINE_GROSS_AMOUNT, lineItem.grossAmount())
                .build();
    }

    private JsonObject toJson(Totals totals) {
        List<JsonValue> byCategory = new ArrayList<>(totals.byCategory().size());
        // EnumMap iteration follows VatCategory's declaration order, so this is deterministic
        // across runs and JVMs without an explicit sort — load-bearing for the golden-file tests.
        for (CategoryTotal categoryTotal : totals.byCategory().values()) {
            byCategory.add(toJson(categoryTotal));
        }
        return JsonObject.builder()
                .put(FIELD_TOTALS_BY_CATEGORY, JsonArray.of(byCategory))
                .put(FIELD_TOTALS_NET, totals.netTotal())
                .put(FIELD_TOTALS_VAT, totals.vatTotal())
                .put(FIELD_TOTALS_GROSS, totals.grossTotal())
                .build();
    }

    private JsonObject toJson(CategoryTotal categoryTotal) {
        return JsonObject.builder()
                .put(FIELD_CATEGORY, categoryTotal.category().code())
                .put(FIELD_CATEGORY_NET, categoryTotal.net())
                .put(FIELD_CATEGORY_VAT, categoryTotal.vat())
                .put(FIELD_CATEGORY_GROSS, categoryTotal.gross())
                .build();
    }
}
