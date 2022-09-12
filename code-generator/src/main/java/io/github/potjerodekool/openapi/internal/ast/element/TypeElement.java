package io.github.potjerodekool.openapi.internal.ast.element;

import io.github.potjerodekool.openapi.internal.ast.Modifier;
import io.github.potjerodekool.openapi.internal.ast.expression.AnnotationExpression;
import io.github.potjerodekool.openapi.internal.ast.type.DeclaredType;
import io.github.potjerodekool.openapi.internal.ast.type.Type;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class TypeElement extends AbstractElement<TypeElement, DeclaredType> implements QualifiedNameable {

    public static final TypeElement ARRAY_ELEMENT = new TypeElement(
            ElementKind.CLASS,
            "array"
    );

    public static final TypeElement PRIMITIVE = new TypeElement(
            ElementKind.CLASS,
            "primitive"
    );

    public static final TypeElement WILDCARD = new TypeElement(
            ElementKind.CLASS,
            "wildcard"
    );

    private @Nullable Type<?> superType = null;

    private final List<Type<?>> interfaces = new ArrayList<>();

    private @Nullable MethodElement primaryConstructor;

    public TypeElement(final ElementKind kind,
                       final String simpleName) {
        super(kind, simpleName);
    }

    public static TypeElement create(final ElementKind kind,
                                     final List<AnnotationExpression> annotations,
                                     final Set<Modifier> modifiers,
                                     final String simpleName) {
        final var te = new TypeElement(kind, simpleName);
        te.addAnnotations(annotations);
        te.addModifiers(modifiers);
        return te;
    }

    public static TypeElement createClass(final String simpleName) {
        return create(ElementKind.CLASS, simpleName);
    }

    public static TypeElement createInterface(final String simpleName) {
        return create(ElementKind.INTERFACE, simpleName);
    }

    private static TypeElement create(final ElementKind kind,
                                      final String simpleName) {
        final var te = new TypeElement(kind, simpleName);
        final var type = new DeclaredType(te, false);
        te.setType(type);
        return te;
    }

    public @Nullable Type<?> getSuperType() {
        return superType;
    }

    public void setSuperType(final @Nullable Type<?> superType) {
        this.superType = superType;
    }

    public List<Type<?>> getInterfaces() {
        return Collections.unmodifiableList(interfaces);
    }

    public void addInterface(final Type<?> interfaceType) {
        this.interfaces.add(interfaceType);
    }

    public MethodElement addPrimaryConstructor() {
        final var pc = MethodElement.createConstructor(getSimpleName());
        this.primaryConstructor = pc;
        return pc;
    }

    public MethodElement addConstructor(final Modifier... modifiers) {
        final var constructor = MethodElement.createConstructor(getSimpleName());
        constructor.addModifiers(modifiers);
        addEnclosedElement(constructor);
        return constructor;
    }

    public MethodElement addMethod(final String name,
                                   final Modifier... modifiers) {
        final var constructor = MethodElement.createMethod(name);
        constructor.addModifiers(modifiers);
        addEnclosedElement(constructor);
        return constructor;
    }

    public void addPrimaryConstructor(final MethodElement primaryConstructor) {
        this.primaryConstructor = primaryConstructor;
    }

    public @Nullable MethodElement getPrimaryConstructor() {
        return primaryConstructor;
    }

    @Override
    public String getQualifiedName() {
        return QualifiedNameable.getQualifiedName(this);
    }

    @Override
    public <R, P> R accept(final ElementVisitor<R, P> elementVisitor, P param) {
        return elementVisitor.visitTypeElement(this, param);
    }

    public VariableElement addField(final Type<?> fieldType,
                         final String name,
                         final Modifier modifier) {
        final var field = VariableElement.createField(name, fieldType, null)
                .addModifier(modifier);
        addEnclosedElement(field);
        return field;
    }
}
