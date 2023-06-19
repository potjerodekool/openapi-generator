package io.github.potjerodekool.openapi.internal.generate.model;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.Language;
import io.github.potjerodekool.openapi.internal.di.ApplicationContext;
import io.github.potjerodekool.openapi.internal.type.OpenApiTypeUtils;
import io.github.potjerodekool.openapi.internal.util.TypeUtils;
import io.github.potjerodekool.openapi.tree.*;
import io.github.potjerodekool.openapi.type.OpenApiObjectType;
import io.github.potjerodekool.openapi.type.OpenApiType;

public class ModelsCodeGenerator {

    private final OpenApiTypeUtils openApiTypeUtils;
    private final TypeUtils typeUtils;
    private final Environment environment;
    private final ApplicationContext applicationContext;

    private final Language language;

    public ModelsCodeGenerator(final TypeUtils typeUtils,
                               final OpenApiTypeUtils openApiTypeUtils,
                               final Environment environment,
                               final Language language,
                               final ApplicationContext applicationContext) {
        this.openApiTypeUtils = openApiTypeUtils;
        this.typeUtils = typeUtils;
        this.environment = environment;
        this.applicationContext = applicationContext;
        this.language = language;
    }

    public void generate(final OpenApi api) {
        processPaths(api);
        generateSchemaModels(api);
    }

    private void processPaths(final OpenApi api) {
        api.paths().forEach(this::processPath);
    }

    private void processPath(final OpenApiPath path) {
        final var patch = path.patch();

        if (patch != null) {
            processPatchOperation(patch);
        }
    }

    private void processPatchOperation(final OpenApiOperation patch) {
        final var requestBody = patch.requestBody();

        if (requestBody != null) {
            processPatchRequestBody(requestBody);
        }
    }

    private void processPatchRequestBody(final OpenApiRequestBody requestBody) {
        final var content = requestBody.contentMediaType().get("application/json");

        if (content != null) {
            processPatchContent(content);
        }
    }

    private void processPatchContent(final OpenApiContent content) {
        final var schema = content.schema();

        if (schema != null) {
            processPatchSchema(schema);
        }
    }

    private void processPatchSchema(final OpenApiType schema) {
        if (schema instanceof OpenApiObjectType ot) {
            final var name = ot.name();
            final String patchSchemaName;

            if (name.toLowerCase().contains("patch")) {
                patchSchemaName = name;
            } else {
                patchSchemaName = "Patch" + name;
            }
            generateSchemaModel(patchSchemaName, ot, true);
        }
    }

    private void generateSchemaModels(final OpenApi api) {
        api.schemas().entrySet().stream()
                .filter(it -> it.getValue() instanceof OpenApiObjectType)
                .forEach( entry -> {
                    final var schemaName = entry.getKey();
                    final var schema = (OpenApiObjectType) entry.getValue();
            generateSchemaModel(schemaName, schema, false);
        });
    }

    private void generateSchemaModel(final String schemaName,
                                     final OpenApiObjectType schema,
                                     final boolean isPatchModel) {
        new ModelCodeGenerator(
                typeUtils,
                openApiTypeUtils,
                environment,
                language,
                applicationContext
        ).generateSchemaModel(schemaName, schema, isPatchModel);
    }

}
