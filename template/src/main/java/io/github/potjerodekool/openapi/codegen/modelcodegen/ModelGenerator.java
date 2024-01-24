package io.github.potjerodekool.openapi.codegen.modelcodegen;

import io.github.potjerodekool.openapi.codegen.*;
import io.github.potjerodekool.openapi.codegen.modelcodegen.builder.JavaModelBuilder;
import io.github.potjerodekool.openapi.codegen.modelcodegen.builder.ModelBuilder;
import io.github.potjerodekool.openapi.codegen.modelcodegen.model.element.Annotation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

public class ModelGenerator implements ApiWalkerListener {

    private final Templates templates;

    private final Set<String> processed = new HashSet<>();

    private final ModelBuilder modelBuilder = new JavaModelBuilder();

    public ModelGenerator(final Templates templates) {
        this.templates = templates;
    }

    public void generateModels(final OpenAPI openAPI) {
        final var walker = new ApiWalker();
        walker.walk(openAPI, this);
    }

    @Override
    public void visitSchema(final OpenAPI openAPI, final HttpMethod httpMethod, final Schema<?> schema) {
        final var resolvedSchemaResult = SchemaResolver.resolve(openAPI, schema);

        if (resolvedSchemaResult.schema() == null) {
            return;
        }

        final var name = httpMethod == HttpMethod.PATCH
                ? "Patch" + resolvedSchemaResult.name()
                : resolvedSchemaResult.name();

        if (processed.contains(name)) {
            return;
        }

        processed.add(name);

        final var resolvedSchema = resolvedSchemaResult.schema();

        if (!shouldProcess(resolvedSchema)) {
            return;
        }

        final var model = modelBuilder.build(openAPI, httpMethod, name, resolvedSchema);
        final var template = templates.getInstanceOf("model");

        model.packageName("org.some.api.model");

        if (model.getSimpleName() == null) {
            throw new UnsupportedOperationException();
        }

        final var date = DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now());

        model.annotation(new Annotation()
                .name("javax.annotation.processing.Generated")
                .attribute("value", getClass().getName())
                .attribute("date", date)
        );

        template.add("model", model);
        final var code = template.render();

        System.out.println("Generated code:\n");
        System.out.println(code);
    }

    private boolean shouldProcess(final Schema<?> schema) {
        return !(schema instanceof ArraySchema);
    }
}
