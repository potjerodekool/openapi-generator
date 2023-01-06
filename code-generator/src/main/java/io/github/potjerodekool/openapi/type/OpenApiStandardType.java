package io.github.potjerodekool.openapi.type;

import org.checkerframework.checker.nullness.qual.Nullable;

public record OpenApiStandardType(OpenApiStandardTypeEnum typeEnum,
                                  @Nullable String format,
                                  Boolean nullable) implements OpenApiType {

    @Override
    public String name() {
        return typeEnum.getTypeName();
    }

    @Override
    public @Nullable String format() {
        return format;
    }

    @Override
    public Boolean nullable() {
        return nullable;
    }

    @Override
    public OpenApiTypeKind kind() {
        return OpenApiTypeKind.STANDARD;
    }

    @Override
    public OpenApiType toNonNullable() {
        return new OpenApiStandardType(
                typeEnum,
                format,
                false
        );
    }
}
