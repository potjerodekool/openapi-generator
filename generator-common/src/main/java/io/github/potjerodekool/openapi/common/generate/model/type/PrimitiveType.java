package io.github.potjerodekool.openapi.common.generate.model.type;

public class PrimitiveType implements Type {

    private String name;

    public String getName() {
        return name;
    }

    public PrimitiveType name(final String name) {
        this.name = name;
        return this;
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.PRIMITIVE;
    }
}
