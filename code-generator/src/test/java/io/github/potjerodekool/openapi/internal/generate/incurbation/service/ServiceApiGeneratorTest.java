package io.github.potjerodekool.openapi.internal.generate.incurbation.service;

import io.github.potjerodekool.codegen.Language;
import io.github.potjerodekool.codegen.template.TemplateBasedGenerator;
import io.github.potjerodekool.openapi.ApiConfiguration;
import io.github.potjerodekool.openapi.GeneratorConfig;
import io.github.potjerodekool.openapi.internal.generate.incurbation.TypeUtilsSpringImpl;
import io.swagger.parser.OpenAPIParser;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServiceApiGeneratorTest {

    @Test
    void test() {
        final var generatorConfig = new GeneratorConfig(
                Language.JAVA,
                "org.some.api",
                new HashMap<>()
        );

        final var apiConfig = mock(ApiConfiguration.class);
        when(apiConfig.basePackageName())
                .thenReturn(generatorConfig.basePackageName());

        final var typeUtils = new TypeUtilsSpringImpl();

        final var generator = new ServiceApiGenerator(
                generatorConfig,
                apiConfig,
                null,
                typeUtils
        );

        final var file = new File("C:\\projects\\openapi-generator\\demo\\petstore\\petstore.yaml");
        final var api = new OpenAPIParser().readLocation(file.getAbsolutePath(), null, null)
                        .getOpenAPI();

        generator.generate(api);

        final var templateBasedGenerator = new TemplateBasedGenerator();

        generator.getCompilationUnits().forEach(cu -> {
            final var code = templateBasedGenerator.doGenerate(cu);
            System.out.println(code);
        });
    }

}