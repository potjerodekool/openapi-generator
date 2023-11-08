package io.github.potjerodekool.openapi.tree;

import java.util.List;
import java.util.Map;

public record OpenApiSecurityRequirement(Map<String, List<String>> requirements) {
}
