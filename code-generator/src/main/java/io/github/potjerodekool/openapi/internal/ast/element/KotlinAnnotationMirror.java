package io.github.potjerodekool.openapi.internal.ast.element;

import io.github.potjerodekool.openapi.internal.ast.type.DeclaredType;
import io.github.potjerodekool.openapi.internal.ast.type.TypeFactory;

import javax.lang.model.element.ElementKind;
import java.util.HashMap;
import java.util.Map;

public class KotlinAnnotationMirror implements AnnotationMirror {

    private final String prefix;
    private final DeclaredType annotationType;
    private final Map<ExecutableElement, AnnotationValue> elementValues = new HashMap<>();

    public KotlinAnnotationMirror(final String prefix,
                                  final DeclaredType annotationType) {
        this.prefix = prefix;
        this.annotationType = annotationType;
    }

    public KotlinAnnotationMirror(final String prefix,
                                  final String className,
                                  final Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues) {
        this.prefix = prefix;
        this.annotationType = TypeFactory.createDeclaredType(ElementKind.ANNOTATION_TYPE, className);
        this.elementValues.putAll(elementValues);
    }


    public String getPrefix() {
        return prefix;
    }

    @Override
    public DeclaredType getAnnotationType() {
        return annotationType;
    }

    @Override
    public Map<? extends ExecutableElement, ? extends AnnotationValue> getElementValues() {
        return elementValues;
    }

    @Override
    public <R, P> R accept(final AnnotationValueVisitor<R, P> visitor, final P param) {
        return visitor.visitAnnotation(this, param);
    }
}
