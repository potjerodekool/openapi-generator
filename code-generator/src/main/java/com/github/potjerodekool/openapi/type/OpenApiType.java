package com.github.potjerodekool.openapi.type;

import com.github.potjerodekool.openapi.tree.OpenApiProperty;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;

public interface OpenApiType {

    @Nullable String name();

    @Nullable String format();

    Map<String, OpenApiProperty> properties();

    @Nullable OpenApiProperty additionalProperties();
}
