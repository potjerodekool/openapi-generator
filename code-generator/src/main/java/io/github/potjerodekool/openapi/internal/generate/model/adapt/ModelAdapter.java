package io.github.potjerodekool.openapi.internal.generate.model.adapt;

import io.github.potjerodekool.codegen.model.CompilationUnit;
import io.swagger.models.HttpMethod;
import io.swagger.v3.oas.models.media.ObjectSchema;

public interface ModelAdapter {
    void adapt(HttpMethod method, ObjectSchema schema, final CompilationUnit unit);
}
