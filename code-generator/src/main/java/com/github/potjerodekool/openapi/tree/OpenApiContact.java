package com.github.potjerodekool.openapi.tree;

import java.util.Map;

public record OpenApiContact(String name,
                             String url,
                             String email,
                             Map<String, Object> extensions) {

}
