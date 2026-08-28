package net.aetherealtech.ujpeinvoice.transport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class SubmissionStatusTest {

    @ParameterizedTest
    @CsvSource({
            "PENDING, PENDING",
            "IN_PROGRESS, PENDING",
            "processing, PENDING",
            "ACCEPTED, ACCEPTED",
            "success, ACCEPTED",
            "SENT, ACCEPTED",
            "REJECTED, REJECTED",
            "failed, REJECTED",
            "ERROR, REJECTED",
            "SOMETHING_ELSE, UNKNOWN",
    })
    void mapsWireValuesToKnownStates(String wireValue, SubmissionStatus expected) {
        assertThat(SubmissionStatus.fromWireValue(wireValue)).isEqualTo(expected);
    }

    @Test
    void nullMapsToUnknownRatherThanThrowing() {
        assertThat(SubmissionStatus.fromWireValue(null)).isEqualTo(SubmissionStatus.UNKNOWN);
    }
}
