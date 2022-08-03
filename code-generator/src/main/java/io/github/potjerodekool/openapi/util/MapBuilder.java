package io.github.potjerodekool.openapi.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MapBuilder<K,V> {

    private final List<Map.Entry<K,V>> entries = new ArrayList<>();

    public MapBuilder<K,V> entry(final K key,
                                 final V value) {
        if (value != null) {
            entries.add(Map.entry(key, value));
        }
        return this;
    }

    public Map<K,V> build() {
        return entries.stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }

}

