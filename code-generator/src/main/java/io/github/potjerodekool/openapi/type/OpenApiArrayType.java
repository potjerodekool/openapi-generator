package io.github.potjerodekool.openapi.type;

public record OpenApiArrayType(OpenApiType items) implements OpenApiType {

    @Override
    public String name() {
        return "array";
    }

    @Override
    public OpenApiTypeKind kind() {
        return OpenApiTypeKind.ARRAY;
    }

}
