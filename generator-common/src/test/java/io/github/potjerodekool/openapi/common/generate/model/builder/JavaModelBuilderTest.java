package io.github.potjerodekool.openapi.common.generate.model.builder;

import io.github.potjerodekool.openapi.common.generate.ContentType;
import io.github.potjerodekool.openapi.common.generate.StandardOpenApiTypeUtils;
import io.github.potjerodekool.openapi.common.generate.model.ModelAsserter;
import io.github.potjerodekool.openapi.common.generate.model.element.Model;
import io.swagger.models.HttpMethod;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class JavaModelBuilderTest {

    @ParameterizedTest()
    @ValueSource(strings = {"models.json", "models-3.1.json"})
    void build(final String resourceName) {
        final var typeUtils = new StandardOpenApiTypeUtils();

        final var builder = new JavaModelBuilder(
                "org.some.example",
                typeUtils
        );

        final var parseResult = parse(resourceName);
        final var openApi = parseResult.getOpenAPI();
        final var models = new HashMap<String, Model>();

        openApi.getComponents().getSchemas().forEach((schemaName, schema) -> {
                    final var model = builder.build(
                            openApi,
                            HttpMethod.GET,
                            "org.some.example",
                            schemaName,
                            schema,
                            ContentType.JSON
                    );
                    models.put(schemaName, model);
                }
            );

        new ModelAsserter(models.get("Person"))
                .assertProperty("id", it -> it.assertType("java.util.UUID"))
                .assertProperty("firstName", it -> it.assertType("java.lang.String"))
                .assertProperty("birthDay", it -> it.assertType("java.time.LocalDate"))
                .assertProperty("someInteger", it -> it.assertType("java.lang.Integer"))
                .assertProperty("someLong", it -> it.assertType("java.lang.Long"))
                .assertProperty("someFloat", it -> it.assertType("java.lang.Float"))
                .assertProperty("someDouble", it -> it.assertType("java.lang.Double"))
                .assertProperty("someBoolean", it -> it.assertType("java.lang.Boolean"))
                .assertProperty("gender", it -> it.assertType("org.some.example.Gender"));

        new ModelAsserter(models.get("Gender"))
                .assertEnumConstants("MALE", "FEMALE");

    }

    private SwaggerParseResult parse(final String url) {
        final var resource = getClass().getClassLoader()
                .getResource(url);
        final var location = resource.toString();
        return new OpenAPIParser().readLocation(location, null, null);
    }
}