package io.github.potjerodekool.openapi.internal.ast.element;

import io.github.potjerodekool.openapi.internal.ast.AstNode;
import io.github.potjerodekool.openapi.internal.ast.type.DeclaredType;

import java.util.Map;

public interface AnnotationMirror extends AstNode {

    DeclaredType getAnnotationType();

    default String getAnnotationClassName() {
        return getAnnotationType().getElement().getQualifiedName();
    }

    Map<? extends ExecutableElement, ? extends AnnotationValue> getElementValues();

    <R,P> R accept(AnnotationValueVisitor<R,P> visitor, P param);
}
