package io.github.potjerodekool.openapi.tree;

import java.util.List;

public record OpenApi(OpenApiInfo info,
                      List<OpenApiPath> paths,
                      java.util.Map<String, OpenApiSecurityScheme> securitySchemas,
                      List<OpenApiSecurityRequirement> securityRequirements) {

}
