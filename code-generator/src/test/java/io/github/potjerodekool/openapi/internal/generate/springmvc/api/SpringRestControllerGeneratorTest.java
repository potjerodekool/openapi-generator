package io.github.potjerodekool.openapi.internal.generate.springmvc.api;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.Language;
import io.github.potjerodekool.codegen.model.element.ElementKind;
import io.github.potjerodekool.codegen.model.tree.java.JMethodDeclaration;
import io.github.potjerodekool.codegen.model.tree.type.ClassOrInterfaceTypeExpression;
import io.github.potjerodekool.openapi.*;
import io.github.potjerodekool.openapi.internal.OpenApiTreeBuilder;
import io.github.potjerodekool.openapi.internal.generate.springmvc.OpenApiTypeUtilsSpringImpl;
import io.github.potjerodekool.openapi.internal.type.OpenApiTypeUtils;
import io.github.potjerodekool.openapi.tree.OpenApiContent;
import io.github.potjerodekool.openapi.tree.OpenApiOperation;
import io.github.potjerodekool.openapi.tree.OpenApiPath;
import io.github.potjerodekool.openapi.tree.OpenApiResponse;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import static io.github.potjerodekool.openapi.ResourceLoader.loadAsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringRestControllerGeneratorTest {

    private SpringRestControllerGenerator generator;

    @BeforeEach
    void setup() {
        final var generatorConfig = new GeneratorConfig(
                Language.JAVA,
                "org.some.api",
                Map.of()
        );

        final var apiConfiguration = mock(ApiConfiguration.class);
        when(apiConfiguration.basePackageName()).thenReturn("org.some.api");

        final var environment = mock(Environment.class);

        final var openApiTypeUtils = new OpenApiTypeUtilsSpringImpl();

        generator = new SpringRestControllerGenerator(
                generatorConfig,
                apiConfiguration,
                environment,
                openApiTypeUtils
        );
    }

    @Test
    void postProcessOperation() {
        final var operation = new OpenApiOperation(
                "",
                "",
                "login",
                List.of(),
                List.of(),
                null,
                Map.of(
                        "200",
                        new OpenApiResponse(
                                "",
                                Map.of(
                                        "application/json",
                                        new OpenApiContent("", null, Map.of())
                                ),
                                Map.of()
                        )
                ),
                null
        );

        final var method = new JMethodDeclaration(
                "login",
                ElementKind.METHOD,
                new ClassOrInterfaceTypeExpression("org.springframework.http.ResponseEntity"),
                List.of(),
                List.of(),
                null
        );

        generator.postProcessOperation(
                HttpMethod.POST,
                "/login",
                operation,
                method
        );

        final var printer = new AstPrinter();
        method.accept(printer, null);
        final var code = printer.getCode();
        System.out.println(code);
    }

    private SwaggerParseResult parse(final File file) {
        return new OpenAPIParser().readLocation(file.getAbsolutePath(), null, null);
    }

    private SwaggerParseResult parse(final String resourceName) {
        final var resource = getClass().getClassLoader().getResource(resourceName);
        try {
            final var url = resource.toURI().toString();
            return new OpenAPIParser().readLocation(url, null, null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void test() {
        final var apiConfiguration = new ApiConfiguration(
                new File(""),
                "org.some.petstore.api",
                true,
                true,
                true,
                Map.of()
        );

        final var builder = new OpenApiTreeBuilder(apiConfiguration);
        final var api = builder.build(parse("petstore.yaml"));

        final var createPetOperation = api.paths().stream()
                .filter(path -> "/pets".equals(path.path()))
                .map(OpenApiPath::post)
                .findFirst().get();

        final var method = new JMethodDeclaration(
                "createPet",
                ElementKind.METHOD,
                new ClassOrInterfaceTypeExpression("org.springframework.http.ResponseEntity"),
                List.of(),
                List.of(),
                null
        );

        generator.postProcessOperation(
                HttpMethod.POST,
                "/pets",
                createPetOperation,
                method
        );

        final var printer = new AstPrinter();
        //method.accept(printer, null);
        method.getBody().get().accept(printer, null);
        final var actual = printer.getCode();
        System.out.println(actual);

        final var expected = loadAsString("SpringRestControllerGeneratorTest/createPet", true);
        assertEquals(expected, actual);
    }
}