package io.github.potjerodekool.openapi.tree;

import java.util.Map;

public record OpenApiSecurityRequirement(Map<String, OpenApiSecurityParameter> requirements) {
}
