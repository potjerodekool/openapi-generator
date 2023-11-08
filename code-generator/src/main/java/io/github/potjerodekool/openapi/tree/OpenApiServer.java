package io.github.potjerodekool.openapi.tree;

import java.util.Map;

public record OpenApiServer(String url,
                            String description,
                            Map<String, Object> variables,
                            Map<String, Object> extensions) {
}
