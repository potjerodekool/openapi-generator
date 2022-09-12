package io.github.potjerodekool.openapi.internal.ast.element;

import io.github.potjerodekool.openapi.internal.ast.AstNode;
import io.github.potjerodekool.openapi.internal.ast.Modifier;
import io.github.potjerodekool.openapi.internal.ast.expression.AnnotationExpression;
import io.github.potjerodekool.openapi.internal.ast.expression.Expression;
import io.github.potjerodekool.openapi.internal.ast.type.Type;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Set;

public interface Element extends AstNode {

    ElementKind getKind();

    String getSimpleName();

    Type<?> asType();

    default @Nullable Element getEnclosingElement() {
        return null;
    }

    void setEnclosingElement(@Nullable Element enclosingElement);

    List<Element> getEnclosedElements();

    <R,P> R accept(ElementVisitor<R,P> visitor, P param);

    Set<Modifier> getModifiers();

    boolean hasModifier(Modifier modifier);

    List<AnnotationExpression> getAnnotations();

    void addAnnotation(String annotationClassName);

    void addAnnotation(String annotationClassName, Expression expression);

    void addAnnotation(AnnotationExpression annotation);

    void addAnnotations(List<AnnotationExpression> annotations);
}
