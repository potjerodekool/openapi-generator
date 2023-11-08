package io.github.potjerodekool.openapi.internal.generate;

import io.github.potjerodekool.openapi.internal.StatusCodeMatcher;
import io.github.potjerodekool.openapi.tree.OpenApiContent;
import io.github.potjerodekool.openapi.tree.OpenApiOperation;
import io.github.potjerodekool.openapi.tree.OpenApiResponse;
import io.github.potjerodekool.openapi.tree.media.OpenApiSchema;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class OpenApiUtils {

    private OpenApiUtils() {
    }

    public static List<? extends OpenApiSchema<?>> resolveResponseTypes(final OpenApiOperation operation) {
        final Map<String, OpenApiResponse> responses = operation.responses();

        return responses.entrySet().stream()
                .filter(entry -> !"default".equals(entry.getKey()))
                .map(entry -> {
                    final var response = entry.getValue();
                    final var contentMediaType = resolveResponseMediaType(response.contentMediaType());
                    return Optional.ofNullable(contentMediaType);
                    })
                .filter(Optional::isPresent)
                .map(Optional::get)
                        .toList();
    }

    public static @Nullable OpenApiSchema<?> resolveResponseMediaType(final Map<String, OpenApiContent> contentMediaType) {
        final var jsonMediaType = findJsonMediaType(contentMediaType);

        if (jsonMediaType != null) {
            return jsonMediaType;
        } else {
            //Not returning json, maybe image/jpg or */*
            if (contentMediaType.size() == 1) {
                final var content = contentMediaType.values().iterator().next();
                if (content != null) {
                    return content.schema();
                }
            }
            return null;
        }
    }

    public static @Nullable OpenApiSchema<?> findJsonMediaType(final Map<String, OpenApiContent> contentMediaType) {
        final var content = contentMediaType.get(ContentTypes.JSON);
        return content != null ? content.schema() : null;
    }

    public static boolean isMultiPart(final Map<String, OpenApiContent> contentMediaType) {
        return contentMediaType.keySet().stream()
                .anyMatch(it -> it.startsWith("multipart/"));
    }

    public static boolean isImageOrVideo(final Map<String, OpenApiContent> contentMediaType) {
        return contentMediaType.keySet().stream()
                .anyMatch(it -> it.startsWith("image/")
                        || it.startsWith("video/")
                );
    }

    public static Optional<Map.Entry<String, OpenApiResponse>> findOkResponse(final Map<String, OpenApiResponse> responses) {
        return responses.entrySet().stream()
                .filter(entry -> StatusCodeMatcher.is2XX(entry.getKey()))
                .findFirst();
    }
}
