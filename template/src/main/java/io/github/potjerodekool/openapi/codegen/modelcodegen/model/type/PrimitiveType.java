package io.github.potjerodekool.openapi.codegen.modelcodegen.model.type;

public class PrimitiveType implements Type {

    private String name;

    public String getName() {
        return name;
    }

    public PrimitiveType name(final String name) {
        this.name = name;
        return this;
    }
}
