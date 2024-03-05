package io.github.potjerodekool.openapi.common.generate.api;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.openapi.common.ApiConfiguration;
import io.github.potjerodekool.openapi.common.OpenApiEnvironment;
import io.swagger.v3.oas.models.OpenAPI;

public interface CodeGenerator {
    void generateCommon(OpenApiEnvironment openApiEnvironment);

    void generateApi(OpenApiEnvironment openApiEnvironment,
                     OpenAPI openApi,
                     ApiConfiguration apiConfiguration,
                     boolean generateConfigs);
}
