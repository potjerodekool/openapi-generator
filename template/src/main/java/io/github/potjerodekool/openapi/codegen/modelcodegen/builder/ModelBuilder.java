package io.github.potjerodekool.openapi.codegen.modelcodegen.builder;

import io.github.potjerodekool.openapi.codegen.HttpMethod;
import io.github.potjerodekool.openapi.codegen.modelcodegen.model.element.Model;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;

public interface ModelBuilder {
    Model build(OpenAPI openAPI,
                HttpMethod method,
                String name,
                Schema<?> schema);
}
