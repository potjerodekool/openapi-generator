package io.github.potjerodekool.openapi.internal.ast.element;

import org.checkerframework.checker.nullness.qual.Nullable;

public interface ExecutableElement extends Element {

    @Nullable AnnotationValue getDefaultValue();

}
