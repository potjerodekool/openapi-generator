package io.github.potjerodekool.openapi.internal.ast.expression;

public interface ExpressionVisitor<R, P> {

    R visitUnknown(Expression expression,
                   P param);

    R visitBinaryExpression(BinaryExpression binaryExpression,
                            P param);

    R visitNameExpression(NameExpression nameExpression,
                          P param);

    R visitFieldAccessExpression(FieldAccessExpression fieldAccessExpression,
                                 P param);

    R visitLiteralExpression(LiteralExpression literalExpression,
                             P param);

    R visitMethodCall(MethodCallExpression methodCallExpression,
                      P param);

    R visitArrayInitializerExpression(ArrayInitializerExpression arrayInitializerExpression,
                                      P param);

    R visitNamedMethodArgumentExpression(NamedMethodArgumentExpression namedMethodArgumentExpression,
                                         P param);

    R visitArrayAccessExpression(ArrayAccessExpression arrayAccessExpression,
                                 P param);

    R visitVariableDeclarationExpression(VariableDeclarationExpression variableDeclarationExpression,
                                         P param);

    default R visitErrorExpression(final ErrorExpression errorExpression,
                                   final P param) {
        return visitUnknown(errorExpression, param);
    }

    R visitNewClassExpression(NewClassExpression newClassExpression, P param);
}
