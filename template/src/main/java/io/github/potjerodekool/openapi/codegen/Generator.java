package io.github.potjerodekool.openapi.codegen;

import io.github.potjerodekool.openapi.codegen.modelcodegen.ModelGenerator;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.io.File;

public class Generator {

    public void generate(final File file) {
        final var parseResult = parse(file);
        final var openApi = parseResult.getOpenAPI();
        final var templates = new Templates();

        final var modelGenerator = new ModelGenerator(templates);
        modelGenerator.generateModels(openApi);
    }


    private SwaggerParseResult parse(final File file) {
        return new OpenAPIParser().readLocation(file.getAbsolutePath(), null, null);
    }
}
