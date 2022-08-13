package io.github.potjerodekool.openapi.internal.generate.model;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import io.github.potjerodekool.openapi.DependencyChecker;
import io.github.potjerodekool.openapi.OpenApiGeneratorConfig;
import io.github.potjerodekool.openapi.generate.model.ModelAdapter;
import io.github.potjerodekool.openapi.HttpMethod;
import io.github.potjerodekool.openapi.RequestCycleLocation;
import io.github.potjerodekool.openapi.internal.generate.GenerateUtils;
import io.github.potjerodekool.openapi.internal.generate.Types;
import io.github.potjerodekool.openapi.tree.OpenApiProperty;

import java.util.ArrayList;
import java.util.List;

public class CombinedModelAdapter implements InternalModelAdapter {

    private final List<ModelAdapter> modelAdapters = new ArrayList<>();

    @Override
    public void init(final OpenApiGeneratorConfig config,
                     final Types types,
                     final DependencyChecker dependencyChecker,
                     final GenerateUtils generateUtils) {
        this.modelAdapters.add(init(new ValidationModelAdapter(generateUtils), config, types, dependencyChecker, generateUtils));
        this.modelAdapters.add(init(new HibernateValidationModelAdapter(), config, types, dependencyChecker, generateUtils));
        this.modelAdapters.add(init(new CheckerFrameworkModelAdapter(), config, types, dependencyChecker, generateUtils));
    }

    private ModelAdapter init(final ModelAdapter modelAdapter,
                              final OpenApiGeneratorConfig config,
                              final Types types,
                              final DependencyChecker dependencyChecker,
                              final GenerateUtils generateUtils) {
        if (modelAdapter instanceof InternalModelAdapter internalModelAdapter) {
            internalModelAdapter.init(config, types, dependencyChecker, generateUtils);
        } else {
            modelAdapter.init(config, types, dependencyChecker);
        }
        return modelAdapter;
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
