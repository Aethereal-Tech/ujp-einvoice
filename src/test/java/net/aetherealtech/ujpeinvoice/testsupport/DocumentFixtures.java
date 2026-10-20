package net.aetherealtech.ujpeinvoice.testsupport;

import net.aetherealtech.ujpeinvoice.model.Address;
import net.aetherealtech.ujpeinvoice.model.Document;
import net.aetherealtech.ujpeinvoice.model.DocumentKind;
import net.aetherealtech.ujpeinvoice.model.Header;
import net.aetherealtech.ujpeinvoice.model.Line;
import net.aetherealtech.ujpeinvoice.model.Party;
import net.aetherealtech.ujpeinvoice.model.Payment;
import net.aetherealtech.ujpeinvoice.model.PaymentType;
import net.aetherealtech.ujpeinvoice.model.VatGroup;

import java.time.LocalDate;

/** Sample documents of invented parties, shared by the model, serializer and client tests. */
public final class DocumentFixtures {

    public static final LocalDate DATE = LocalDate.of(2026, 8, 28);

    private DocumentFixtures() {
    }

    public static Party seller() {
        return Party.company("Aethereal Tech DOOEL", "4030012345678",
                new Address("Bul. Partizanski Odredi", "1", "1000", "Skopje"));
    }

    public static Party buyer() {
        return Party.company("Buyer Company DOO", "4057098765432",
                new Address("Ul. Makedonija", "10", "7000", "Bitola"));
    }

    public static Header header() {
        return Header.of("INV-2026-0001", "1", DATE).withTurnoverDate(DATE);
    }

    public static Line consultingHour(String quantity) {
        return Line.builder()
                .description("Consulting services")
                .unit("hour")
                .quantity(quantity)
                .unitPrice("100")
                .standardRate(VatGroup.DDV_A)
                .build();
    }

    /** The builder of an ordinary invoice with a seller, a buyer and a header, ready for lines. */
    public static Document.Builder invoice() {
        return Document.builder(DocumentKind.INVOICE)
                .header(header())
                .seller(seller())
                .buyer(buyer());
    }

    /** An ordinary invoice of two consulting hours at 100 plus 18%. */
    public static Document simpleInvoice() {
        return invoice()
                .payment(Payment.of(PaymentType.P_12))
                .line(consultingHour("2"))
                .build();
    }
}
