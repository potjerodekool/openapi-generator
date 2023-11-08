package io.github.potjerodekool.openapi.internal.resolve;

import io.github.potjerodekool.codegen.model.CompilationUnit;
import io.github.potjerodekool.codegen.model.CompilationUnitVisitor;
import io.github.potjerodekool.codegen.model.element.Name;
import io.github.potjerodekool.codegen.model.element.NestingKind;
import io.github.potjerodekool.codegen.model.symbol.MethodSymbol;
import io.github.potjerodekool.codegen.model.symbol.VariableSymbol;
import io.github.potjerodekool.codegen.model.tree.JTreeVisitor;
import io.github.potjerodekool.codegen.model.tree.PackageDeclaration;
import io.github.potjerodekool.codegen.model.tree.java.JMethodDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.java.JClassDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.java.JVariableDeclaration;
import io.github.potjerodekool.codegen.model.util.SymbolTable;
import io.github.potjerodekool.codegen.resolve.WritableScope;

public class Enter implements
        CompilationUnitVisitor<Object, Object>,
        JTreeVisitor<Object, Object> {

    private final SymbolTable symbolTable;

    public Enter(final SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    @Override
    public Object visitCompilationUnit(final CompilationUnit compilationUnit,
                                       final Object param) {
        compilationUnit.getDefinitions().forEach(definition -> definition.accept(this, param));
        return null;
    }

    @Override
    public Object visitPackageDeclaration(final PackageDeclaration packageDeclaration, final Object param) {
        final var packageSymbol = symbolTable.enterPackage(null, Name.of(packageDeclaration.getName().getName()));
        packageDeclaration.setPackageSymbol(packageSymbol);
        packageSymbol.scope = new WritableScope(packageSymbol);
        return null;
    }

    @Override
    public Object visitClassDeclaration(final JClassDeclaration classDeclaration,
                                        final Object param) {
        final var enclosing = classDeclaration.getEnclosing();
        final var nestingKind = enclosing instanceof PackageDeclaration
                ? NestingKind.TOP_LEVEL
                : NestingKind.MEMBER;

        final var classSymbol = symbolTable.enterClass(null, classDeclaration.getQualifiedName());
        classSymbol.setKind(classDeclaration.getKind());
        classSymbol.setNestingKind(nestingKind);
        classDeclaration.setClassSymbol(classSymbol);

        classSymbol.scope = new WritableScope(classSymbol);

        classDeclaration.getEnclosed().forEach(enclosed -> enclosed.accept(this, param));
        return null;
    }

    @Override
    public Object visitMethodDeclaration(final JMethodDeclaration methodDeclaration, final Object param) {
        methodDeclaration.getTypeParameters().forEach(typeParam -> typeParam.accept(this, param));
        methodDeclaration.getParameters().forEach(parameter -> parameter.accept(this, param));

        final var parameters = methodDeclaration.getParameters().stream()
                .map(parameter ->  (VariableSymbol) parameter.getSymbol())
                .toList();

        final var methodSymbol = new MethodSymbol(
                methodDeclaration.getKind(),
                null,
                methodDeclaration.getSimpleName()
        );

        methodSymbol.addModifiers(methodDeclaration.getModifiers());
        methodSymbol.addParameters(parameters);

        methodDeclaration.setMethodSymbol(methodSymbol);
        return null;
    }

    @Override
    public Object visitVariableDeclaration(final JVariableDeclaration variableDeclaration,
                                           final Object param) {
        final var variableSymbol = new VariableSymbol(variableDeclaration.getKind(), variableDeclaration.getName());
        variableSymbol.addModifiers(variableDeclaration.getModifiers());
        variableDeclaration.setSymbol(variableSymbol);
        return null;
    }
}
