package net.aetherealtech.ujpeinvoice.signing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SigningExceptionTest {

    @Test
    void carriesMessageAndCause() {
        Exception cause = new Exception("root cause");
        SigningException exception = new SigningException("could not sign", cause);

        assertThat(exception.getMessage()).isEqualTo("could not sign");
        assertThat(exception.getCause()).isSameAs(cause);
    }
}
