package io.github.potjerodekool.openapi.common.generate;

import java.util.List;
import java.util.Map;

public final class ExtensionsHelper {

    private ExtensionsHelper() {
    }

    public static <T> T getExtension(final Map<String, Object> extensions, final String name, final Class<T> resultClass) {
        if (extensions == null || extensions.isEmpty()) {
            return getDefaultValue(resultClass);
        }

        final var extension = extensions.get(name);

        if (extension == null) {
            return getDefaultValue(resultClass);
        }

        return (T) extension;
    }

    private static <T> T getDefaultValue(final Class<T> resultClass) {
        if (resultClass == List.class) {
            return (T) List.of();
        } else {
            return null;
        }
    }
}
