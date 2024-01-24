package io.github.potjerodekool.openapi.codegen.modelcodegen.model.type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReferenceType implements Type {

    private String name;

    private List<Type> typeArgs;

    public String getName() {
        return name;
    }

    public ReferenceType name(final String name) {
        this.name = name;
        return this;
    }

    public List<Type> getTypeArgs() {
        return typeArgs;
    }

    public ReferenceType typeArgs(final List<Type> typeArgs) {
        if (this.typeArgs == null) {
            this.typeArgs = new ArrayList<>();
        }
        this.typeArgs.addAll(typeArgs);
        return this;
    }

    public ReferenceType typeArgs(final Type... typeArgs) {
        return typeArgs(Arrays.asList(typeArgs));
    }

    public ReferenceType typeArg(final Type typeArg) {
        if (this.typeArgs == null) {
            this.typeArgs = new ArrayList<>();
        }

        this.typeArgs.add(typeArg);
        return this;
    }
}
