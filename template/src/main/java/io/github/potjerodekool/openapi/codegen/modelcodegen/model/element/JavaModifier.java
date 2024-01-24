package io.github.potjerodekool.openapi.codegen.modelcodegen.model.element;

public enum JavaModifier implements Modifier {

    PUBLIC,
    PRIVATE,
    PROTECTED,
    STATIC,
    FINAL,
    ABSTRACT,
    NATIVE,
    SYNCHRONIZED,
    TRANSIENT,
    VOLATILE;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
