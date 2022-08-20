package io.github.potjerodekool.openapi.generate.model;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import io.github.potjerodekool.openapi.HttpMethod;
import io.github.potjerodekool.openapi.RequestCycleLocation;
import io.github.potjerodekool.openapi.tree.OpenApiProperty;

public interface ModelAdapter {

    default void adaptField(HttpMethod httpMethod,
                            RequestCycleLocation requestCycleLocation,
                            OpenApiProperty property,
                            FieldDeclaration fieldDeclaration) {
    }

    default void adaptGetter(HttpMethod httpMethod,
                             RequestCycleLocation requestCycleLocation,
                             OpenApiProperty property,
                             MethodDeclaration methodDeclaration) {
    }

    default void adaptSetter(HttpMethod httpMethod,
                            RequestCycleLocation requestCycleLocation,
                             OpenApiProperty property,
                             MethodDeclaration methodDeclaration) {
    }
}
