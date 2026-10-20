package net.aetherealtech.ujpeinvoice.transport;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ErrorStatusTest {

    @Test
    void equalComponentsAreEqual() {
        ErrorStatus a = new ErrorStatus("E4015", "Invalid signature", Map.of("jws", List.of("bad")));
        ErrorStatus b = new ErrorStatus("E4015", "Invalid signature", Map.of("jws", List.of("bad")));

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(new ErrorStatus("E4016", "Invalid signature", Map.of("jws", List.of("bad"))));
    }

    @Test
    void theMapIsCopiedAndImmutable() {
        List<String> messages = new ArrayList<>(List.of("a"));
        Map<String, List<String>> source = new LinkedHashMap<>();
        source.put("p", messages);

        ErrorStatus status = new ErrorStatus("E1", "m", source);
        messages.add("b");
        source.put("q", List.of());

        assertThat(status.errorPropertyMap()).containsOnlyKeys("p");
        assertThat(status.errorPropertyMap().get("p")).containsExactly("a");
        assertThatThrownBy(() -> status.errorPropertyMap().put("x", List.of()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void anAbsentMapIsEmptyNotNull() {
        assertThat(new ErrorStatus("E1", "m", null).errorPropertyMap()).isEmpty();
    }

    @Test
    void anUnlistedCodeSurvivesAsAString() {
        assertThat(new ErrorStatus("E9999", null, null).errorCode()).isEqualTo("E9999");
    }

    @Test
    void parsesTheErrorStatusOfAGenericApiResponse() {
        String body = """
                {"success":false,"createdDate":"2026-10-20T07:00:00","errorStatus":{
                  "errorCode":"E4013","errorMessage":"JSON schema validation failed",
                  "errorPropertyMap":{"docDate":["must not be null","bad format",null],"x":null}}}""";

        assertThat(ErrorStatus.fromResponseBody(body)).contains(new ErrorStatus("E4013",
                "JSON schema validation failed",
                Map.of("docDate", List.of("must not be null", "bad format"), "x", List.of())));
    }

    @Test
    void aSparseErrorStatusParsesWithNulls() {
        assertThat(ErrorStatus.fromResponseBody("{\"errorStatus\":{\"errorCode\":\"E5000\"}}"))
                .contains(new ErrorStatus("E5000", null, Map.of()));
        assertThat(ErrorStatus.fromResponseBody("{\"errorStatus\":{\"errorCode\":null,\"errorPropertyMap\":[]}}"))
                .contains(new ErrorStatus(null, null, Map.of()));
    }

    @Test
    void parsesABareErrorStatusBeanToo() {
        assertThat(ErrorStatus.fromResponseBody(
                "{\"errorCode\":\"E5000\",\"errorMessage\":\"Internal server error\","
                        + "\"errorPropertyMap\":{\"euid\":[\"must not be blank\"]}}"))
                .contains(new ErrorStatus("E5000", "Internal server error", Map.of("euid", List.of("must not be blank"))));
    }

    @Test
    void whatIsNotAnErrorStatusIsEmptyRatherThanAnError() {
        assertThat(ErrorStatus.fromResponseBody(null)).isEmpty();
        assertThat(ErrorStatus.fromResponseBody("not json")).isEmpty();
        assertThat(ErrorStatus.fromResponseBody("[]")).isEmpty();
        assertThat(ErrorStatus.fromResponseBody("{\"success\":true}")).isEmpty();
        assertThat(ErrorStatus.fromResponseBody("{\"errorStatus\":null}")).isEmpty();
        assertThat(ErrorStatus.fromResponseBody("{\"errorStatus\":{\"errorCode\":5}}")).isEmpty();
    }
}
