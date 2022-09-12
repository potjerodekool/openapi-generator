package io.github.potjerodekool.openapi.internal.generate.model;

import io.github.potjerodekool.openapi.HttpMethod;
import io.github.potjerodekool.openapi.RequestCycleLocation;
import io.github.potjerodekool.openapi.generate.model.ModelAdapter;
import io.github.potjerodekool.openapi.internal.ast.element.MethodElement;
import io.github.potjerodekool.openapi.internal.ast.element.VariableElement;
import io.github.potjerodekool.openapi.internal.di.ApplicationContext;
import io.github.potjerodekool.openapi.tree.OpenApiProperty;

import java.util.ArrayList;
import java.util.List;

public class CombinedModelAdapter implements ModelAdapter {

    private final List<ModelAdapter> modelAdapters = new ArrayList<>();

    public CombinedModelAdapter(final ApplicationContext applicationContext) {
        final var modelAdapters = applicationContext.getBeansOfType(ModelAdapter.class);
        this.modelAdapters.addAll(modelAdapters);
    }

    @Override
    public void adaptField(final HttpMethod httpMethod,
                           final RequestCycleLocation requestCycleLocation,
                           final OpenApiProperty property,
                           final VariableElement field) {
        modelAdapters.forEach(modelAdapter -> modelAdapter.adaptField(httpMethod, requestCycleLocation, property, field));
    }

    @Override
    public void adaptGetter(final HttpMethod httpMethod,
                            final RequestCycleLocation requestCycleLocation,
                            final OpenApiProperty property,
                            final MethodElement method) {
        modelAdapters.forEach(modelAdapter -> modelAdapter.adaptGetter(httpMethod, requestCycleLocation, property, method));
    }

    @Override
    public void adaptSetter(final HttpMethod httpMethod,
                            final RequestCycleLocation requestCycleLocation,
                            final OpenApiProperty property,
                            final MethodElement method) {
        modelAdapters.forEach(modelAdapter -> modelAdapter.adaptSetter(httpMethod, requestCycleLocation, property, method));
    }
}
