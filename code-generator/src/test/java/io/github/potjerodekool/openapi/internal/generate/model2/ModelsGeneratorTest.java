package io.github.potjerodekool.openapi.internal.generate.model2;

import io.github.potjerodekool.codegen.io.Location;
import io.github.potjerodekool.codegen.template.TemplateBasedGenerator;
import io.github.potjerodekool.openapi.InMemoryFileManager;
import io.github.potjerodekool.openapi.internal.generate.model.ModelsGenerator;
import io.github.potjerodekool.openapi.internal.generate.model.OpenApiWalker;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
class ModelsGeneratorTest {

    private SwaggerParseResult parse(final File file) {
        return new OpenAPIParser().readLocation(file.getAbsolutePath(), null, null);
    }

    @Test
    void generateModel() {
        final var parseResult = parse(new File("C:\\projects\\openapi-generator\\demo\\petstore\\petstore.yaml"));
        final var api = parseResult.getOpenAPI();

        final var modelGenerator = new ModelsGenerator("org.some.model");
        modelGenerator.registerDefaultModelAdapters();

        OpenApiWalker.walk(api, modelGenerator);

        final var models = modelGenerator.getModels();

        final var fileManager = new InMemoryFileManager();

        final var generator = new TemplateBasedGenerator();

        for (var model : models) {
            final var clazz = model.getClassDeclarations().get(0);
            final var fileName = clazz.getSimpleName() + ".java";
            final var code = generator.doGenerate(model);

            final var resource = fileManager.createResource(
                    Location.SOURCE_OUTPUT,
                    null,
                    fileName
            );

            try (var stream = resource.openOutputStream()) {
                stream.write(code.getBytes());
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }

            final var savedResource = fileManager.getResource(
                    Location.SOURCE_OUTPUT,
                    null,
                    fileName
            );

            final var data = new String(fileManager.getData(savedResource));
            System.out.println(data);
        }
    }
}