package io.github.potjerodekool.openapi.internal.generate.model;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import io.github.potjerodekool.openapi.dependency.DependencyChecker;
import io.github.potjerodekool.openapi.OpenApiGeneratorConfig;
import io.github.potjerodekool.openapi.generate.model.ModelAdapter;
import io.github.potjerodekool.openapi.HttpMethod;
import io.github.potjerodekool.openapi.RequestCycleLocation;
import io.github.potjerodekool.openapi.internal.DependencyAwareServiceProvider;
import io.github.potjerodekool.openapi.internal.ServiceLoader;
import io.github.potjerodekool.openapi.internal.generate.GenerateUtils;
import io.github.potjerodekool.openapi.internal.generate.Types;
import io.github.potjerodekool.openapi.tree.OpenApiProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class CombinedModelAdapter implements ModelAdapter {

    private final List<ModelAdapter> modelAdapters = new ArrayList<>();

    public CombinedModelAdapter(final OpenApiGeneratorConfig config,
                                final Types types,
                                final DependencyChecker dependencyChecker,
                                final GenerateUtils generateUtils) {
        this.modelAdapters.add(createValidationModelAdapter(config, types, dependencyChecker, generateUtils));
        this.modelAdapters.add(new CheckerFrameworkModelAdapter(config));

        final Function<String, ModelAdapter> dependencyAwareServiceProvider = DependencyAwareServiceProvider
                .create(List.of(config, types, dependencyChecker));

        ServiceLoader.load(ModelAdapter.class, dependencyAwareServiceProvider)
                .forEach(this.modelAdapters::add);
    }

    private ValidationModelAdapter createValidationModelAdapter(final OpenApiGeneratorConfig config,
                                                                final Types types,
                                                                final DependencyChecker dependencyChecker,
                                                                final GenerateUtils generateUtils) {
        if (dependencyChecker.isDependencyPresent(
                "org.hibernate.validator",
                "hibernate-validator")) {
            return new HibernateValidationModelAdapter(
                    config,
                    types,
                    dependencyChecker,
                    generateUtils
            );
        }

        return new ValidationModelAdapter(
                config,
                types,
                generateUtils
        );
    }

    @Override
    public void adaptField(final HttpMethod httpMethod,
                           final RequestCycleLocation requestCycleLocation,
                           final OpenApiProperty apiProperty,
                           final FieldDeclaration fieldDeclaration) {
        modelAdapters.forEach(modelAdapter -> modelAdapter.adaptField(httpMethod, requestCycleLocation, apiProperty, fieldDeclaration));
    }

    @Override
    public void adaptGetter(final HttpMethod httpMethod,
                            final RequestCycleLocation requestCycleLocation,
                            final OpenApiProperty property,
                            final MethodDeclaration methodDeclaration) {
        modelAdapters.forEach(modelAdapter -> modelAdapter.adaptGetter(httpMethod, requestCycleLocation, property, methodDeclaration));
    }

    @Override
    public void adaptSetter(final HttpMethod httpMethod,
                            final RequestCycleLocation requestCycleLocation,
                            final OpenApiProperty property,
                            final MethodDeclaration methodDeclaration) {
        modelAdapters.forEach(modelAdapter -> modelAdapter.adaptSetter(httpMethod, requestCycleLocation, property, methodDeclaration));
    }
}
