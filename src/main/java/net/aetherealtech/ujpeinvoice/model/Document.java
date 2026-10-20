package net.aetherealtech.ujpeinvoice.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A sales document as UJP describes it: a kind, a header, the mode (ordinary, storno or correction),
 * references, the two parties, payment, lines, an advance deducted and adjustments. Its totals and
 * its per-indicator VAT totals are derived from the lines on every call and never accepted from the
 * caller, so a document cannot disagree with its own arithmetic.
 *
 * <p>The constructor validates against the kind and refuses a missing mandatory field by name, with
 * the field's UJP code in the message, before anything is serialized or signed. The model follows
 * UJP's concepts but names no wire field and knows no JSON.
 *
 * <p>{@code lines} describes the lines as the caller sees them, with positive quantities, also for
 * a storno; {@link #effectiveLines()} is what the document carries, with every quantity negated on
 * a storno. Build a document with {@link #builder(DocumentKind)}.
 */
public record Document(DocumentKind kind, Header header, Mode mode, List<Reference> references, Party seller,
                       Party buyer, Payment payment, List<Line> lines, Advance advance, Adjustments adjustments,
                       List<Attribute> attributes) {

    public Document {
        Check.present(kind, "UJP01-01", "kind");
        Check.present(header, "UJP01", "header");
        mode = mode == null ? Mode.ORDINARY : mode;
        references = references == null ? List.of() : List.copyOf(references);
        lines = lines == null ? List.of() : List.copyOf(lines);
        adjustments = adjustments == null ? Adjustments.NONE : adjustments;
        attributes = attributes == null ? List.of() : List.copyOf(attributes);
        checkHeader(kind, header);
        checkReferences(mode, references);
        checkParty(kind, seller, "seller", "UJP03");
        Check.that(seller.tin() != null, "UJP03-03", "seller.tin",
                "is required: a seller is always an identified business");
        checkBuyer(kind, buyer);
        checkPayment(kind, payment);
        checkLines(kind, lines);
        Check.that(advance == null || kind.allowsAdvanceDeduction(), "UJP08-06", "advance",
                "is not on the page of a " + kind.code());
        Check.that(attributes.isEmpty() || kind.allowsAttributes(), "UJP12-01", "attributes",
                "are not on the page of a " + kind.code());
    }

    public static Builder builder(DocumentKind kind) {
        return new Builder(kind);
    }

    /** The lines as the document carries them: every quantity negated on a storno, as given otherwise. */
    public List<Line> effectiveLines() {
        return mode instanceof Mode.Storno ? lines.stream().map(Line::negated).toList() : lines;
    }

    /** UJP08: the totals, derived from {@link #effectiveLines()}. */
    public DocumentTotals totals() {
        return DocumentTotals.compute(effectiveLines(), advance, adjustments);
    }

    /** UJP09: one entry per tax indicator and rate group, in first-appearance order. */
    public List<VatTotal> vatTotals() {
        return VatTotal.compute(effectiveLines());
    }

    private static void checkHeader(DocumentKind kind, Header header) {
        Check.that(header.id().length() <= kind.docIdMaxLength(), "UJP01-11", "id",
                "is at most " + kind.docIdMaxLength() + " characters on a " + kind.code());
        Check.that(header.turnoverDate() != null || !kind.requiresTurnoverDate(), "UJP01-06", "turnoverDate",
                "is required on a " + kind.code());
        Check.that(header.indicator() == null || header.indicator().validOn(kind), "UJP01-15", "indicator",
                "is not valid on a " + kind.code() + ": " + header.indicator());
    }

    private static void checkReferences(Mode mode, List<Reference> references) {
        long originals = references.stream().filter(reference -> reference.type().equals(RefDocumentType.ST)).count();
        switch (mode) {
            case Mode.Ordinary _ -> Check.that(originals == 0, "UJP02-01-01", "references",
                    "may not contain type ST on an ordinary document: ST names the original of a storno or correction");
            case Mode.Storno _ -> Check.that(originals == 1, "UJP02-01-01", "references",
                    "needs exactly one reference of type ST naming the original, to be stornoed");
            case Mode.Correction _ -> Check.that(originals == 1, "UJP02-01-01", "references",
                    "needs exactly one reference of type ST naming the original, to be corrected");
        }
    }

    private static void checkBuyer(DocumentKind kind, Party buyer) {
        Check.present(buyer, "UJP05", "buyer");
        Check.that(!buyer.isNaturalPerson(), "UJP05-03", "buyer.tin", "is required: a natural-person buyer is "
                + "kind 170, which this library does not ship");
        Check.that(buyer.tin() != null || !buyer.country().equals(Country.MK), "UJP05-03", "buyer.tin",
                "is required for a buyer in " + Country.MK.code() + "; foreignTin is for a non-resident");
        checkParty(kind, buyer, "buyer", "UJP05");
    }

    private static void checkParty(DocumentKind kind, Party party, String side, String ujp) {
        Check.present(party, ujp, side);
        Check.that(party.name().length() <= kind.partyNameMaxLength(), ujp + "-06", side + ".name",
                "is at most " + kind.partyNameMaxLength() + " characters on a " + kind.code());
        Check.present(party.address(), ujp + "-07", side + ".address");
        Address address = party.address();
        Check.that(address.street().length() <= kind.streetMaxLength(), ujp + "-07-01", side + ".address.street",
                "is at most " + kind.streetMaxLength() + " characters on a " + kind.code());
        Check.that(address.number().length() <= kind.streetNumberMaxLength(), ujp + "-07-02",
                side + ".address.number", "is at most " + kind.streetNumberMaxLength() + " characters on a "
                        + kind.code());
        Subsidiary subsidiary = party.subsidiary();
        Check.that(subsidiary == null || subsidiary.name() == null
                        || subsidiary.name().length() <= kind.subsidiaryNameMaxLength(), ujp + "-09",
                side + ".subsidiary.name", "is at most " + kind.subsidiaryNameMaxLength() + " characters on a "
                        + kind.code());
    }

    private static void checkPayment(DocumentKind kind, Payment payment) {
        Check.that(payment == null || kind.allowsPayment(), "UJP10", "payment",
                "is not on the page of a " + kind.code());
        Check.that(!kind.requiresPaymentType() || (payment != null && payment.type() != null), "UJP10-01",
                "payment.type", "is required on a " + kind.code());
    }

    private static void checkLines(DocumentKind kind, List<Line> lines) {
        Check.that(!lines.isEmpty(), "UJP07-01", "lines", "needs at least one line");
        for (int index = 0; index < lines.size(); index++) {
            Line line = lines.get(index);
            String position = "lines[" + (index + 1) + "]";
            Check.that(line.quantity().signum() > 0, "UJP07-08", position + ".quantity",
                    "must be positive: describe the original's lines, a storno negates them itself");
            Check.that(line.unit() != null || !kind.requiresUnit(), "UJP07-07", position + ".unit",
                    "is required on a " + kind.code());
            Check.that(line.attributes().isEmpty() || kind.allowsAttributes(), "UJP07-26", position + ".attributes",
                    "are not on the page of a " + kind.code());
        }
    }

    /** Builds a {@link Document} of one kind; the header, the parties and at least one line are required. */
    public static final class Builder {
        private final DocumentKind kind;
        private Header header;
        private Mode mode = Mode.ORDINARY;
        private final List<Reference> references = new ArrayList<>();
        private Party seller;
        private Party buyer;
        private Payment payment;
        private final List<Line> lines = new ArrayList<>();
        private Advance advance;
        private Adjustments adjustments = Adjustments.NONE;
        private final List<Attribute> attributes = new ArrayList<>();

        private Builder(DocumentKind kind) {
            this.kind = kind;
        }

        public Builder header(Header value) {
            header = value;
            return this;
        }

        public Builder mode(Mode value) {
            mode = value;
            return this;
        }

        public Builder reference(Reference value) {
            references.add(value);
            return this;
        }

        public Builder seller(Party value) {
            seller = value;
            return this;
        }

        public Builder buyer(Party value) {
            buyer = value;
            return this;
        }

        public Builder payment(Payment value) {
            payment = value;
            return this;
        }

        public Builder line(Line value) {
            lines.add(value);
            return this;
        }

        public Builder advance(Advance value) {
            advance = value;
            return this;
        }

        public Builder adjustments(Adjustments value) {
            adjustments = value;
            return this;
        }

        public Builder attribute(Attribute value) {
            attributes.add(value);
            return this;
        }

        public Document build() {
            return new Document(kind, header, mode, references, seller, buyer, payment, lines, advance,
                    adjustments, attributes);
        }
    }
}
