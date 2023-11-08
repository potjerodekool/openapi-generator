package io.github.potjerodekool.openapi.internal.generate.model;

import io.github.potjerodekool.codegen.model.tree.MethodDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.VariableDeclaration;
import io.github.potjerodekool.openapi.generate.model.ModelAdapter;
import io.github.potjerodekool.openapi.internal.di.ApplicationContext;

import java.util.ArrayList;
import java.util.List;

public class CombinedModelAdapter implements ModelAdapter {

    private final List<ModelAdapter> modelAdapters = new ArrayList<>();

    public CombinedModelAdapter(final ApplicationContext applicationContext) {
        final var modelAdapters = applicationContext.getBeansOfType(ModelAdapter.class);
        this.modelAdapters.addAll(modelAdapters);
    }

    @Override
    public void adaptConstructor(final MethodDeclaration<?> constructor) {
        modelAdapters.forEach(modelAdapter -> modelAdapter.adaptConstructor(constructor));
    }

    @Override
    public void adaptField(final VariableDeclaration<?> field) {
        modelAdapters.forEach(modelAdapter -> modelAdapter.adaptField(field));
    }

    @Override
    public void adaptGetter(final MethodDeclaration<?> method) {
        modelAdapters.forEach(modelAdapter -> modelAdapter.adaptGetter(method));
    }

    @Override
    public void adaptSetter(final MethodDeclaration<?> method) {
        modelAdapters.forEach(modelAdapter -> modelAdapter.adaptSetter(method));
    }
}
