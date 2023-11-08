package io.github.potjerodekool.openapi;

import io.github.potjerodekool.codegen.io.Printer;
import io.github.potjerodekool.codegen.model.CompilationUnit;
import io.github.potjerodekool.codegen.model.CompilationUnitVisitor;
import io.github.potjerodekool.codegen.model.element.ElementKind;
import io.github.potjerodekool.codegen.model.element.Modifier;
import io.github.potjerodekool.codegen.model.tree.JTreeVisitor;
import io.github.potjerodekool.codegen.model.tree.PackageDeclaration;
import io.github.potjerodekool.codegen.model.tree.TreeVisitor;
import io.github.potjerodekool.codegen.model.tree.expression.*;
import io.github.potjerodekool.codegen.model.tree.java.JMethodDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.BlockStatement;
import io.github.potjerodekool.codegen.model.tree.statement.ExpressionStatement;
import io.github.potjerodekool.codegen.model.tree.statement.ReturnStatement;
import io.github.potjerodekool.codegen.model.tree.statement.java.JClassDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.java.JVariableDeclaration;
import io.github.potjerodekool.codegen.model.tree.type.ClassOrInterfaceTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.NoTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.PrimitiveTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.VarTypeExpression;
import io.github.potjerodekool.codegen.model.type.TypeKind;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AstPrinter implements CompilationUnitVisitor<Object, Object>,
        TreeVisitor<Object, Object>,
        JTreeVisitor<Object, Object> {

    private final StringWriter writer = new StringWriter();

    private final BufferedWriter bufferedWriter = new BufferedWriter(writer);

    private final Printer printer = new Printer(bufferedWriter);

    @Override
    public Object visitCompilationUnit(final CompilationUnit compilationUnit, final Object param) {
        compilationUnit.getDefinitions().forEach(definitation -> {
            definitation.accept(this, param);
        });

        return null;
    }

    @Override
    public Object visitPackageDeclaration(final PackageDeclaration packageDeclaration, final Object param) {
        if (!packageDeclaration.isDefaultPackage()) {
            final String packageName = packageDeclaration.getName().getName();

            printLn("package " + packageName + ";");
            printLn("");
        }

        return null;
    }

    @Override
    public Object visitClassDeclaration(final JClassDeclaration classDeclaration, final Object param) {
        final var kind = switch (classDeclaration.getKind()) {
            case CLASS -> "class";
            default -> "kind";
        };


        print(kind);
        print(" ");
        print(classDeclaration.getSimpleName());
        printLn(" {");

        classDeclaration.getEnclosed().forEach(enclosed -> {
            indent();
            printLn();
            enclosed.accept(this, param);
            deIndent();
        });

        printLn("}");

        return null;
    }

    @Override
    public Object visitMethodDeclaration(final JMethodDeclaration methodDeclaration, final Object param) {
        printIndent();

        if (!methodDeclaration.getModifiers().isEmpty()) {
            printModifiers(methodDeclaration.getModifiers());
            print(" ");
        }

        if (methodDeclaration.getKind() != ElementKind.CONSTRUCTOR) {
            methodDeclaration.getReturnType().accept(this, param);
            print(" ");
        }

        print(methodDeclaration.getSimpleName());
        print("(");

        final var parameters = methodDeclaration.getParameters();
        final var lastIndex = parameters.size() - 1;

        for (int i = 0; i < parameters.size(); i++) {
            parameters.get(i).accept(this, param);
            if (i < lastIndex) {
                print(", ");
            }
        }

        print(")");

        if (methodDeclaration.getBody().isPresent()) {
            print(" ");
            methodDeclaration.getBody().get().accept(this, param);
        } else {
            printLn(";");
        }

        return null;
    }

    @Override
    public Object visitVariableDeclaration(final JVariableDeclaration variableDeclaration, final Object param) {
        if (variableDeclaration.getKind() != ElementKind.PARAMETER) {
            printIndent();
        }

        if (!variableDeclaration.getModifiers().isEmpty()) {
            printModifiers(variableDeclaration.getModifiers());
            print(" ");
        }

        variableDeclaration.getVarType().accept(this, param);
        print(" ");
        print(variableDeclaration.getName());

        variableDeclaration.getInitExpression().ifPresent(initExpression -> {
            print(" = ");
            initExpression.accept(this, param);
        });

        if (variableDeclaration.getKind() == ElementKind.FIELD
            || variableDeclaration.getKind() == ElementKind.LOCAL_VARIABLE) {
            printLn(";");
        }


        return null;
    }

    @Override
    public Object visitBlockStatement(final BlockStatement blockStatement, final Object param) {
        printLn("{");
        indent();
        blockStatement.getStatements().forEach(statement -> {
            printIndent();
            statement.accept(this, param);
        });
        deIndent();
        printIndent();
        printLn("}");
        return null;
    }

    @Override
    public Object visitClassOrInterfaceTypeExpression(final ClassOrInterfaceTypeExpression classOrInterfaceTypeExpression, final Object param) {
        print(classOrInterfaceTypeExpression.getName());
        return null;
    }

    @Override
    public Object visitExpressionStatement(final ExpressionStatement expressionStatement, final Object param) {
        expressionStatement.getExpression().accept(this, param);
        printLn(";");
        return null;
    }

    @Override
    public Object visitBinaryExpression(final BinaryExpression binaryExpression, final Object param) {
        binaryExpression.getLeft().accept(this, param);
        printOperator(binaryExpression.getOperator());
        binaryExpression.getRight().accept(this, param);
        return null;
    }

    private void printOperator(final Operator operator) {
        switch (operator) {
            case ASSIGN -> print(" = ");
            default -> throw new UnsupportedOperationException(operator.name());
        }
    }

    @Override
    public Object visitFieldAccessExpression(final FieldAccessExpression fieldAccessExpression, final Object param) {
        final var scope = fieldAccessExpression.getScope();

        if (scope != null) {
            scope.accept(this, param);
            print(".");
        }

        print(fieldAccessExpression.getField());
        return null;
    }

    @Override
    public Object visitIdentifierExpression(final IdentifierExpression identifierExpression, final Object param) {
        print(identifierExpression.getName());
        return null;
    }

    @Override
    public Object visitReturnStatement(final ReturnStatement returnStatement, final Object param) {
        print("return ");
        returnStatement.getExpression().accept(this, param);
        printLn(";");
        return null;
    }

    @Override
    public Object visitPrimitiveTypeExpression(final PrimitiveTypeExpression primitiveTypeExpression, final Object param) {
        final var primitiveName = switch (primitiveTypeExpression.getKind()) {
            case INT -> "int";
            default -> throw new UnsupportedOperationException(primitiveTypeExpression.getKind().name());
        };

        print(primitiveName);

        return null;
    }

    @Override
    public Object visitLiteralExpression(final LiteralExpression literalExpression, final Object param) {
        final var literalValue = switch (literalExpression.getLiteralType()) {
            case NULL -> "null";
            case INT -> ((StringValueLiteralExpression)literalExpression).getValue();
            default -> throw new UnsupportedOperationException(literalExpression.getLiteralType().name());
        };

        print(literalValue);
        return null;
    }

    @Override
    public Object visitNoType(final NoTypeExpression noTypeExpression, final Object param) {
        if (noTypeExpression.getKind() == TypeKind.VOID) {
            print("void");
        } else {
            throw new UnsupportedOperationException(noTypeExpression.getKind().name());
        }
        return null;
    }

    private void printModifiers(final Set<Modifier> modifiers) {
        print(modifiers.stream()
                .map(m -> m.name().toLowerCase())
                .collect(Collectors.joining(" "))
        );
    }

    @Override
    public Object visitMethodCall(final MethodCallExpression methodCallExpression, final Object param) {
        methodCallExpression.getTarget().ifPresent(target -> {
            target.accept(this, param);
            print(".");
        });

        print(methodCallExpression.getMethodName());
        print("(");

        printList(methodCallExpression.getArguments(), param, ", ");

        print(")");

        return null;
    }

    @Override
    public Object visitVarTypeExpression(final VarTypeExpression varTypeExpression, final Object param) {
        print("var");
        return null;
    }

    @Override
    public Object visitNewClassExpression(final NewClassExpression newClassExpression, final Object param) {
        print("new ");
        newClassExpression.getClazz().accept(this, param);
        print("(");
        printList(newClassExpression.getArguments(), param, ", ");
        print(")");
        return null;
    }

    private void printList(final List<Expression> list,
                           final Object param,
                           final String separator) {
        final var lastIndex = list.size() - 1;

        for (int i = 0; i < list.size(); i++) {
            list.get(i).accept(this, param);
            if (i < lastIndex) {
                print(separator);
            }
        }
    }


    private void print(final Object value) {
        printer.print(value.toString());
    }

    private void printLn(final Object value) {
        printer.printLn(value.toString());
    }

    private void printLn() {
        printer.printLn();
    }

    private void indent() {
        printer.indent();
    }

    private void deIndent() {
        printer.deIndent();
    }

    private void printIndent() {
        printer.printIndent();
    }

    public String getCode() {
        try {
            bufferedWriter.flush();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return writer.getBuffer().toString();
    }
}
