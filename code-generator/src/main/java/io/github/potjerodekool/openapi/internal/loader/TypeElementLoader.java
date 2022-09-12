package io.github.potjerodekool.openapi.internal.loader;

import io.github.potjerodekool.openapi.internal.ast.element.TypeElement;

public interface TypeElementLoader {
    TypeElement loadTypeElement(String className) throws ClassNotFoundException;
}
