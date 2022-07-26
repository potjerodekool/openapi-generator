package com.github.potjerodekool.openapi.type;

import com.github.potjerodekool.openapi.tree.OpenApiProperty;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;

public class OpenApiStandardType implements OpenApiType {

    public static final OpenApiStandardType STRING_TYPE = new OpenApiStandardType("string", null, StandardTypeEnum.STRING);
    public static final OpenApiStandardType INTEGER_TYPE = new OpenApiStandardType("integer", "int32", StandardTypeEnum.INTEGER);
    public static final OpenApiStandardType LONG_TYPE = new OpenApiStandardType("integer", "int64", StandardTypeEnum.LONG);
    public static final OpenApiStandardType DATE_TYPE = new OpenApiStandardType("date", null, StandardTypeEnum.DATE);
    public static final OpenApiStandardType DATE_TIME_TYPE = new OpenApiStandardType("date-time", null, StandardTypeEnum.DATE_TIME);

    private final String name;
    private final @Nullable String format;
    private final StandardTypeEnum standardTypeEnum;

    public OpenApiStandardType(final String name,
                               final @Nullable String format,
                               final StandardTypeEnum standardTypeEnum) {
        this.name = name;
        this.format = format;
        this.standardTypeEnum = standardTypeEnum;
    }

    @Override
    public @Nullable String name() {
        return name;
    }

    @Override
    public @Nullable String format() {
        return format;
    }

    @Override
    public Map<String, OpenApiProperty> properties() {
        return Map.of();
    }

    @Override
    public @Nullable OpenApiProperty additionalProperties() {
        return null;
    }

    public boolean isString() {
        return this == STRING_TYPE;
    }

    public StandardTypeEnum getStandardTypeEnum() {
        return standardTypeEnum;
    }
}
