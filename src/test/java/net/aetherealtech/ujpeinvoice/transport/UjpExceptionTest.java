package net.aetherealtech.ujpeinvoice.transport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UjpExceptionTest {

    @Test
    void aKnownCodeInItsNamespaceResolvesToTheCatalogueEntry() {
        UjpException e = new UjpException(400, ErrorNamespace.JSON_RECEIVER, "E1020", "{\"raw\":true}", "msg", null);

        assertThat(e.known()).contains(
                new UjpErrorCode(ErrorNamespace.JSON_RECEIVER, "E1020", "Invalid calculated totals"));
        assertThat(e.namespace()).isEqualTo(ErrorNamespace.JSON_RECEIVER);
        assertThat(e.errorCode()).isEqualTo("E1020");
        assertThat(e.httpStatus()).isEqualTo(400);
        assertThat(e.responseBody()).isEqualTo("{\"raw\":true}");
        assertThat(e.getMessage()).isEqualTo("msg");
    }

    @Test
    void theSameCodeResolvesToADifferentMeaningInAnotherNamespace() {
        UjpException e = new UjpException(400, ErrorNamespace.EINVOICE_API, "E1020", null, "msg", null);

        assertThat(e.known()).map(UjpErrorCode::meaning)
                .contains("This certificate for this company already exists!");
    }

    @Test
    void anUnlistedCodeIsNotMappedToAGuess() {
        UjpException e = new UjpException(400, ErrorNamespace.JSON_RECEIVER, "E9999", "body", "msg", null);

        assertThat(e.known()).isEmpty();
        assertThat(e.errorCode()).isEqualTo("E9999");
        assertThat(e.responseBody()).isEqualTo("body");
    }

    @Test
    void aCodeWithoutANamespaceIsNotResolved() {
        UjpException e = new UjpException(400, "E1012", "msg");

        assertThat(e.errorCode()).isEqualTo("E1012");
        assertThat(e.namespace()).isNull();
        assertThat(e.responseBody()).isNull();
        assertThat(e.known()).isEmpty();
    }

    @Test
    void aFailureBeforeAnyResponseCarriesNoCode() {
        UjpException e = new UjpTransportException("down", new RuntimeException("cause"));

        assertThat(e.httpStatus()).isEqualTo(-1);
        assertThat(e.errorCode()).isNull();
        assertThat(e.namespace()).isNull();
        assertThat(e.known()).isEmpty();
        assertThat(e.getCause()).hasMessage("cause");
    }

    @Test
    void theCauseIsKeptOnTheFourArgumentConstructor() {
        RuntimeException cause = new RuntimeException("cause");

        assertThat(new UjpException(500, "E5001", "msg", cause).getCause()).isSameAs(cause);
    }
}
