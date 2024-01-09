package io.github.potjerodekool.openapi.internal.generate.config;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.Language;
import io.github.potjerodekool.openapi.GeneratorConfig;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.File;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SpringOpenApiConfigGeneratorTest {

    private SwaggerParseResult parse(final File file) {
        return new OpenAPIParser().readLocation(file.getAbsolutePath(), null, null);
    }

    private SwaggerParseResult parse(final String url) {
        return new OpenAPIParser().readLocation(url, null, null);
    }

    @Test
    void generate() {
        final var generatorConfig = new GeneratorConfig(
                Language.JAVA,
                "org.some",
                Map.of()
        );

        final var environment = Mockito.mock(Environment.class);

        final var generator = new SpringOpenApiConfigGenerator(
                generatorConfig,
                environment
        );

        final var url = getClass().getClassLoader().getResource("petstore.yaml").toExternalForm();
        final var openApi = parse(url).getOpenAPI();

        // petstore.yaml

        generator.generate(openApi);
    }
}