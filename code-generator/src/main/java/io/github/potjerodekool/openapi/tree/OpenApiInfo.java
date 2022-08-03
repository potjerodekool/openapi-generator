package io.github.potjerodekool.openapi.tree;

import java.util.Map;

public record OpenApiInfo(String title,
                          String description,
                          String termsOfService,
                          OpenApiContact contact,
                          OpenApiLicense license,
                          String version,
                          Map<String, Object> extensions) {
}
