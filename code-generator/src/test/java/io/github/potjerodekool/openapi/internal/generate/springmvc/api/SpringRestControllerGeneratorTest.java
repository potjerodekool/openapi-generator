package io.github.potjerodekool.openapi.internal.generate.springmvc.api;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.Language;
import io.github.potjerodekool.codegen.model.element.ElementKind;
import io.github.potjerodekool.codegen.model.element.Name;
import io.github.potjerodekool.codegen.model.tree.MethodDeclaration;
import io.github.potjerodekool.codegen.model.tree.type.ClassOrInterfaceTypeExpression;
import io.github.potjerodekool.openapi.ApiConfiguration;
import io.github.potjerodekool.openapi.AstPrinter;
import io.github.potjerodekool.openapi.GeneratorConfig;
import io.github.potjerodekool.openapi.internal.generate.springmvc.OpenApiTypeUtilsSpringImpl;
import io.github.potjerodekool.openapi.internal.type.OpenApiTypeUtilsJava;
import io.swagger.models.HttpMethod;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Map;

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

        final var openApiTypeUtils = new OpenApiTypeUtilsSpringImpl(new OpenApiTypeUtilsJava("org.some.api.model"));

        generator = new SpringRestControllerGenerator(
                generatorConfig,
                apiConfiguration,
                environment,
                openApiTypeUtils
        );
    }

    @Test
    void postProcessOperation() {
        final var responses = new ApiResponses();
        responses.put("200", new ApiResponse()
                .content(new Content()
                        .addMediaType("application/json", null)
                )
        );

        final var operation = new Operation()
                .operationId("login")
                .responses(responses);

        final var method = new MethodDeclaration()
                .simpleName(Name.of("login"))
                .kind(ElementKind.METHOD)
                .returnType(new ClassOrInterfaceTypeExpression("org.springframework.http.ResponseEntity"));

        generator.postProcessOperation(
                null,
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

}