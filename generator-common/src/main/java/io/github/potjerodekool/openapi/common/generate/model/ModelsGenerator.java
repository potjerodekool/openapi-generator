package io.github.potjerodekool.openapi.common.generate.model;

import io.github.potjerodekool.codegen.io.Filer;
import io.github.potjerodekool.codegen.io.Location;
import io.github.potjerodekool.openapi.common.dependency.DependencyChecker;
import io.github.potjerodekool.openapi.common.generate.model.adapter.CheckerModelAdapter;
import io.github.potjerodekool.openapi.common.generate.model.adapter.HibernateValidationModelAdapter;
import io.github.potjerodekool.openapi.common.generate.model.adapter.ModelAdapter;
import io.github.potjerodekool.openapi.common.generate.model.builder.JavaModelBuilder;
import io.github.potjerodekool.openapi.common.generate.model.element.Annotation;
import io.github.potjerodekool.openapi.common.generate.model.element.Model;
import io.github.potjerodekool.openapi.common.generate.model.adapter.JakartaValidationModelAdapter;
import io.github.potjerodekool.openapi.common.OpenApiEnvironment;
import io.github.potjerodekool.openapi.common.generate.Templates;
import io.github.potjerodekool.openapi.common.generate.OpenApiWalker;
import io.github.potjerodekool.openapi.common.generate.OpenApiWalkerListener;
import io.github.potjerodekool.openapi.common.generate.SchemaResolver;
import io.swagger.models.HttpMethod;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;

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
                           final OpenApiEnvironment openApiEnvironment) {
        final var applicationContext = openApiEnvironment.getApplicationContext();
        final var dependencyChecker = openApiEnvironment.getProject().dependencyChecker();

        this.templates = templates;
        this.modelPackageName = modelPackageName;
        this.filer = openApiEnvironment.getEnvironment().getFiler();
        this.modelBuilder = new JavaModelBuilder(modelPackageName);
        registerDefaultModelAdapters(dependencyChecker);
        final var adapters = applicationContext.getBeansOfType(ModelAdapter.class);
        this.adapters.addAll(adapters);
    }

    private void registerDefaultModelAdapters(final DependencyChecker dependencyChecker) {
        if (dependencyChecker.isDependencyPresent("org.checkerframework", "checker-qual")) {
            this.adapters.add(new CheckerModelAdapter());
        }

        if (dependencyChecker.isDependencyPresent("org.hibernate", "hibernate-validator")) {
            this.adapters.add(new HibernateValidationModelAdapter());
        } else {
            this.adapters.add(new JakartaValidationModelAdapter());
        }

        ServiceLoader.load(ModelAdapter.class).forEach(this.adapters::add);
    }

    public void generateModels(final OpenAPI openAPI) {
        OpenApiWalker.walk(openAPI, this);
    }

    @Override
    public void visitSchema(final OpenAPI openAPI,
                            final HttpMethod httpMethod,
                            final String path,
                            final Operation operation,
                            final Schema<?> schema) {
        if (schema instanceof ArraySchema arraySchema) {
            visitSchema(openAPI, httpMethod, path, operation, arraySchema.getItems());
            return;
        }

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
        if (resolvedSchema instanceof ObjectSchema objectSchema) {
            objectSchema.getProperties().values().forEach(propertySchema ->
                    visitSchema(openAPI, httpMethod, path, operation, propertySchema));
        } else if (resolvedSchema instanceof ComposedSchema composedSchema) {
            resolvedSchema.getProperties().values().forEach(propertySchema ->
                    visitSchema(openAPI, httpMethod, path, operation, propertySchema));
        }
    }

    private Model buildModel(final OpenAPI openAPI,
                             final HttpMethod httpMethod,
                             final String name,
                             final Schema<?> resolvedSchema) {
        final var model = modelBuilder.build(openAPI, httpMethod, name, resolvedSchema);
        model.packageName(this.modelPackageName);

        if (model.getSimpleName() == null) {
            throw new UnsupportedOperationException();
        }

        final var date = DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now());

        model.annotation(new Annotation()
                .name("javax.annotation.processing.Generated")
                .attribute("value", getClass().getName())
                .attribute("date", date)
        );

        return model;
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
