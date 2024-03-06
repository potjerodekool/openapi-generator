package io.github.potjerodekool.openapi.common.generate.model.type;

public class TypeVariable implements Type {

    private String name;
    private Type bounds;

    public TypeVariable name(final String name) {
        this.name = name;
        return this;
    }

    public String getName() {
        return name;
    }

    public TypeVariable bounds(final Type bounds) {
        this.bounds = bounds;
        return this;
    }

    public Type getBounds() {
        return bounds;
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.TYPEVAR;
    }
}
