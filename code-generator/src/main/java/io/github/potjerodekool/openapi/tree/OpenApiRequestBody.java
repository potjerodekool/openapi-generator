package io.github.potjerodekool.openapi.tree;

import java.util.Map;

public record OpenApiRequestBody(String description,
                                 Map<String, OpenApiContent> contentMediaType,
                                 Boolean required) {

}
