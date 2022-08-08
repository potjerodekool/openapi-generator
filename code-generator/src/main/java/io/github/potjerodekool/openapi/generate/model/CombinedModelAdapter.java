package io.github.potjerodekool.openapi.generate.model;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import io.github.potjerodekool.openapi.HttpMethod;
import io.github.potjerodekool.openapi.OpenApiGeneratorConfig;
import io.github.potjerodekool.openapi.RequestCycleLocation;
import io.github.potjerodekool.openapi.generate.GenerateUtils;
import io.github.potjerodekool.openapi.generate.Types;
import io.github.potjerodekool.openapi.tree.OpenApiProperty;

import java.util.List;

public class CombinedModelAdapter implements ModelAdapter {

    private final List<ModelAdapter> modelAdapters;

    public CombinedModelAdapter(final OpenApiGeneratorConfig config,
                                final Types types,
                                final GenerateUtils generateUtils) {
        this.modelAdapters = List.of(
                new ValidationModelAdapter(config, types, generateUtils),
                new CheckerFrameworkModelAdapter(config)
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
