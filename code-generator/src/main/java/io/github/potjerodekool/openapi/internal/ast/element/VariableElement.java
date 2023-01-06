package io.github.potjerodekool.openapi.internal.ast.element;

import io.github.potjerodekool.openapi.internal.ast.Modifier;
import io.github.potjerodekool.openapi.internal.ast.expression.Expression;
import io.github.potjerodekool.openapi.internal.ast.type.Type;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.lang.model.element.ElementKind;
import java.util.List;
import java.util.Set;

public class VariableElement extends AbstractElement<VariableElement, Type<?>> {

    private @Nullable Expression initExpression;

    private VariableElement(final ElementKind kind,
                           final String simpleName,
                           final @Nullable Expression initExpression) {
        super(kind, simpleName);
        this.initExpression = initExpression;
    }


    public static VariableElement create(final ElementKind kind,
                                         final Type<?> type,
                                         final String simpleName,
                                         final List<AnnotationMirror> annotations,
                                         final Set<Modifier> modifiers,
                                         final @Nullable Expression initExpression) {
        final var variableElement = new VariableElement(kind, simpleName, initExpression);
        variableElement.setType(type);
        variableElement.addAnnotations(annotations);
        variableElement.addModifiers(modifiers);
        return variableElement;
    }

    public static VariableElement createParameter(final String simpleName,
                                                  final Type<?> type) {
        final var ve = new VariableElement(
                ElementKind.PARAMETER,
                simpleName,
                null
        );
        ve.setType(type);
        return ve;
    }

    public static VariableElement createField(final String simpleName,
                                              final Type<?> type) {
        return createField(simpleName, type, null);
    }

    public static VariableElement createField(final String simpleName,
                                              final Type<?> type,
                                              final @Nullable Expression initExpression) {
        final var ve = new VariableElement(
                ElementKind.FIELD,
                simpleName,
                initExpression
        );
        ve.setType(type);
        return ve;
    }

    public @Nullable Expression getInitExpression() {
        return initExpression;
    }

    public void setInitExpression(final @Nullable Expression initExpression) {
        this.initExpression = initExpression;
    }

    @Override
    public <R, P> R accept(final ElementVisitor<R, P> elementVisitor, final P param) {
        return elementVisitor.visitVariableElement(this, param);
    }

    public Type<?> getType() {
        return asType();
    }
}
