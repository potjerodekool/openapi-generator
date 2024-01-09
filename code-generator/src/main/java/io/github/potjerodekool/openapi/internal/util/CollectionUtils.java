package io.github.potjerodekool.openapi.internal.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CollectionUtils {

    private CollectionUtils() {
    }

    public static <T> List<T> nonNull(final List<T> list) {
        return list != null
                ? list
                : new ArrayList<>();
    }

    public static <K,V> Map<K,V> nonNull(final Map<K, V> map) {
        return map != null
                ? map
                : new HashMap<>();
    }
}
