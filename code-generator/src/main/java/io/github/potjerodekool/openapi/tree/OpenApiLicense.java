package io.github.potjerodekool.openapi.tree;

import java.util.Map;

public record OpenApiLicense(String name,
                             String identifier,
                             String url,
                             Map<String, Object> extensions) {

}
