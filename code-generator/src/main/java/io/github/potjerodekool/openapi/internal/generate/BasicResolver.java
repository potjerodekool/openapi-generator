package io.github.potjerodekool.openapi.internal.generate;

import io.github.potjerodekool.codegen.model.element.*;
import io.github.potjerodekool.codegen.model.symbol.ClassSymbol;
import io.github.potjerodekool.codegen.model.symbol.MethodSymbol;
import io.github.potjerodekool.codegen.model.symbol.VariableSymbol;
import io.github.potjerodekool.codegen.model.tree.MethodDeclaration;
import io.github.potjerodekool.codegen.model.tree.PackageDeclaration;
import io.github.potjerodekool.codegen.model.tree.Tree;
import io.github.potjerodekool.codegen.model.tree.statement.ClassDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.VariableDeclaration;
import io.github.potjerodekool.codegen.model.tree.type.BoundKind;
import io.github.potjerodekool.codegen.model.tree.type.WildCardTypeExpression;
import io.github.potjerodekool.codegen.model.type.ClassType;
import io.github.potjerodekool.codegen.model.type.TypeMirror;
import io.github.potjerodekool.codegen.model.util.Elements;
import io.github.potjerodekool.codegen.model.util.SymbolTable;
import io.github.potjerodekool.codegen.model.util.type.Types;

import java.util.List;

public class BasicResolver extends AbstractResolver {
    private final SymbolTable symbolTable;

    public BasicResolver(final Elements elements,
                         final Types types,
                         final SymbolTable symbolTable) {
        super(elements, types);
        this.symbolTable = symbolTable;
    }

    public void resolve(final Tree tree) {
        tree.accept(this, null);
    }

    @Override
    public Object visitPackageDeclaration(final PackageDeclaration packageDeclaration, final Object param) {
        return null;
    }

    @Override
    public Object visitClassDeclaration(final ClassDeclaration classDeclaration, final Object param) {
        var enclosing = classDeclaration.getEnclosing();
        final Element enclosingElement;
        final NestingKind nestingKind;

        if (enclosing == null) {
            enclosingElement = symbolTable.findOrCreatePackageSymbol(Name.of(""));
            nestingKind = NestingKind.TOP_LEVEL;
        } else if (enclosing instanceof PackageDeclaration packageDeclaration) {
            enclosingElement = symbolTable.findOrCreatePackageSymbol(Name.of(packageDeclaration.getName().getName()));
            nestingKind = NestingKind.TOP_LEVEL;
        } else {
            enclosingElement = ((ClassDeclaration)enclosing).getClassSymbol();
            nestingKind = NestingKind.MEMBER;
        }

        final var classSymbol = ClassSymbol.create(
                ElementKind.CLASS,
                classDeclaration.getSimpleName(),
                nestingKind,
                enclosingElement);

        symbolTable.addClass(classSymbol);

        classDeclaration.setClassSymbol(classSymbol);

        classDeclaration.getEnclosed().forEach(enclosed -> enclosed.accept(this, param));
        return null;
    }

    @Override
    public Object visitMethodDeclaration(final MethodDeclaration methodDeclaration, final Object param) {
        methodDeclaration.getReturnType().accept(this, param);
        final var returnType = methodDeclaration.getReturnType().getType();
        //methodDeclaration.getMethodSymbol().setReturnType(returnType);

        methodDeclaration.getTypeParameters().forEach(typeParam -> typeParam.accept(this, param));
        methodDeclaration.getAnnotations().forEach(annotation -> annotation.accept(this, param));
        methodDeclaration.getParameters().forEach(parameter -> parameter.accept(this, param));

        final var annotations = methodDeclaration.getAnnotations().stream()
                .map(annotation -> (ClassType) annotation.getType())
                .toList();

        final var parameters = methodDeclaration.getParameters().stream()
                .map(parameter ->  (VariableSymbol) parameter.getSymbol())
                .toList();

        final var methodSymbol = MethodSymbol.create(
                methodDeclaration.getKind(),
                List.of(),
                methodDeclaration.getModifiers(),
                returnType,
                methodDeclaration.getSimpleName().toString(),
                parameters,
                null
        );

        methodDeclaration.setMethodSymbol(methodSymbol);

        return null;
    }
    @Override
    public Object visitVariableDeclaration(final VariableDeclaration variableDeclaration, final Object param) {
        variableDeclaration.getVarType().accept(this, param);
        variableDeclaration.getAnnotations().forEach(annotation -> annotation.accept(this, param));

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
    public Object visitWildCardTypeExpression(final WildCardTypeExpression wildCardTypeExpression, final Object param) {
        wildCardTypeExpression.getTypeExpression().accept(this, param);

        final TypeMirror extendsType;
        final TypeMirror superType;

        if (wildCardTypeExpression.getBoundKind() == BoundKind.EXTENDS) {
            extendsType = wildCardTypeExpression.getTypeExpression().getType();
            superType = null;
        } else {
            extendsType = null;
            superType = wildCardTypeExpression.getTypeExpression().getType();
        }

        final var wildCardType = types.getWildcardType(extendsType, superType);
        wildCardTypeExpression.setType(wildCardType);
        return null;
    }

}
