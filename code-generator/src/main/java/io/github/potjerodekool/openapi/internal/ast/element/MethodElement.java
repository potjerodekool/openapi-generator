package io.github.potjerodekool.openapi.internal.ast.element;

import io.github.potjerodekool.openapi.internal.ast.Modifier;
import io.github.potjerodekool.openapi.internal.ast.statement.BlockStatement;
import io.github.potjerodekool.openapi.internal.ast.type.ExecutableType;
import io.github.potjerodekool.openapi.internal.ast.type.Type;
import io.github.potjerodekool.openapi.internal.ast.type.java.JavaExecutableType;
import io.github.potjerodekool.openapi.internal.ast.type.java.VoidType;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.lang.model.element.ElementKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class MethodElement extends AbstractElement<MethodElement, ExecutableType> implements ExecutableElement {

    private Type<?> returnType;

    private ExecutableType type;

    private final List<VariableElement> parameters = new ArrayList<>();
    private @Nullable BlockStatement body;

    private @Nullable AnnotationValue defaultValue = null;

    @SuppressWarnings("initialization.fields.uninitialized")
    private MethodElement(final ElementKind kind,
                          final Type<?> returnType,
                          final String simpleName) {
        super(kind, simpleName);
        this.returnType = returnType;
    }

    public static MethodElement create(final ElementKind kind,
                                       final List<AnnotationMirror> annotations,
                                       final Set<Modifier> modifiers,
                                       final Type<?> returnType,
                                       final String simpleName,
                                       final List<VariableElement> parameters,
                                       final @Nullable BlockStatement body) {
        final var methodElement = new MethodElement(kind, returnType, simpleName);
        methodElement.addModifiers(modifiers);
                methodElement.addAnnotations(annotations);
        parameters.forEach(methodElement::addParameter);
        methodElement.setBody(body);
        return methodElement;
    }

    public static MethodElement createConstructor(final String simpleName) {
        return createMethod(ElementKind.CONSTRUCTOR, VoidType.INSTANCE, simpleName);
    }

    public static MethodElement createMethod(final String simpleName) {
        return createMethod(ElementKind.METHOD, VoidType.INSTANCE, simpleName);
    }

    public static MethodElement createMethod(final String simpleName,
                                             final Type<?> returnType) {
        return createMethod(ElementKind.METHOD, returnType, simpleName);
    }

    private static MethodElement createMethod(final ElementKind elementKind,
                                              final Type<?> returnType,
                                              final String simpleName) {
        final var me =  new MethodElement(elementKind, returnType, simpleName);
        final var type = new JavaExecutableType(me);
        me.setType(type);
        return me;
    }

    @Override
    public ExecutableType asType() {
        return type;
    }

    public void setType(final ExecutableType type) {
        this.type = type;
    }

    public Type<?> getReturnType() {
        return returnType;
    }

    public List<VariableElement> getParameters() {
        return parameters;
    }

    public void addParameter(final VariableElement parameter) {
        this.parameters.add(parameter);
    }

    @Override
    public <R, P> R accept(final ElementVisitor<R, P> elementVisitor, final P param) {
        return elementVisitor.visitExecutableElement(this, param);
    }

    public Optional<BlockStatement> getBody() {
        return Optional.ofNullable(body);
    }

    public void setBody(final @Nullable BlockStatement body) {
        this.body = body;
    }

    public void setReturnType(final Type<?> returnType) {
        this.returnType = returnType;
    }

    @Override
    public @Nullable AnnotationValue getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(final AnnotationValue defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public String toString() {
        return String.format("%s %s()", returnType, getSimpleName());
    }
}
