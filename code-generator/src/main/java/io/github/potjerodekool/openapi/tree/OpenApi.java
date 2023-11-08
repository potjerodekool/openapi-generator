package io.github.potjerodekool.openapi.tree;

import java.util.List;
import java.util.Map;

public record OpenApi(String version,
                      OpenApiInfo info,
                      List<OpenApiServer> servers,
                      List<OpenApiPath> paths,
                      Map<String, OpenApiSecurityScheme> securitySchemas,
                      List<OpenApiSecurityRequirement> securityRequirements,
                      OpenApiComponents components) {

}
