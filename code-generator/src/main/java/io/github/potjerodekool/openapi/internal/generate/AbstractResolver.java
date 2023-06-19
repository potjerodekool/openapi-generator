package io.github.potjerodekool.openapi.internal.generate;

import io.github.potjerodekool.codegen.model.element.AnnotationMirror;
import io.github.potjerodekool.codegen.model.element.Name;
import io.github.potjerodekool.codegen.model.element.TypeElement;
import io.github.potjerodekool.codegen.model.symbol.VariableSymbol;
import io.github.potjerodekool.codegen.model.tree.AnnotationExpression;
import io.github.potjerodekool.codegen.model.tree.Tree;
import io.github.potjerodekool.codegen.model.tree.TreeVisitor;
import io.github.potjerodekool.codegen.model.tree.expression.*;
import io.github.potjerodekool.codegen.model.tree.statement.BlockStatement;
import io.github.potjerodekool.codegen.model.tree.statement.ExpressionStatement;
import io.github.potjerodekool.codegen.model.tree.statement.ReturnStatement;
import io.github.potjerodekool.codegen.model.tree.statement.VariableDeclaration;
import io.github.potjerodekool.codegen.model.tree.type.AnnotatedTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.NoTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.ParameterizedType;
import io.github.potjerodekool.codegen.model.tree.type.PrimitiveTypeExpression;
import io.github.potjerodekool.codegen.model.type.ClassType;
import io.github.potjerodekool.codegen.model.type.DeclaredType;
import io.github.potjerodekool.codegen.model.type.TypeKind;
import io.github.potjerodekool.codegen.model.type.TypeMirror;
import io.github.potjerodekool.codegen.model.util.Elements;
import io.github.potjerodekool.codegen.model.util.type.Types;

import java.util.List;

public abstract class AbstractResolver implements TreeVisitor<Object, Object> {

    protected final Elements elements;
    protected final Types types;

    public AbstractResolver(final Elements elements,
                            final Types types) {
        this.elements = elements;
        this.types = types;
    }

    @Override
    public Object visitVariableDeclaration(final VariableDeclaration variableDeclaration, final Object param) {
        variableDeclaration.getVarType().accept(this, param);
        variableDeclaration.getInitExpression().ifPresent(it -> it.accept(this, param));

        variableDeclaration.getAnnotations().forEach(annotationExpression -> annotationExpression.accept(this, param));

        final var variableSymbol = VariableSymbol.create(
                variableDeclaration.getKind(),
                variableDeclaration.getVarType().getType(),
                Name.of(variableDeclaration.getName()),
                List.of(),
                variableDeclaration.getModifiers()
        );

        variableDeclaration.setSymbol(variableSymbol);
        return null;
    }

    @Override
    public Object visitMethodCall(final MethodCallExpression methodCallExpression, final Object param) {
        methodCallExpression.getTarget().ifPresent(it -> it.accept(this, param));
        methodCallExpression.getArguments().forEach(it -> it.accept(this, param));
        return null;
    }

    @Override
    public Object visitBinaryExpression(final BinaryExpression binaryExpression, final Object param) {
        binaryExpression.getLeft().accept(this, param);
        binaryExpression.getRight().accept(this, param);
        return null;
    }

    @Override
    public Object visitExpressionStatement(final ExpressionStatement expressionStatement, final Object param) {
        expressionStatement.getExpression().accept(this, param);
        return null;
    }

    @Override
    public Object visitBlockStatement(final BlockStatement blockStatement, final Object param) {
        blockStatement.getStatements().forEach(it -> it.accept(this, param));
        return null;
    }

    @Override
    public Object visitFieldAccessExpression(final FieldAccessExpression fieldAccessExpression, final Object param) {
        fieldAccessExpression.getScope().accept(this, param);
        fieldAccessExpression.getField().accept(this, param);
        return null;
    }

    @Override
    public Object visitReturnStatement(final ReturnStatement returnStatement, final Object param) {
        returnStatement.getExpression().accept(this, param);
        return null;
    }

    @Override
    public Object visitParameterizedType(final ParameterizedType parameterizedType, final Object param) {
        parameterizedType.getArguments().forEach(arg -> arg.accept(this, param));
        parameterizedType.getClazz().accept(this, param);

        final var type = (DeclaredType) parameterizedType.getClazz().getType();
        final var argumentTypes = parameterizedType.getArguments().stream()
                .map(Tree::getType)
                .toArray(TypeMirror[]::new);

        final var declaredType = types.getDeclaredType(
                (TypeElement) type.asElement(),
                argumentTypes
        );

        parameterizedType.setType(declaredType);
        return null;
    }

    @Override
    public Object visitAnnotatedType(final AnnotatedTypeExpression annotatedTypeExpression, final Object param) {
        annotatedTypeExpression.getIdentifier().accept(this, param);
        annotatedTypeExpression.setType(annotatedTypeExpression.getIdentifier().getType());
        return null;
    }

    @Override
    public Object visitNameExpression(final NameExpression nameExpression, final Object param) {
        final String name = nameExpression.getName();
        final var typeElement = elements.getTypeElement(name);

        if (typeElement != null) {
            nameExpression.setType(typeElement.asType());
        }

        return null;
    }

    @Override
    public Object visitNoType(final NoTypeExpression noTypeExpression, final Object param) {
        final var type = types.getNoType(noTypeExpression.getKind());
        noTypeExpression.setType(type);
        return null;
    }

    @Override
    public Object visitPrimitiveTypeExpression(final PrimitiveTypeExpression primitiveTypeExpression, final Object param) {
        final var type = types.getPrimitiveType(primitiveTypeExpression.getKind());
        primitiveTypeExpression.setType(type);
        return null;
    }

    @Override
    public Object visitAnnotationExpression(final AnnotationExpression annotationExpression, final Object param) {
        annotationExpression.getAnnotationType().accept(this, param);
        annotationExpression.setType(annotationExpression.getAnnotationType().getType());
        annotationExpression.getArguments().values().forEach(value -> value.accept(this, param));
        return null;
    }

    @Override
    public Object visitLiteralExpression(final LiteralExpression literalExpression, final Object param) {
        final var type = switch (literalExpression.getLiteralType()) {
            case NULL -> types.getNullType();
            case BOOLEAN -> types.getPrimitiveType(TypeKind.BOOLEAN);
            case BYTE -> types.getPrimitiveType(TypeKind.BYTE);
            case SHORT -> types.getPrimitiveType(TypeKind.SHORT);
            case INT -> types.getPrimitiveType(TypeKind.INT);
            case LONG -> types.getPrimitiveType(TypeKind.LONG);
            case FLOAT -> types.getPrimitiveType(TypeKind.FLOAT);
            case DOUBLE -> types.getPrimitiveType(TypeKind.DOUBLE);
            case STRING -> elements.getTypeElement("java.lang.String").asType();
            case CLASS -> elements.getTypeElement("java.lang.Class").asType();
            case CHAR -> types.getPrimitiveType(TypeKind.CHAR);
        };
        literalExpression.setType(type);
        return null;
    }

    @Override
    public Object visitArrayInitializerExpression(final ArrayInitializerExpression arrayInitializerExpression, final Object param) {
        arrayInitializerExpression.getValues().forEach(it -> it.accept(this, param));
        return null;
    }

}
