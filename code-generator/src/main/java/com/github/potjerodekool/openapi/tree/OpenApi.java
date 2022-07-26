package com.github.potjerodekool.openapi.tree;

import java.util.List;

public record OpenApi(OpenApiInfo info, List<OpenApiPath> paths) {

}
