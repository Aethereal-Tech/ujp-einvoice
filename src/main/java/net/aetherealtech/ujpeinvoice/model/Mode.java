package net.aetherealtech.ujpeinvoice.model;

import net.aetherealtech.ujpeinvoice.ProvisionalSpec;

/**
 * Whether a document is an ordinary one, a storno of another, or a correction of another: the
 * {@code docStorno} axis (UJP01-14: 0, 1, 2), independent of the document's kind. A storno and a
 * correction both name the original through a reference of type {@code ST}, which {@link Document}
 * requires.
 *
 * <p>A storno is the one place a quantity is negative, and the model writes the sign: the caller
 * describes the original's lines, so a caller cannot negate one line and forget another.
 */
@ProvisionalSpec("docStorno has no row on the 110, 120 and 130 pages (their examples send 0); nothing says a storno "
        + "or a correction is valid on them, and the model does not refuse one")
public sealed interface Mode {

    Ordinary ORDINARY = new Ordinary();

    /** A regular document (docStorno 0). */
    record Ordinary() implements Mode {
    }

    /** A new document with negative amounts that voids the one it references (docStorno 1). */
    record Storno(VoidReason reason, String comment) implements Mode {

        public Storno {
            Check.present(reason, "UJP01-14", "storno.reason");
            comment = Check.optional(comment, "UJP01-14", "storno.comment", 300);
            Check.that(!reason.needsComment() || comment != null, "UJP01-14", "storno.comment",
                    "is required for reason " + reason.code() + ": the code list asks for free text");
        }
    }

    /** A replacement document that corrects the one it references (docStorno 2). */
    record Correction(CorrectionReason reason, String comment) implements Mode {

        public Correction {
            Check.present(reason, "UJP01-14", "correction.reason");
            comment = Check.optional(comment, "UJP01-14", "correction.comment", 300);
            Check.that(!reason.needsComment() || comment != null, "UJP01-14", "correction.comment",
                    "is required for reason " + reason.code() + ": the code list asks for free text");
        }
    }
}
