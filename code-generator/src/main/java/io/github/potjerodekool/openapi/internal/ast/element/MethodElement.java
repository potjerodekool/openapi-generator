package io.github.potjerodekool.openapi.internal.ast.element;

import io.github.potjerodekool.openapi.internal.ast.Modifier;
import io.github.potjerodekool.openapi.internal.ast.expression.AnnotationExpression;
import io.github.potjerodekool.openapi.internal.ast.statement.BlockStatement;
import io.github.potjerodekool.openapi.internal.ast.type.ExecutableType;
import io.github.potjerodekool.openapi.internal.ast.type.Type;
import io.github.potjerodekool.openapi.internal.ast.type.VoidType;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class MethodElement extends ExecutableElement {

    private Type<?> returnType = VoidType.INSTANCE;

    private ExecutableType type;

    private final List<VariableElement> parameters = new ArrayList<>();
    private @Nullable BlockStatement body;

    @SuppressWarnings("initialization.fields.uninitialized")
    public MethodElement(final ElementKind kind,
                         final String simpleName) {
        super(kind, simpleName);
    }

    public static MethodElement create(final ElementKind kind,
                                       final List<AnnotationExpression> annotations,
                                       final Set<Modifier> modifiers,
                                       final Type<?> returnType,
                                       final String simpleName,
                                       final List<VariableElement> parameters,
                                       final @Nullable BlockStatement body) {
        final var methodElement = new MethodElement(kind, simpleName);
        methodElement.addModifiers(modifiers);
        methodElement.setReturnType(returnType);
        methodElement.addAnnotations(annotations);
        parameters.forEach(methodElement::addParameter);
        methodElement.setBody(body);
        return methodElement;
    }

    public static MethodElement createConstructor(final String simpleName) {
        return createMethod(ElementKind.CONSTRUCTOR, simpleName);
    }

    public static MethodElement createMethod(final String simpleName) {
        return createMethod(ElementKind.METHOD, simpleName);
    }

    private static MethodElement createMethod(final ElementKind elementKind,
                                              final String simpleName) {
        final var me =  new MethodElement(elementKind, simpleName);
        final var type = new ExecutableType(me);
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

    public void setReturnType(final Type<?> returnType) {
        this.returnType = returnType;
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
}
