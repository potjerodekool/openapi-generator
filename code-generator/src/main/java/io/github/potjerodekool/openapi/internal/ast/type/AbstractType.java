package io.github.potjerodekool.openapi.internal.ast.type;

import io.github.potjerodekool.openapi.internal.ast.element.Element;
import io.github.potjerodekool.openapi.internal.ast.expression.AnnotationExpression;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("initialization.fields.uninitialized")
public abstract class AbstractType<E extends Element> implements Type<E> {

    private final E element;

    private final List<AnnotationExpression> annotations;

    protected AbstractType(final E element) {
        this(element, new ArrayList<>());
    }

    protected AbstractType(final E element,
                           final List<AnnotationExpression> annotations) {
        this.element = element;
        this.annotations = annotations;
    }

    @Override
    public E getElement() {
        return element;
    }

    @Override
    public List<AnnotationExpression> getAnnotations() {
        return annotations;
    }

    @Override
    public void addAnnotation(final AnnotationExpression annotation) {
        this.annotations.add(annotation);
    }

}
