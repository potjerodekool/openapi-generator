package io.github.potjerodekool.openapi.tree;

import java.util.Map;

public record OpenApiResponse(String description,
                              Map<String, OpenApiContent> contentMediaType, Map<String, OpenApiHeader> headers) {

}
