package io.github.potjerodekool.openapi.generate.model;

import io.github.potjerodekool.openapi.HttpMethod;
import io.github.potjerodekool.openapi.RequestCycleLocation;
import io.github.potjerodekool.openapi.internal.ast.element.MethodElement;
import io.github.potjerodekool.openapi.internal.ast.element.VariableElement;
import io.github.potjerodekool.openapi.tree.OpenApiProperty;

public interface ModelAdapter {

    default void adaptConstructor(HttpMethod httpMethod,
                                  RequestCycleLocation requestCycleLocation,
                                  MethodElement constructor) {
    }

    default void adaptField(HttpMethod httpMethod,
                            RequestCycleLocation requestCycleLocation,
                            OpenApiProperty property,
                            VariableElement field) {
    }

    default void adaptGetter(HttpMethod httpMethod,
                             RequestCycleLocation requestCycleLocation,
                             OpenApiProperty property,
                             MethodElement method) {

    }

    default void adaptSetter(HttpMethod httpMethod,
                             RequestCycleLocation requestCycleLocation,
                             OpenApiProperty property,
                             MethodElement method) {
    }
}
