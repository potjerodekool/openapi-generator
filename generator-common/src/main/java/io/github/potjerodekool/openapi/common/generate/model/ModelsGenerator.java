package io.github.potjerodekool.openapi.common.generate.model;

import io.github.potjerodekool.codegen.io.Filer;
import io.github.potjerodekool.codegen.io.Location;
import io.github.potjerodekool.codegen.template.model.annotation.Annot;
import io.github.potjerodekool.openapi.common.dependency.DependencyChecker;
import io.github.potjerodekool.openapi.common.generate.*;
import io.github.potjerodekool.openapi.common.generate.model.adapter.ModelAdapter;
import io.github.potjerodekool.openapi.common.generate.model.builder.JavaModelBuilder;
import io.github.potjerodekool.openapi.common.generate.model.element.Model;
import io.github.potjerodekool.openapi.common.OpenApiEnvironment;
import io.swagger.models.HttpMethod;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ModelsGenerator implements OpenApiWalkerListener {

    private final Templates templates;

    private final String modelPackageName;

    private final Filer filer;

    private final Set<String> processed = new HashSet<>();

    private final JavaModelBuilder modelBuilder;

    private final List<ModelAdapter> adapters = new ArrayList<>();

    public ModelsGenerator(final Templates templates,
                           final String modelPackageName,
                           final OpenApiEnvironment openApiEnvironment, final OpenApiTypeUtils typeUtils) {
        final var applicationContext = openApiEnvironment.getApplicationContext();
        final var dependencyChecker = openApiEnvironment.getProject().dependencyChecker();

        this.templates = templates;
        this.modelPackageName = modelPackageName;
        this.filer = openApiEnvironment.getEnvironment().getFiler();
        this.modelBuilder = new JavaModelBuilder(modelPackageName, typeUtils);
        registerDefaultModelAdapters(dependencyChecker);
        final var adapters = applicationContext.getBeansOfType(ModelAdapter.class);
        this.adapters.addAll(adapters);
    }

    private void registerDefaultModelAdapters(final DependencyChecker dependencyChecker) {
        ServiceLoader.load(ModelAdapter.class).forEach(this.adapters::add);
    }

    public void generateModels(final OpenAPI openAPI) {
        OpenApiWalker.walk(openAPI, this);
    }

    @Override
    public void visitContent(final OpenAPI openAPI,
                             final HttpMethod httpMethod,
                             final String path,
                             final Operation operation,
                             final Content content) {
        final var contentType = (MediaType) content.get(ContentTypes.JSON);

        if (contentType != null) {
            final List<Map<String, Object>> typeArgs = ExtensionsHelper.getExtension(
                    contentType.getExtensions(),
                    Extensions.TYPE_ARGS,
                    List.class
            );

            if (typeArgs != null) {
                typeArgs.forEach(typeArg -> {
                    final var ref = (String) typeArg.get("$ref");

                    if (ref != null) {
                        final var resolved = SchemaResolver.resolve(openAPI, ref);
                        visitSchema(openAPI, httpMethod, path, operation, resolved.schema(), resolved.name());
                    }
                });
            }
        }
    }

    @Override
    public void visitSchema(final OpenAPI openAPI,
                            final HttpMethod httpMethod,
                            final String path,
                            final Operation operation,
                            final Schema<?> schema,
                            final String schemaName) {
        if (schema instanceof ArraySchema arraySchema) {
            visitSchema(openAPI, httpMethod, path, operation, arraySchema.getItems(), null);
            return;
        }

        final var resolvedSchemaResult = SchemaResolver.resolve(openAPI, schema);

        if (resolvedSchemaResult.schema() == null) {
            return;
        }

        var name = (resolvedSchemaResult.name() != null
                ? resolvedSchemaResult.name()
                : schemaName);

        if (httpMethod == HttpMethod.PATCH
                && name != null
                && !name.toLowerCase().contains("patch")) {
            name = "Patch" + name;
        }

        if (processed.contains(name)) {
            return;
        }

        processed.add(name);

        final var resolvedSchema = resolvedSchemaResult.schema();

        if (!shouldProcess(resolvedSchema)) {
            return;
        }

        final var model = buildModel(
                openAPI,
                httpMethod,
                name,
                resolvedSchema
        );

        adaptModel(model, resolvedSchema);
        writeCode(model);

        processProperties(openAPI, httpMethod, path, operation, resolvedSchema);
    }

    private void processProperties(final OpenAPI openAPI,
                                   final HttpMethod httpMethod,
                                   final String path,
                                   final Operation operation,
                                   final Schema<?> resolvedSchema) {
        final Map<String, Schema> properties = Objects.requireNonNullElse(
                resolvedSchema.getProperties(),
                Map.of()
        );

        if (resolvedSchema instanceof ObjectSchema
                || resolvedSchema instanceof ComposedSchema) {
            properties.values().forEach(propertySchema ->
                    visitSchema(openAPI, httpMethod, path, operation, propertySchema, null));
        }
    }

    private Model buildModel(final OpenAPI openAPI,
                             final HttpMethod httpMethod,
                             final String name,
                             final Schema<?> resolvedSchema) {
        generateSuperClass(openAPI, httpMethod, name, resolvedSchema);

        final var model = modelBuilder.build(openAPI, httpMethod, modelPackageName, name, resolvedSchema);

        if (model.getSimpleName() == null) {
            throw new UnsupportedOperationException();
        }

        final var date = DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now());

        model.annotation(new Annot()
                .name("javax.annotation.processing.Generated")
                .value("value", getClass().getName())
                .value("date", date)
        );

        processExtensions(openAPI, httpMethod, resolvedSchema);

        return model;
    }

    private void generateSuperClass(final OpenAPI openAPI,
                                    final HttpMethod httpMethod,
                                    final String name,
                                    final Schema<?> resolvedSchema) {
        if (resolvedSchema.getAllOf() != null && resolvedSchema.getAllOf().size() == 1) {
            //Generate super class
            final var parentSchema = resolvedSchema.getAllOf().getFirst();
            visitSchema(openAPI, httpMethod, name, null, parentSchema, null);
        }
    }

    private void processExtensions(final OpenAPI openAPI,
                                   final HttpMethod httpMethod,
                                   final Schema<?> resolvedSchema) {
        final List<Map<String, Object>> superTypeArgs = ExtensionsHelper.getExtension(resolvedSchema.getExtensions(), Extensions.SUPER_TYPE_ARGS, List.class);

        if (superTypeArgs != null) {
            superTypeArgs.stream()
                    .map(typeArg -> (String) typeArg.get("$ref"))
                    .filter(Objects::nonNull)
                    .forEach(ref -> {
                        final var resolved = SchemaResolver.resolve(openAPI, ref);

                        if (resolved.schema() != null) {
                            visitSchema(openAPI, httpMethod, resolved.name(), null, resolved.schema(), resolved.name());
                        }
                    });
        }
    }

    private void adaptModel(final Model model,
                            final Schema<?> schema) {
        if (schema instanceof ObjectSchema objectSchema) {
            for (final var adapter : adapters) {
                adapter.adapt(model, objectSchema);
            }
        }
    }

    private void writeCode(final Model model) {
        final var template = templates.getInstanceOf("/model/model");
        template.add("model", model);
        final var code = template.render();

        final var resource = filer.createResource(
                Location.SOURCE_OUTPUT,
                this.modelPackageName,
                model.getSimpleName() + ".java"
        );

        resource.writeToOutputStream(code.getBytes());
    }

    private boolean shouldProcess(final Schema<?> schema) {
        return schema instanceof ObjectSchema
                || schema instanceof ComposedSchema;
    }
}
