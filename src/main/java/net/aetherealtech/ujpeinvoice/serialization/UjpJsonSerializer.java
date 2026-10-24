package net.aetherealtech.ujpeinvoice.serialization;

import net.aetherealtech.ujpeinvoice.ProvisionalSpec;
import net.aetherealtech.ujpeinvoice.internal.json.JsonArray;
import net.aetherealtech.ujpeinvoice.internal.json.JsonBoolean;
import net.aetherealtech.ujpeinvoice.internal.json.JsonNumber;
import net.aetherealtech.ujpeinvoice.internal.json.JsonObject;
import net.aetherealtech.ujpeinvoice.internal.json.JsonValue;
import net.aetherealtech.ujpeinvoice.internal.json.JsonWriter;
import net.aetherealtech.ujpeinvoice.model.Address;
import net.aetherealtech.ujpeinvoice.model.Advance;
import net.aetherealtech.ujpeinvoice.model.Attribute;
import net.aetherealtech.ujpeinvoice.model.Document;
import net.aetherealtech.ujpeinvoice.model.DocumentTotals;
import net.aetherealtech.ujpeinvoice.model.Header;
import net.aetherealtech.ujpeinvoice.model.Line;
import net.aetherealtech.ujpeinvoice.model.Mode;
import net.aetherealtech.ujpeinvoice.model.Party;
import net.aetherealtech.ujpeinvoice.model.Payment;
import net.aetherealtech.ujpeinvoice.model.Reference;
import net.aetherealtech.ujpeinvoice.model.Subsidiary;
import net.aetherealtech.ujpeinvoice.model.VatTotal;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Renders a {@link Document} as the JSON UJP's type pages and worked examples (primer_za_json of
 * 21.10.2026) describe: the envelope {@code {requestTimestamp, document}} with {@code voidReason}
 * or {@code correctionReason} beside {@code document}, and inside it {@code header},
 * {@code docReferences}, {@code seller}, {@code buyer}, {@code docPayment}, {@code docItems},
 * {@code docTotals}, {@code vatTotals} and {@code docAttributes}. Wire names are the ones in the
 * tables; the model beneath names none of them.
 *
 * <h2>What is written, and what is deliberately not</h2>
 * <ul>
 *   <li>The envelope is written for every kind. The 21.10 examples show the bare body on 110, 120, 130
 *       and the ordinary 100s, a {@code document} with its reason and no timestamp on a storno and a
 *       correction, and the full envelope on 310 alone; the signed payload nests under {@code document}
 *       and carries the timestamp (specification-status).</li>
 *   <li>An absent scalar or object is {@code null} and an empty list is {@code []}, as every example
 *       writes them. This stays {@link ProvisionalSpec}: it is the examples' habit, not a published
 *       rule, and the gateway's tolerance for omission is unstated.</li>
 *   <li>A group the kind's table does not list is left out whole: {@code docPayment} on 110 and 120,
 *       {@code docAttributes} and line {@code attributes} on all but 100. An optional member no
 *       example carries (subsidiary, base price) is left out when absent rather than written as
 *       {@code null}, and so is a line's {@code attributes} when it has none: the 21.10 examples carry
 *       the member on the one line that has attributes and on no other, while {@code docAttributes}
 *       is {@code []} on every 100. The reason object that does not apply is left out too.</li>
 *   <li>Nothing undocumented is written: not {@code source}, not a trailing {@code requestTimestamp},
 *       not {@code sellerEinvoiceUser} or {@code buyerEinvoiceUser}, not
 *       {@code sellerEntityType} or {@code buyerEntityType}, not {@code docGlobalDiscountAmount},
 *       {@code docItemUnitItemDiscountAmount} or {@code docItemHs}. They appear in some examples and
 *       in no table.</li>
 *   <li>{@code docCurrencyDate} and {@code docCurrencyExchRate} follow the kind: removed from 100 on
 *       15.10 (the 21.10 examples of 100 no longer print them), absent where there is no payment group,
 *       listed on 130 and 310. {@code docCurrencyCode} is written beside {@code docCurrency} on every
 *       kind, as the tables list it, though the 21.10 examples of 100 print {@code docCurrency}
 *       alone.</li>
 * </ul>
 *
 * <h2>Numbers</h2>
 * Every number is written as a JSON number without trailing zeros: {@code 100}, {@code 3417.1},
 * {@code 4.7619}, {@code -95.2381}. That is how every example prints its figures, including the
 * five amounts that became Decimal 15.2 on 19.10.2026: an MKD sum prints {@code 668}, not
 * {@code 668.00}, and the changelog's ",00" is a display convention the examples do not carry.
 *
 * <p>Member order within each object is fixed. The examples disagree among themselves (the
 * {@code docStorno} member comes first in some and last in others); this order follows the tables'
 * UJP codes, with the groups in the order of the envelope above.
 */
@ProvisionalSpec("The schema is unpublished (specification-status). Open: null for an absent scalar and [] for an "
        + "empty list rather than omission; the envelope, which the 21.10 examples write bare or without a "
        + "timestamp on every kind but 310; the single reason object, where the 310 example writes both as null; "
        + "leaving out every member no table documents; and which optional members are written when absent, "
        + "among them a line's empty attributes")
public final class UjpJsonSerializer implements Serializer {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    public byte[] serialize(Document document, LocalDateTime requestTimestamp) {
        return JsonWriter.write(envelope(document, requestTimestamp)).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String contentType() {
        return "application/json";
    }

    private JsonObject envelope(Document document, LocalDateTime requestTimestamp) {
        JsonObject.Builder envelope = JsonObject.builder()
                .put("requestTimestamp", TIMESTAMP.format(requestTimestamp))
                .put("document", body(document));
        switch (document.mode()) {
            case Mode.Ordinary _ -> {
            }
            case Mode.Storno storno -> envelope.put("voidReason", JsonObject.builder()
                    .put("voidCode", storno.reason().code())
                    .put("voidReason", storno.reason().name())
                    .put("voidComment", storno.comment())
                    .build());
            case Mode.Correction correction -> envelope.put("correctionReason", JsonObject.builder()
                    .put("correctionReasonCode", correction.reason().code())
                    .put("correctionReason", correction.reason().name())
                    .put("correctionReasonComment", correction.comment())
                    .build());
        }
        return envelope.build();
    }

    private JsonObject body(Document document) {
        JsonObject.Builder body = JsonObject.builder()
                .put("header", header(document))
                .put("docReferences", array(document.references(), this::reference))
                .put("seller", party("seller", document.seller()))
                .put("buyer", party("buyer", document.buyer()));
        if (document.kind().allowsPayment()) {
            body.put("docPayment", document.payment() == null ? null : payment(document, document.payment()));
        }
        body.put("docItems", items(document))
                .put("docTotals", totals(document.totals(), document.advance()))
                .put("vatTotals", array(document.vatTotals(), this::vatTotal));
        if (document.kind().allowsAttributes()) {
            body.put("docAttributes", array(document.attributes(), this::attribute));
        }
        return body.build();
    }

    @ProvisionalSpec("docAlt: Boolean in the changelog of 14.09, Integer(4) in the type pages of 08.10, "
            + "false in every 21.10 example of 100 and absent from the rest; written as a JSON boolean. docIndicator is written "
            + "for every kind though only 100 lists it, as the 110, 120, 130 and 310 examples do")
    private JsonObject header(Document document) {
        Header header = document.header();
        return JsonObject.builder()
                .put("docStorno", number(storno(document.mode())))
                .put("docType", document.kind().code())
                .put("docAlt", JsonBoolean.of(header.alternativeMode()))
                .put("docTypeName", document.kind().ujpName())
                .put("docDate", date(header.date()))
                .put("docDeliveryDate", date(header.deliveryDate()))
                .put("docDelivery", header.delivery())
                .put("docTurnoverDate", date(header.turnoverDate()))
                .put("docPeriodStartDate", date(header.periodStart()))
                .put("docPeriodEndDate", date(header.periodEnd()))
                .put("docNumber", header.number())
                .put("docId", header.id())
                .put("docNotes", header.notes())
                .put("docIndicator", header.indicator() == null ? null : header.indicator().name())
                .put("docHeader", header.headerText())
                .put("docFooter", header.footerText())
                .build();
    }

    private JsonObject reference(Reference reference) {
        return JsonObject.builder()
                .put("docTypeRef", reference.type().code())
                .put("docNameRef", reference.type().name())
                .put("docDateRef", date(reference.date()))
                .put("docNumberRef", reference.number())
                .build();
    }

    private JsonObject party(String side, Party party) {
        JsonObject.Builder builder = JsonObject.builder()
                .put(side + "CCode", party.country().code())
                .put(side + "CName", party.country().name())
                .put(side + "Tin", party.tin())
                .put(side + "ForeignTin", party.foreignTin())
                .put(side + "VatNumber", party.vatNumber())
                .put(side + "Name", party.name())
                .put(side + "Address", address(party.address()));
        Subsidiary subsidiary = party.subsidiary();
        if (subsidiary != null) {
            builder.put(side + "SubsidiaryCode", subsidiary.code())
                    .put(side + "SubsidiaryName", subsidiary.name())
                    .put(side + "SubsidiaryAddress", subsidiary.address());
        }
        return builder.put(side + "Contact", party.contact())
                .put(side + "Email", party.email())
                .build();
    }

    private JsonObject address(Address address) {
        return JsonObject.builder()
                .put("streetAddress", address.street())
                .put("streetNumber", address.number())
                .put("postalCode", address.postalCode())
                .put("city", address.city())
                .build();
    }

    private JsonObject payment(Document document, Payment payment) {
        JsonObject.Builder builder = JsonObject.builder()
                .put("docPaymentTypeCode", payment.type() == null ? null : payment.type().code())
                .put("docPaymentTypeDesc", payment.type() == null ? null : payment.type().description())
                .put("docPaymentTypeDueDays", payment.dueDays() == null ? null : number(payment.dueDays()))
                .put("docPaymentTypeDueDate", date(payment.dueDate()))
                .put("docPaymentTerms", payment.terms())
                .put("docPaymentNote", payment.note())
                .put("docPaymentInterest", payment.interest())
                .put("docPaymentDiscount", payment.discount())
                .put("docCurrency", payment.currency().getCurrencyCode())
                .put("docCurrencyCode", payment.currency().getCurrencyCode());
        if (document.kind().listsCurrencyRate()) {
            appendCurrencyRate(builder, document, payment);
        }
        return builder.build();
    }

    /**
     * Every 130 and 310 example writes the document's own date and a rate of 1 beside MKD. The model
     * holds no exchange rate, so a payment in another currency writes {@code null} for both, which
     * the tables mark optional (UJP10-10 is О on 310 and ОП on 130, UJP10-11 is О on both).
     */
    @ProvisionalSpec("The date and rate beside MKD are what the 130 and 310 examples print; no table says how "
            + "they are filled, and the model has no exchange rate for another currency")
    private void appendCurrencyRate(JsonObject.Builder builder, Document document, Payment payment) {
        boolean domestic = payment.currency().getCurrencyCode().equals("MKD");
        builder.put("docCurrencyDate", domestic ? date(document.header().date()) : null)
                .put("docCurrencyExchRate", domestic ? number(1) : null);
    }

    private JsonArray items(Document document) {
        List<Line> lines = document.effectiveLines();
        return JsonArray.of(IntStream.range(0, lines.size())
                .mapToObj(index -> item(document, index + 1, lines.get(index)))
                .toList());
    }

    @ProvisionalSpec("docItemTotalOriginalPriceWoVat is in every example and in the 310 table (UJP07-15), but "
            + "not in the 100, 110, 120 or 130 tables")
    private JsonObject item(Document document, int lineNumber, Line line) {
        JsonObject.Builder item = JsonObject.builder()
                .put("docItemLineNo", number(lineNumber))
                .put("docItemSku", line.sku())
                .put("docItemSenderCode", line.senderCode())
                .put("docItemReceiverCode", line.receiverCode())
                .put("docItemDesc", line.description())
                .put("docItemMUnit", line.unit())
                .put("docItemQty", number(line.quantity()))
                .put("docItemUnitOriginalPriceWoVat", number(line.unitPrice()))
                .put("docItemUnitDiscountAmount", number(line.unitDiscount()))
                .put("docItemUnitPriceWoVat", number(line.unitPriceAfterDiscount()))
                .put("docItemUnitVat", number(line.unitVat()))
                .put("docItemVat", number(line.vatGroup().percent()))
                .put("docItemVatGroup", line.vatGroup().code())
                .put("docItemTotalOriginalPriceWoVat", number(line.totalBeforeDiscount()))
                .put("docItemTotalPriceWoVat", number(line.total()))
                .put("docItemTotalVat", number(line.totalVat()))
                .put("docItemTotalPriceWVat", number(line.totalWithVat()))
                .put("docItemTaxIndicator", line.taxIndicator().code())
                .put("docItemDomesticProduct", Boolean.TRUE.equals(line.domesticProduct()) ? "MP" : null);
        if (line.basePrice() != null) {
            item.put("docItemBasePriceWoVat", number(line.basePrice().base()))
                    .put("docItemBasePriceWoVatAdd", number(line.basePrice().add()))
                    .put("docItemBasePriceWoVatDeduct", number(line.basePrice().deduct()))
                    .put("docItemBasePriceType", line.basePrice().type());
        }
        if (document.kind().allowsAttributes() && !line.attributes().isEmpty()) {
            item.put("attributes", array(line.attributes(), this::attribute));
        }
        return item.build();
    }

    @ProvisionalSpec("The advance block is in every example and the increase and decrease in every 100 example, but "
            + "only some tables list them (UJP08-06 to UJP08-09 are on the 100 table alone); written for every kind")
    private JsonObject totals(DocumentTotals totals, Advance advance) {
        return JsonObject.builder()
                .put("docNetAmount", number(totals.net()))
                .put("docDiscountAmount", number(totals.discount()))
                .put("docNetAmountDisc", number(totals.netAfterDiscount()))
                .put("docVatAmount", number(totals.vat()))
                .put("docGrossAmount", number(totals.gross()))
                .put("docGrossAmountR", number(totals.grossRounded()))
                .put("docAvansDate", advance == null ? null : date(advance.date()))
                .put("docAvansDesc", advance == null ? null : advance.description())
                .put("docAvansAmount", number(totals.advanceAmount()))
                .put("docAmountIncrease", number(totals.increase()))
                .put("docAmountDecrease", number(totals.decrease()))
                .put("docFinalAmount", number(totals.finalAmount()))
                .build();
    }

    @ProvisionalSpec("An absent note is null, as the 110, 130 and 310 examples write it; the 100 examples write "
            + "an empty string")
    private JsonObject vatTotal(VatTotal total) {
        return JsonObject.builder()
                .put("vatTaxIndicator", total.indicator().code())
                .put("vatTaxIndicatorNote", total.indicator().note())
                .put("vatCode", total.group().code())
                .put("vatPercent", number(total.group().percent()))
                .put("vatTaxableAmount", number(total.taxable()))
                .put("vatAmount", number(total.vat()))
                .put("vatTotalAmount", number(total.total()))
                .build();
    }

    private JsonObject attribute(Attribute attribute) {
        return JsonObject.builder()
                .put("name", attribute.name())
                .put("value", attribute.value())
                .put("type", attribute.type().name())
                .build();
    }

    private static int storno(Mode mode) {
        return switch (mode) {
            case Mode.Ordinary _ -> 0;
            case Mode.Storno _ -> 1;
            case Mode.Correction _ -> 2;
        };
    }

    private static <T> JsonArray array(List<T> items, Function<T, JsonObject> element) {
        return JsonArray.of(items.stream().map(element).toList());
    }

    private static String date(LocalDate date) {
        return date == null ? null : date.toString();
    }

    private static JsonValue number(int value) {
        return new JsonNumber(BigDecimal.valueOf(value));
    }

    /** A number without trailing zeros, so 100.0000 is written 100 and 4.7619 stays 4.7619. */
    private static JsonValue number(BigDecimal value) {
        return new JsonNumber(value.stripTrailingZeros());
    }
}
