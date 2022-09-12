package io.github.potjerodekool.openapi.internal.ast.element;

import io.github.potjerodekool.openapi.internal.ast.Modifier;
import io.github.potjerodekool.openapi.internal.ast.expression.AnnotationExpression;
import io.github.potjerodekool.openapi.internal.ast.expression.Expression;
import io.github.potjerodekool.openapi.internal.ast.type.Type;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

public abstract class AbstractElement<E extends AbstractElement<E, T>, T extends Type<?>> implements Element {

    private final ElementKind kind;
    private String simpleName;

    private T type;

    private @Nullable Element enclosingElement;

    private final List<Element> enclosedElements = new ArrayList<>();

    private final Set<Modifier> modifiers = new LinkedHashSet<>();

    private final List<AnnotationExpression> annotations = new ArrayList<>();

    @SuppressWarnings("initialization.fields.uninitialized")
    protected AbstractElement(final ElementKind kind,
                              final String simpleName) {
        this.kind = kind;
        this.simpleName = simpleName;
    }

    protected void setType(final T type) {
        this.type = type;
    }

    @Override
    public ElementKind getKind() {
        return kind;
    }

    @Override
    public String getSimpleName() {
        return simpleName;
    }

    public void setSimpleName(final String simpleName) {
        this.simpleName = simpleName;
    }

    @Override
    public T asType() {
        return type;
    }

    @Override
    public @Nullable Element getEnclosingElement() {
        return enclosingElement;
    }

    @Override
    public void setEnclosingElement(final @Nullable Element enclosingElement) {
        this.enclosingElement = enclosingElement;
    }

    @Override
    public List<Element> getEnclosedElements() {
        return Collections.unmodifiableList(this.enclosedElements);
    }

    public void addEnclosedElement(final Element enclosedElement) {
        Objects.requireNonNull(enclosedElement);
        this.enclosedElements.add(enclosedElement);
        enclosedElement.setEnclosingElement(this);
    }

    public void removeEnclosedElement(final Element enclosedElement) {
        if (this.enclosedElements.remove(enclosedElement)) {
            enclosedElement.setEnclosingElement(null);
        }
    }

    @Override
    public Set<Modifier> getModifiers() {
        return Collections.unmodifiableSet(modifiers);
    }

    public boolean isStatic() {
        return hasModifier(Modifier.FINAL);
    }

    public boolean isFinal() {
        return hasModifier(Modifier.FINAL);
    }

    public E addModifier(final Modifier modifier) {
        this.modifiers.add(modifier);
        return (E) this;
    }

    public E addModifiers(final Modifier... modifier) {
        this.modifiers.addAll(Arrays.asList(modifier));
        return (E) this;
    }

    public E addModifiers(final Set<Modifier> modifiers) {
        this.modifiers.addAll(modifiers);
        return (E) this;
    }

    public void removeModifier(final Modifier modifier) {
        this.modifiers.remove(modifier);
    }

    @Override
    public boolean hasModifier(final Modifier modifier) {
        return this.modifiers.contains(modifier);
    }

    @Override
    public List<AnnotationExpression> getAnnotations() {
        return Collections.unmodifiableList(this.annotations);
    }

    @Override
    public void addAnnotation(String annotationClassName) {
        addAnnotation(new AnnotationExpression(annotationClassName));
    }

    @Override
    public void addAnnotation(final String annotationClassName, final Expression expression) {
        final var annotation = new AnnotationExpression(annotationClassName);
        annotation.addElementValue("value", expression);
        addAnnotation(annotation);
    }

    @Override
    public void addAnnotation(final AnnotationExpression annotation) {
        this.annotations.add(annotation);
    }

    @Override
    public void addAnnotations(final List<AnnotationExpression> annotations) {
        this.annotations.addAll(annotations);
    }

}
