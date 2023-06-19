package io.github.potjerodekool.openapi.internal.generate;

import io.github.potjerodekool.codegen.model.tree.MethodDeclaration;
import io.github.potjerodekool.codegen.model.tree.Tree;
import io.github.potjerodekool.codegen.model.tree.statement.ClassDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.IfStatement;
import io.github.potjerodekool.codegen.model.tree.type.WildCardTypeExpression;
import io.github.potjerodekool.codegen.model.util.Elements;
import io.github.potjerodekool.codegen.model.util.type.Types;

public class FullResolver extends AbstractResolver {

    public FullResolver(final Elements elements,
                        final Types types) {
        super(elements, types);
    }

    public void resolve(final Tree tree) {
        tree.accept(this, null);
    }

    @Override
    public Object visitClassDeclaration(final ClassDeclaration classDeclaration, final Object param) {
        classDeclaration.getEnclosed().forEach(it -> it.accept(this, param));
        return null;
    }

    @Override
    public Object visitMethodDeclaration(final MethodDeclaration methodDeclaration, final Object param) {
        methodDeclaration.getTypeParameters().forEach(it -> it.accept(this, param));
        methodDeclaration.getAnnotations().forEach(it -> it.accept(this, param));
        methodDeclaration.getReturnType().accept(this, param);
        methodDeclaration.getBody().ifPresent(it -> it.accept(this, param));
        return null;
    }

    @Override
    public Object visitIfStatement(final IfStatement ifStatement, final Object param) {
        ifStatement.getCondition().accept(this, param);
        ifStatement.getBody().accept(this, param);
        return null;
    }

    @Override
    public Object visitWildCardTypeExpression(final WildCardTypeExpression wildCardTypeExpression, final Object param) {
        wildCardTypeExpression.getTypeExpression().accept(this, param);
        return null;
    }
}
