package io.github.potjerodekool.openapi.internal.ast.element;

import io.github.potjerodekool.openapi.internal.ast.Modifier;
import io.github.potjerodekool.openapi.internal.ast.type.DeclaredType;
import io.github.potjerodekool.openapi.internal.ast.type.TypeFactory;
import io.github.potjerodekool.openapi.internal.ast.type.Type;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.lang.model.element.ElementKind;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class TypeElement extends AbstractElement<TypeElement, Type<?>> implements QualifiedNameable {

    private @Nullable Type<?> superType = null;

    private final List<Type<?>> interfaces = new ArrayList<>();

    private @Nullable MethodElement primaryConstructor;

    protected TypeElement(final ElementKind kind,
                       final String simpleName) {
        super(kind, simpleName);

        if (simpleName.contains(".")) {
            throw new IllegalArgumentException();
        }
    }

    public static TypeElement create(final ElementKind kind,
                                     final List<AnnotationMirror> annotations,
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
        final var typeElement = new TypeElement(kind, simpleName);
        final var type = TypeFactory.createDeclaredType(typeElement, false);
        typeElement.setType(type);
        return typeElement;
    }

    @Override
    public void setType(final Type<?> type) {
        super.setType(type);
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

    public MethodElement addMethod(final String name,
                                   final Type<?> returnType,
                                   final Modifier... modifiers) {
        final var constructor = MethodElement.createMethod(name, returnType);
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
                                    final Modifier... modifiers) {
        final var field = VariableElement.createField(name, fieldType, null)
            .addModifiers(modifiers);
        addEnclosedElement(field);
        return field;
    }
}
