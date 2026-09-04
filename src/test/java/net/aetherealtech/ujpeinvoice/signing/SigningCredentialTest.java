package net.aetherealtech.ujpeinvoice.signing;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SigningCredentialTest {

    @Test
    void carriesTheKeyAndChainItWasBuiltFrom() throws Exception {
        KeyStoreFixture fixture = KeyStoreFixture.load();

        SigningCredential credential = new SigningCredential(fixture.privateKey(), fixture.certificateChain());

        assertThat(credential.privateKey()).isEqualTo(fixture.privateKey());
        assertThat(credential.certificateChain()).isEqualTo(fixture.certificateChain());
    }

    @Test
    void rejectsANullPrivateKey() {
        assertThatThrownBy(() -> new SigningCredential(null, List.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void certificateChainIsDefensivelyCopied() throws Exception {
        KeyStoreFixture fixture = KeyStoreFixture.load();
        SigningCredential credential = new SigningCredential(fixture.privateKey(), fixture.certificateChain());

        assertThatThrownBy(() -> credential.certificateChain().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
