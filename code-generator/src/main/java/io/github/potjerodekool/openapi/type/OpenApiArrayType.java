package io.github.potjerodekool.openapi.type;

public record OpenApiArrayType(OpenApiType items, Boolean nullable) implements OpenApiType {

    @Override
    public String name() {
        return "array";
    }

    @Override
    public OpenApiTypeKind kind() {
        return OpenApiTypeKind.ARRAY;
    }

    @Override
    public OpenApiType toNonNullable() {
        return new OpenApiArrayType(
                items,
                false
        );
    }
}
