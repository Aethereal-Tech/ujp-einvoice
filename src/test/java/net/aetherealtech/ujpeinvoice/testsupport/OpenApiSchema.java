package net.aetherealtech.ujpeinvoice.testsupport;

import net.aetherealtech.ujpeinvoice.internal.json.JsonArray;
import net.aetherealtech.ujpeinvoice.internal.json.JsonBoolean;
import net.aetherealtech.ujpeinvoice.internal.json.JsonNull;
import net.aetherealtech.ujpeinvoice.internal.json.JsonNumber;
import net.aetherealtech.ujpeinvoice.internal.json.JsonObject;
import net.aetherealtech.ujpeinvoice.internal.json.JsonParser;
import net.aetherealtech.ujpeinvoice.internal.json.JsonString;
import net.aetherealtech.ujpeinvoice.internal.json.JsonValue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A small OpenAPI 3.1 reader and JSON Schema checker for tests, so the committed API description needs no
 * test dependency. It reads {@code $ref}, {@code type}, {@code properties}, {@code required}, {@code items},
 * {@code additionalProperties}, {@code enum} and the {@code date} and {@code date-time} formats, and nothing else.
 *
 * <p>Two leniencies, both because the gateway's own examples need them (see {@code ujp/README.md}): a JSON
 * {@code null} is accepted for any member that is not {@code required}, and a member the schema does not declare
 * is accepted. What it checks is therefore a member's type, format and enum, and the presence of a
 * {@code required} member, which this description rarely declares.
 */
public final class OpenApiSchema {

    private static final String SCHEMA_PREFIX = "#/components/schemas/";

    private final JsonObject root;

    private OpenApiSchema(JsonObject root) {
        this.root = root;
    }

    public static OpenApiSchema load(String classpathResource) {
        try (InputStream in = OpenApiSchema.class.getResourceAsStream(classpathResource)) {
            if (in == null) {
                throw new IllegalStateException("No classpath resource " + classpathResource);
            }
            return of(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static OpenApiSchema of(String json) {
        return new OpenApiSchema((JsonObject) JsonParser.parse(json));
    }

    /** The path of the first server, for example {@code /einvoice_api}: the root every path is relative to. */
    public String serverPath() {
        JsonObject server = (JsonObject) root.getArray("servers").items().getFirst();
        return URI.create(server.getString("url")).getPath();
    }

    public boolean hasPath(String pathBelowServer) {
        return root.getObject("paths").get(pathBelowServer).isPresent();
    }

    /** The operation for {@code method} at {@code pathBelowServer}, or empty. */
    public Optional<JsonObject> operation(String method, String pathBelowServer) {
        return root.getObject("paths").get(pathBelowServer)
                .map(JsonObject.class::cast)
                .flatMap(path -> path.get(method.toLowerCase()))
                .map(JsonObject.class::cast);
    }

    /** The names of the header parameters the operation declares {@code required}. */
    public List<String> requiredHeaders(JsonObject operation) {
        List<String> headers = new ArrayList<>();
        operation.get("parameters").map(JsonArray.class::cast).ifPresent(parameters -> parameters.items().forEach(item -> {
            JsonObject parameter = (JsonObject) item;
            boolean required = parameter.get("required").orElse(JsonBoolean.FALSE) instanceof JsonBoolean b && b.value();
            if ("header".equals(parameter.getString("in")) && required) {
                headers.add(parameter.getString("name"));
            }
        }));
        return headers;
    }

    /** The name of the component schema the operation declares for {@code httpStatus}, or empty. */
    public Optional<String> responseSchemaName(JsonObject operation, int httpStatus) {
        return operation.getObject("responses").get(Integer.toString(httpStatus))
                .map(JsonObject.class::cast)
                .flatMap(response -> firstMediaSchema(response))
                .map(OpenApiSchema::schemaName);
    }

    /** The name of the component schema the operation declares for its request body, or empty. */
    public Optional<String> requestSchemaName(JsonObject operation) {
        return operation.get("requestBody").map(JsonObject.class::cast)
                .flatMap(OpenApiSchema::firstMediaSchema)
                .map(OpenApiSchema::schemaName);
    }

    public Optional<String> requestMediaType(JsonObject operation) {
        return operation.get("requestBody").map(JsonObject.class::cast)
                .map(body -> body.getObject("content").members().keySet().iterator().next());
    }

    public JsonObject schema(String name) {
        return root.getObject("components").getObject("schemas").getObject(name);
    }

    /** Every violation of the named component schema by {@code instance}; empty when it conforms. */
    public List<String> validate(String schemaName, JsonValue instance) {
        List<String> violations = new ArrayList<>();
        check(schema(schemaName), instance, "$", false, violations);
        return violations;
    }

    public List<String> validate(String schemaName, String json) {
        return validate(schemaName, JsonParser.parse(json));
    }

    private void check(JsonObject schema, JsonValue instance, String at, boolean required, List<String> violations) {
        JsonObject resolved = resolve(schema);
        if (instance instanceof JsonNull) {
            if (required) {
                violations.add(at + ": null where the schema requires a value");
            }
            return;
        }
        resolved.get("type").ifPresent(type -> checkType(((JsonString) type).value(), instance, at, violations));
        resolved.get("enum").ifPresent(values -> {
            if (!((JsonArray) values).items().contains(instance)) {
                violations.add(at + ": " + instance + " is not in the enum");
            }
        });
        resolved.get("format").ifPresent(format -> checkFormat(((JsonString) format).value(), instance, at, violations));
        if (instance instanceof JsonObject object) {
            checkObject(resolved, object, at, violations);
        } else if (instance instanceof JsonArray array && resolved.get("items").isPresent()) {
            JsonObject items = (JsonObject) resolved.get("items").get();
            for (int i = 0; i < array.items().size(); i++) {
                check(items, array.items().get(i), at + "[" + i + "]", false, violations);
            }
        }
    }

    private void checkObject(JsonObject schema, JsonObject object, String at, List<String> violations) {
        List<String> required = new ArrayList<>();
        schema.get("required").map(JsonArray.class::cast)
                .ifPresent(names -> names.items().forEach(name -> required.add(((JsonString) name).value())));
        required.forEach(name -> {
            if (object.get(name).isEmpty()) {
                violations.add(at + "." + name + ": required member is missing");
            }
        });
        JsonObject properties = schema.get("properties").map(JsonObject.class::cast).orElse(JsonObject.builder().build());
        object.members().forEach((name, value) -> {
            if (properties.get(name).isPresent()) {
                check((JsonObject) properties.get(name).get(), value, at + "." + name, required.contains(name), violations);
            } else if (schema.get("additionalProperties").orElse(null) instanceof JsonObject additional) {
                check(additional, value, at + "." + name, false, violations);
            }
        });
    }

    private void checkType(String type, JsonValue instance, String at, List<String> violations) {
        boolean ok = switch (type) {
            case "object" -> instance instanceof JsonObject;
            case "array" -> instance instanceof JsonArray;
            case "string" -> instance instanceof JsonString;
            case "number" -> instance instanceof JsonNumber;
            case "integer" -> instance instanceof JsonNumber n && n.value().stripTrailingZeros().scale() <= 0;
            case "boolean" -> instance instanceof JsonBoolean;
            default -> throw new IllegalStateException("Unsupported schema type " + type);
        };
        if (!ok) {
            violations.add(at + ": " + instance + " is not a " + type);
        }
    }

    private void checkFormat(String format, JsonValue instance, String at, List<String> violations) {
        if (!(instance instanceof JsonString text)) {
            return;
        }
        try {
            switch (format) {
                case "date" -> LocalDate.parse(text.value());
                case "date-time" -> {
                    try {
                        LocalDateTime.parse(text.value());
                    } catch (RuntimeException e) {
                        OffsetDateTime.parse(text.value());
                    }
                }
                default -> {
                }
            }
        } catch (RuntimeException e) {
            violations.add(at + ": " + instance + " is not a " + format);
        }
    }

    private JsonObject resolve(JsonObject schema) {
        return schema.get("$ref").map(ref -> schema(schemaName(schema))).orElse(schema);
    }

    private static Optional<JsonObject> firstMediaSchema(JsonObject holder) {
        return holder.get("content").map(JsonObject.class::cast)
                .flatMap(content -> content.members().values().stream().findFirst())
                .map(JsonObject.class::cast)
                .flatMap(media -> media.get("schema")).map(JsonObject.class::cast);
    }

    private static String schemaName(JsonObject referencing) {
        String ref = referencing.getString("$ref");
        if (!ref.startsWith(SCHEMA_PREFIX)) {
            throw new IllegalStateException("Unsupported $ref " + ref);
        }
        return ref.substring(SCHEMA_PREFIX.length());
    }
}
