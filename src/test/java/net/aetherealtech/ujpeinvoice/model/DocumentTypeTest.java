package net.aetherealtech.ujpeinvoice.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentTypeTest {

    @Test
    void onlyTheTwoNoteTypesCorrectAnEarlierDocument() {
        assertThat(DocumentType.INVOICE.corrects()).isFalse();
        assertThat(DocumentType.CREDIT_NOTE.corrects()).isTrue();
        assertThat(DocumentType.DEBIT_NOTE.corrects()).isTrue();
    }

    @Test
    void hasExactlyTheThreeTypesMacedonianVatPracticeKnows() {
        assertThat(DocumentType.values())
                .containsExactly(DocumentType.INVOICE, DocumentType.CREDIT_NOTE, DocumentType.DEBIT_NOTE);
    }
}
