package io.github.potjerodekool.openapi.internal.ast.element;

import io.github.potjerodekool.openapi.internal.ast.Attribute;
import io.github.potjerodekool.openapi.internal.ast.type.Type;

import javax.lang.model.element.ElementKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class VariableElementBuilder {

    private final ElementKind kind;

    private String simpleName;

    private final Type<?> type;

    private final List<AnnotationMirror> annotations = new ArrayList<>();

    public VariableElementBuilder(final ElementKind kind,
                                  final String simpleName,
                                  final Type<?> type) {
        this.kind = kind;
        this.simpleName = simpleName;
        this.type = type;
    }

    public static VariableElementBuilder createParameter(final String simpleName,
                                                         final Type<?> type) {
        return new VariableElementBuilder(ElementKind.PARAMETER, simpleName, type);
    }

    public void addAnnotation(final String annotationClassName) {
        annotations.add(Attribute.compound(annotationClassName));
    }

    public void addAnnotation(final AnnotationMirror annotation) {
        this.annotations.add(annotation);
    }

    public void setSimpleName(final String simpleName) {
        this.simpleName = simpleName;
    }

    public VariableElement build() {
        return VariableElement.create(
                kind,
                type,
                simpleName,
                annotations,
                Set.of(),
                null
        );
    }
}
