package io.github.potjerodekool.openapi.type;

public record OpenApiArrayType(OpenApiType items) implements OpenApiType {

    @Override
    public OpenApiTypeKind kind() {
        return OpenApiTypeKind.ARRAY;
    }
}
