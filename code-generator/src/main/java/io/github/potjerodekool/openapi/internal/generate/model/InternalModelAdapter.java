package io.github.potjerodekool.openapi.internal.generate.model;

import io.github.potjerodekool.openapi.DependencyChecker;
import io.github.potjerodekool.openapi.OpenApiGeneratorConfig;
import io.github.potjerodekool.openapi.generate.model.ModelAdapter;
import io.github.potjerodekool.openapi.internal.generate.GenerateUtils;
import io.github.potjerodekool.openapi.internal.generate.Types;

public interface InternalModelAdapter extends ModelAdapter {

    default void init(final OpenApiGeneratorConfig config,
                      final Types types,
                      final DependencyChecker dependencyChecker,
                      final GenerateUtils generateUtils) {
        this.init(config, types, dependencyChecker);
    }
}
