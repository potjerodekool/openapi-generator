package io.github.potjerodekool.openapi.generate.config;

import io.swagger.v3.oas.models.OpenAPI;

public interface ApiConfigGenerator {

    void generate(final OpenAPI openAPI);
}
