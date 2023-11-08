package io.github.potjerodekool.openapi.internal.generate.model;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.openapi.internal.OpenApiEnvironment;
import io.github.potjerodekool.openapi.internal.di.ApplicationContext;
import io.github.potjerodekool.openapi.internal.type.OpenApiTypeUtils;
import io.github.potjerodekool.openapi.tree.*;
import io.github.potjerodekool.openapi.tree.media.OpenApiObjectSchema;
import io.github.potjerodekool.openapi.tree.media.OpenApiSchema;

public class ModelsCodeGenerator {

    private final OpenApiTypeUtils openApiTypeUtils;
    private final Environment environment;
    private final ApplicationContext applicationContext;

    public ModelsCodeGenerator(final OpenApiEnvironment openApiEnvironment) {
        this.openApiTypeUtils = openApiEnvironment.getOpenApiTypeUtils();
        this.environment = openApiEnvironment.getEnvironment();
        this.applicationContext = openApiEnvironment.getApplicationContext();
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

    private void processPatchSchema(final OpenApiSchema<?> schema) {
        if (schema instanceof OpenApiObjectSchema os) {
            final var name = os.name();
            final String patchSchemaName;

            if (name.toLowerCase().contains("patch")) {
                patchSchemaName = name;
            } else {
                patchSchemaName = "Patch" + name;
            }
            generateSchemaModel(patchSchemaName, os, true);
        }
    }

    private void generateSchemaModels(final OpenApi api) {
        api.components().schemas().entrySet().stream()
                .filter(it -> it.getValue() instanceof OpenApiObjectSchema)
                .forEach( entry -> {
                    final var schemaName = entry.getKey();
                    final var schema = (OpenApiObjectSchema) entry.getValue();
            generateSchemaModel(schemaName, schema, false);
        });
    }

    private void generateSchemaModel(final String schemaName,
                                     final OpenApiObjectSchema schema,
                                     final boolean isPatchModel) {
        new ModelCodeGenerator(
                openApiTypeUtils,
                environment,
                applicationContext
        ).generateSchemaModel(schemaName, schema, isPatchModel);
    }

}
