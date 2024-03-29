package io.github.potjerodekool.openapi.common.generate.model.element;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractElement<E extends Element> implements Element {

    private String simpleName;

    private Element enclosingElement;

    private final List<Element> enclosedElements = new ArrayList<>();

    private final Set<Modifier> modifiers = new LinkedHashSet<>();

    private List<Annotation> annotations;

    @Override
    public String getSimpleName() {
        return simpleName;
    }

    public E simpleName(final String simpleName) {
        this.simpleName = simpleName;
        return self();
    }

    @Override
    public String getQualifiedName() {
        if (enclosingElement != null) {
            return enclosingElement.getQualifiedName() + "." + simpleName;
        } else {
            return simpleName;
        }
    }

    @Override
    public Element getEnclosedElement() {
        return enclosingElement;
    }

    public E enclosingElement(final Element enclosingElement) {
        this.enclosingElement = enclosingElement;
        return self();
    }

    @Override
    public List<Element> getEnclosedElements() {
        return enclosedElements;
    }

    public E enclosedElement(final Element element) {
        enclosedElements.add(element);
        return self();
    }

    @Override
    public Set<Modifier> getModifiers() {
        return modifiers;
    }

    public E modifier(final Modifier modifier) {
        modifiers.add(modifier);
        return self();
    }

    public E modifiers(final Modifier... modifiers) {
        //add is used here to preserve order, addAll doesn't preserve order.
        for (final Modifier modifier : modifiers) {
            this.modifiers.add(modifier);
        }
        return self();
    }

    public List<Annotation> getFieldAnnotations() {
        return annotations.stream()
                .filter(annotation -> annotation.getAnnotationTarget() == AnnotationTarget.FIELD)
                .toList();
    }

    public List<Annotation> getAnnotations() {
        return annotations;
    }

    public E annotations(final List<Annotation> annotations) {
        this.annotations = annotations;
        return self();
    }

    public E annotation(final Annotation annotation) {
        if (annotations == null) {
            annotations = new ArrayList<>();
        }
        annotations.add(annotation);
        return self();
    }

    protected abstract E self();
}
