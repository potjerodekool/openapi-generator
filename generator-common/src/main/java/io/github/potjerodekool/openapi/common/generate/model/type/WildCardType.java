package io.github.potjerodekool.openapi.common.generate.model.type;

public class WildCardType implements Type {

    private Type extendsBound;

    public Type getExtendsBound() {
        return extendsBound;
    }

    public WildCardType extendsBound(final Type extendsBound) {
        this.extendsBound = extendsBound;
        return this;
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.WILDCARD;
    }
}
