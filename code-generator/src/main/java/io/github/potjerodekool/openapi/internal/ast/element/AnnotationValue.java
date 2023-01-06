package io.github.potjerodekool.openapi.internal.ast.element;


public interface AnnotationValue {

    Object getValue();

    <R, P> R accept(AnnotationValueVisitor<R, P> visitor, P param);
}
