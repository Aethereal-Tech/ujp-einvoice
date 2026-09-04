package net.aetherealtech.ujpeinvoice.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentReferenceTest {

    @Test
    void carriesTheNumberAndIssueDateOfTheCorrectedDocument() {
        DocumentReference reference = new DocumentReference("INV-2026-0001", LocalDate.of(2026, 8, 28));

        assertThat(reference.number()).isEqualTo("INV-2026-0001");
        assertThat(reference.issueDate()).isEqualTo(LocalDate.of(2026, 8, 28));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void rejectsABlankNumber(String number) {
        assertThatThrownBy(() -> new DocumentReference(number, LocalDate.of(2026, 8, 28)))
                .isInstanceOf(InvoiceValidationException.class)
                .hasMessageContaining("DocumentReference.number");
    }

    @Test
    void rejectsAMissingIssueDate() {
        assertThatThrownBy(() -> new DocumentReference("INV-2026-0001", null))
                .isInstanceOf(InvoiceValidationException.class)
                .hasMessageContaining("DocumentReference.issueDate");
    }
}
