package io.github.potjerodekool.openapi.common.util;

import io.github.potjerodekool.openapi.common.StatusCodeMatcher;
import io.github.potjerodekool.openapi.common.generate.ContentTypes;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class OpenApiUtils {

    private static final String SCHEMAS_PREFIX = "#/components/schemas/";

    private OpenApiUtils() {
    }

    public static Schema<?> findComponentSchemaByName(final OpenAPI openAPI,
                                                      final String name) {
        final String schemaName;

        if (name.startsWith(SCHEMAS_PREFIX)) {
            schemaName = name.substring(SCHEMAS_PREFIX.length());
        } else {
            schemaName = name;
        }

        final var schemas = getComponentSchemas(openAPI);
        return schemas.get(schemaName);
    }

    public static String getSchemaName(final String ref) {
        if (ref.startsWith(SCHEMAS_PREFIX)) {
            return ref.substring(SCHEMAS_PREFIX.length());
        }
        return null;
    }

    public static Map<String, Schema> getComponentSchemas(final OpenAPI openAPI) {
        final var components = openAPI.getComponents();

        if (components == null) {
            return Map.of();
        }

        final var schemas = components.getSchemas();
        return schemas != null
                ? schemas
                : Map.of();
    }

    public static List<? extends Schema<?>> resolveResponseTypes(final Operation operation) {
        if (operation.getResponses() == null) {
            return List.of();
        }

        final var responses = operation.getResponses();

        return responses.entrySet().stream()
                .filter(entry -> !"default".equals(entry.getKey()))
                .map(entry -> {
                    final var response = entry.getValue();

                    final var contentMediaType = resolveResponseMediaType(response.getContent());
                    return Optional.ofNullable(contentMediaType);
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    public static @Nullable Schema<?> resolveResponseMediaType(final Content contentMediaType) {
        final var jsonMediaType = findJsonMediaType(contentMediaType);

        if (jsonMediaType != null) {
            return jsonMediaType;
        } else {
            //Not returning json, maybe image/jpg or */*
            if (contentMediaType != null && contentMediaType.size() == 1) {
                final var content = contentMediaType.values().iterator().next();
                if (content != null) {
                    return content.getSchema();
                }
            }
            return null;
        }
    }

    public static @Nullable Schema<?> findJsonMediaType(final Content contentMediaType) {
        if (contentMediaType == null) {
            return null;
        } else {
            final var content = contentMediaType.get(ContentTypes.JSON);
            return content != null ? content.getSchema() : null;
        }
    }

    public static boolean isMultiPart(final Content contentMediaType) {
        return contentMediaType.keySet().stream()
                .anyMatch(it -> it.startsWith("multipart/"));
    }

    public static boolean isImageOrVideo(final Content contentMediaType) {
        return contentMediaType.keySet().stream()
                .anyMatch(it -> it.startsWith("image/")
                        || it.startsWith("video/")
                );
    }

    public static Optional<Map.Entry<String, ApiResponse>> findOkResponse(final ApiResponses responses) {
        return responses != null
                ? responses.entrySet().stream()
                    .filter(entry -> StatusCodeMatcher.is2XX(entry.getKey()))
                    .findFirst()
                : Optional.empty();
    }

}