package net.aetherealtech.ujpeinvoice.testsupport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The conformance helper must reject what it claims to reject, or a green conformance run proves nothing. */
class OpenApiSchemaTest {

    private static final OpenApiSchema API = OpenApiSchema.of("""
            {
              "servers": [{"url": "https://host.test/root"}],
              "paths": {"/p": {"post": {
                "parameters": [{"name":"X-A","in":"header","required":true},
                               {"name":"X-B","in":"header"}, {"name":"q","in":"query","required":true}],
                "requestBody": {"content": {"application/json": {"schema": {"$ref": "#/components/schemas/Thing"}}}},
                "responses": {"200": {"content": {"application/json": {"schema": {"$ref": "#/components/schemas/Thing"}}}},
                              "204": {"description": "none"}}}}},
              "components": {"schemas": {
                "Color": {"type": "string", "enum": ["red", "green"]},
                "Thing": {"type": "object", "required": ["id"], "properties": {
                  "id": {"type": "integer"}, "name": {"type": "string"}, "on": {"type": "boolean"},
                  "price": {"type": "number"}, "day": {"type": "string", "format": "date"},
                  "at": {"type": "string", "format": "date-time"}, "color": {"$ref": "#/components/schemas/Color"},
                  "tags": {"type": "array", "items": {"type": "string"}},
                  "labels": {"type": "object", "additionalProperties": {"type": "array", "items": {"type": "string"}}},
                  "child": {"$ref": "#/components/schemas/Thing"}}}}}
            }""");

    @Test
    void readsTheServerAndTheOperations() {
        assertThat(API.serverPath()).isEqualTo("/root");
        assertThat(API.hasPath("/p")).isTrue();
        assertThat(API.hasPath("/q")).isFalse();
        assertThat(API.operation("POST", "/p")).isPresent();
        assertThat(API.operation("GET", "/p")).isEmpty();
        assertThat(API.operation("POST", "/missing")).isEmpty();
    }

    @Test
    void readsHeadersBodiesAndResponses() {
        var operation = API.operation("POST", "/p").orElseThrow();

        assertThat(API.requiredHeaders(operation)).containsExactly("X-A");
        assertThat(API.requestSchemaName(operation)).contains("Thing");
        assertThat(API.requestMediaType(operation)).contains("application/json");
        assertThat(API.responseSchemaName(operation, 200)).contains("Thing");
        assertThat(API.responseSchemaName(operation, 204)).isEmpty();
        assertThat(API.responseSchemaName(operation, 500)).isEmpty();
    }

    @Test
    void anOperationWithoutParametersOrABodyDeclaresNone() {
        OpenApiSchema bare = OpenApiSchema.of("""
                {"servers":[{"url":"https://h.test/x"}],"paths":{"/g":{"get":{"responses":{}}}},
                 "components":{"schemas":{}}}""");
        var operation = bare.operation("GET", "/g").orElseThrow();

        assertThat(bare.requiredHeaders(operation)).isEmpty();
        assertThat(bare.requestSchemaName(operation)).isEmpty();
        assertThat(bare.requestMediaType(operation)).isEmpty();
    }

    @Test
    void aConformingBodyHasNoViolationsAndNullIsAcceptedWhereNotRequired() {
        assertThat(API.validate("Thing", """
                {"id": 3, "name": "n", "on": true, "price": 1.5, "day": "2026-05-20", "at": "2026-05-20T01:02:03",
                 "color": "red", "tags": ["a"], "labels": {"k": ["v"]}, "child": {"id": 4}, "extra": 1, "name2": null}"""))
                .isEmpty();
        assertThat(API.validate("Thing", "{\"id\": 1, \"name\": null, \"child\": null, \"at\": \"2026-05-20T01:02:03Z\"}"))
                .isEmpty();
    }

    @Test
    void aWrongTypeIsAViolation() {
        assertThat(API.validate("Thing", "{\"id\": 1.5}")).hasSize(1);
        assertThat(API.validate("Thing", "{\"id\": 1, \"name\": 4}")).hasSize(1);
        assertThat(API.validate("Thing", "{\"id\": 1, \"on\": \"yes\"}")).hasSize(1);
        assertThat(API.validate("Thing", "{\"id\": 1, \"price\": \"1\"}")).hasSize(1);
        assertThat(API.validate("Thing", "{\"id\": 1, \"tags\": \"a\"}")).hasSize(1);
        assertThat(API.validate("Thing", "{\"id\": 1, \"child\": []}")).hasSize(1);
        assertThat(API.validate("Thing", "[]")).hasSize(1);
    }

    @Test
    void aNestedViolationIsFoundAndLocated() {
        assertThat(API.validate("Thing", "{\"id\": 1, \"tags\": [\"a\", 2], \"child\": {\"id\": \"x\"}}"))
                .anyMatch(v -> v.startsWith("$.tags[1]"))
                .anyMatch(v -> v.startsWith("$.child.id"));
        assertThat(API.validate("Thing", "{\"id\": 1, \"labels\": {\"k\": [1]}}"))
                .anyMatch(v -> v.startsWith("$.labels.k[0]"));
    }

    @Test
    void anEnumFormatAndRequiredMemberAreChecked() {
        assertThat(API.validate("Thing", "{\"id\": 1, \"color\": \"blue\"}")).hasSize(1);
        assertThat(API.validate("Thing", "{\"id\": 1, \"day\": \"20/05/2026\"}")).hasSize(1);
        assertThat(API.validate("Thing", "{\"id\": 1, \"at\": \"later\"}")).hasSize(1);
        assertThat(API.validate("Thing", "{}")).hasSize(1);
        assertThat(API.validate("Thing", "{\"id\": null}")).hasSize(1);
    }

    @Test
    void anUnsupportedTypeOrReferenceIsRefusedLoudly() {
        OpenApiSchema odd = OpenApiSchema.of("""
                {"servers":[{"url":"https://h.test"}],"paths":{},
                 "components":{"schemas":{"A":{"type":"null"},"B":{"$ref":"#/definitions/X"}}}}""");

        assertThatThrownBy(() -> odd.validate("A", "1")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> odd.validate("B", "1")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aMissingResourceIsRefused() {
        assertThatThrownBy(() -> OpenApiSchema.load("/ujp/none.json")).isInstanceOf(IllegalStateException.class);
    }
}
