package io.github.potjerodekool.openapi.internal.ast;

import io.github.potjerodekool.openapi.internal.Printer;
import io.github.potjerodekool.openapi.internal.ast.element.*;
import io.github.potjerodekool.openapi.internal.ast.expression.*;
import io.github.potjerodekool.openapi.internal.ast.statement.*;
import io.github.potjerodekool.openapi.internal.ast.type.*;
import io.github.potjerodekool.openapi.internal.ast.type.java.VoidType;
import io.github.potjerodekool.openapi.internal.ast.util.TypeUtils;
import io.github.potjerodekool.openapi.internal.util.QualifiedName;

import javax.lang.model.element.ElementKind;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public abstract class AbstractAstPrinter implements CompilationUnitVisitor<Void, CodeContext>,
        ElementVisitor<Void, CodeContext>,
        TypeVisitor<Void, CodeContext>,
        StatementVisitor<Void, CodeContext>,
        ExpressionVisitor<Void, CodeContext>,
        AnnotationValueVisitor<Void, CodeContext> {

    protected final Printer printer;
    private final TypeUtils typeUtils;

    protected AbstractAstPrinter(final Printer printer,
                                 final TypeUtils typeUtils) {
        this.printer = printer;
        this.typeUtils = typeUtils;
    }

    @Override
    public Void visitCompilationUnit(final CompilationUnit compilationUnit,
                                     final CodeContext context) {
        final var packageElement = compilationUnit.getPackageElement();

        if (!packageElement.isDefaultPackage()) {
            packageElement.accept(this, context);
            printer.printLn();
            printer.printLn();
        }

        final var imports = compilationUnit.getImports();

        if (imports.size() > 0) {
            imports.forEach(importStr -> {
                printer.print("import " + importStr);
                if (useSemiColonAfterStatement()) {
                    printer.print(";");
                }
                printer.printLn();
            });
            printer.printLn();
        }

        compilationUnit.getElements().forEach(element -> element.accept(this, context));
        return null;
    }

    //Elements
    @Override
    public Void visitPackageElement(final PackageElement packageElement,
                                    final CodeContext context) {
        if (!packageElement.isDefaultPackage()) {
            printer.print("package " + packageElement.getQualifiedName());

            if (useSemiColonAfterStatement()) {
                printer.print(";");
            }
        }
        return null;
    }

    protected void printModifiers(final Set<Modifier> modifiers) {
        if (modifiers.size() > 0) {
            final var mods = modifiers.stream()
                    .map(this::modifierToString)
                    .collect(Collectors.joining(" "));
            printer.print(mods);
        }
    }

    private String modifierToString(final Modifier modifier) {
        return modifier.name().toLowerCase();
    }

    @Override
    public Void visitExecutableElement(final MethodElement methodElement, final CodeContext context) {
        printer.printIndent();

        final var modifiers = methodElement.getModifiers();
        printModifiers(modifiers);

        if (modifiers.size() > 0) {
            printer.print(" ");
        }

        if (methodElement.getKind() != ElementKind.CONSTRUCTOR) {
            methodElement.getReturnType().accept(this, context);
            printer.print(" ");
        }

        printer.print(methodElement.getSimpleName());

        visitMethodParameters(methodElement.getParameters(), context);
        printer.printLn();

        methodElement.getBody().ifPresent(body -> body.accept(this, context));
        return null;
    }

    public void visitMethodParameters(final List<VariableElement> parameters,
                                      final CodeContext context) {
        printer.print("(");

        final int lastParameter = parameters.size() - 1;

        for (int i = 0; i < parameters.size(); i++) {
            final var parameter = parameters.get(i);
            parameter.accept(this, context);
            if (i < lastParameter) {
                printer.print(", ");
            }
        }
        printer.print(")");
    }

    @Override
    public Void visitVariableElement(final VariableElement variableElement,
                                     final CodeContext context) {
        final var isField = variableElement.getKind() == ElementKind.FIELD;
        final var annotations = variableElement.getAnnotations();

        printAnnotations(variableElement.getAnnotations(), isField, context);

        final var modifiers = variableElement.getModifiers();

        if (modifiers.size() > 0
                && isField) {
            printer.printIndent();
        }

        if (modifiers.size() > 0
                && annotations.size() > 0
                && variableElement.getKind() == ElementKind.PARAMETER) {
            printer.print(" ");
        }

        printModifiers(modifiers);

        if (annotations.size() > 0
                || modifiers.size() > 0) {
            printer.print(" ");
        }

        variableElement.getType().accept(this, context);
        printer.print(" ");
        printer.print(variableElement.getSimpleName());

        if (isField) {
            printer.printLn(";");
        }
        return null;
    }

    protected void printAnnotations(final List<AnnotationMirror> annotations,
                                    final boolean addNewLineAfterAnnotation,
                                    final CodeContext context) {
        if (annotations.isEmpty()) {
            return;
        }

        final var lastIndex = annotations.size() - 1;

        for (int i = 0; i < annotations.size(); i++) {
            if (addNewLineAfterAnnotation) {
                printer.printIndent();
            }

            final var annotation = annotations.get(i);
            printAnnotation(annotation, addNewLineAfterAnnotation, context);

            if (i < lastIndex && !addNewLineAfterAnnotation) {
                printer.print(" ");
            }
        }
    }

    protected void printAnnotation(final AnnotationMirror annotation,
                                   final boolean addNewLineAfterAnnotation,
                                   final CodeContext context) {
        final String annotationName = resolveClassName(annotation.getAnnotationType().getElement().getQualifiedName(), context);
        printer.print("@").print(annotationName).print("(");

        final var elementValues = annotation.getElementValues();
        final var lastIndex = elementValues.size() - 1;
        final var childContext = context.child(annotation);
        final var elementValueIndex = new AtomicInteger();

        annotation.getElementValues().forEach((key, value) -> {
            printer.print(name(key.getSimpleName())).print(" = ");
            value.accept(this, childContext);

            if (elementValueIndex.get() < lastIndex) {
                printer.print(", ");
            }
            elementValueIndex.incrementAndGet();
        });

        printer.print(")");

        if (addNewLineAfterAnnotation) {
            printer.printLn();
        }
    }

    protected String name(final String value) {
        return value;
    }

    @Override
    public Void visitUnknown(final Element element, final CodeContext context) {
        return null;
    }

    //Statements
    @Override
    public Void visitBlockStatement(final BlockStatement blockStatement,
                                    final CodeContext context) {
        printer.printLn("{");
        printer.indent();

        final var statements = blockStatement.getStatements();

        statements.forEach(statement -> {
            printer.printIndent();
            statement.accept(this, context);
            if (useSemiColonAfterStatement()) {
                printer.print(";");
            }
            printer.printLn();
        });
        printer.deIndent();

        if (statements.isEmpty()) {
            printer.printLn();
        }
        printer.printIndent();
        printer.printLn("}");
        return null;
    }

    @Override
    public Void visitExpressionStatement(final ExpressionStatement expressionStatement,
                                         final CodeContext context) {
        expressionStatement.getExpression().accept(this, context);
        return null;
    }

    @Override
    public Void visitReturnStatement(final ReturnStatement returnStatement,
                                     final CodeContext context) {
        printer.print("return ");
        returnStatement.getExpression().accept(this, context);
        return null;
    }

    @Override
    public Void visitUnknown(final Statement statement,
                             final CodeContext context) {
        printer.printLn("unknown statement " + statement);
        return null;
    }

    @Override
    public Void visitIfStatement(final IfStatement ifStatement,
                                 final CodeContext context) {
        printer.print("if (");
        ifStatement.getCondition().accept(this, context);
        printer.print(")");
        ifStatement.getBody().accept(this, context);
        return null;
    }

    //Expressions
    @Override
    public Void visitBinaryExpression(final BinaryExpression binaryExpression,
                                      final CodeContext context) {
        binaryExpression.getLeft().accept(this, context);
        printer.print(" ");

        switch (binaryExpression.getOperator()) {
            case ASSIGN -> printer.print("=");
            case MINUS -> printer.print("-");
            case NOT_EQUALS -> printer.print("!=");
        }

        printer.print(" ");
        binaryExpression.getRight().accept(this, context);
        return null;
    }

    @Override
    public Void visitFieldAccessExpression(final FieldAccessExpression fieldAccessExpression,
                                           final CodeContext context) {
        fieldAccessExpression.getScope().accept(this, context);
        printer.print(".");
        fieldAccessExpression.getField().accept(this, context);
        return null;
    }

    @Override
    public Void visitNameExpression(final NameExpression nameExpression,
                                    final CodeContext context) {
        printer.print(nameExpression.getName());
        return null;
    }

    @Override
    public Void visitUnknown(final Expression expression,
                             final CodeContext context) {
        printer.printLn("unknown expression " + expression);
        return null;
    }

    @Override
    public Void visitLiteralExpression(final LiteralExpression literalExpression,
                                       final CodeContext context) {
        switch (literalExpression.getLiteralType()) {
            case NULL -> printer.print("null");
            case CLASS -> {
                final var classLiteralExpression = (ClassLiteralExpression) literalExpression;
                final var type = classLiteralExpression.getType();

                if (context.getAstNode() instanceof AnnotationMirror) {
                    final String className;
                    if (type.isDeclaredType()) {
                        final var declaredType = (DeclaredType) type;
                        className = resolveClassName(declaredType.getElement().getQualifiedName(), context);
                    } else {
                        final var boxedType = typeUtils.getBoxedType(type);
                        className = resolveClassName(boxedType.getElement().getQualifiedName(), context);
                    }
                    printer.print(className + ".class");
                } else if (type.isDeclaredType()) {
                    final var declaredType = (DeclaredType) type;
                    final String className = resolveClassName(declaredType.getElement().getQualifiedName(), context);
                    printer.print(className + ".class");
                } else if (type.isPrimitiveType()) {
                    final var boxedType = typeUtils.getBoxedType(type);
                    final String className = resolveClassName(boxedType.getElement().getQualifiedName(), context);
                    printer.print(className + ".TYPE");
                } else {
                    throw new UnsupportedOperationException("TODO");
                }
            }
            case STRING -> {
                final var le = (StringValueLiteralExpression) literalExpression;
                printer.print("\"" + le.getValue() + "\"");
            }
            case CHAR -> {
                final var le = (StringValueLiteralExpression) literalExpression;
                printer.print("'" + le.getValue() + "'");
            }
            default -> {
                final var le = (StringValueLiteralExpression) literalExpression;
                printer.print(le.getValue());
            }
        }
        return null;
    }

    @Override
    public Void visitMethodCall(final MethodCallExpression methodCallExpression,
                                final CodeContext context) {
        methodCallExpression.getTarget()
                .ifPresent(target -> {
                    target.accept(this, context);
                    printer.print(".");
                });

        printer.print(methodCallExpression.getMethodName());
        printer.print("(");

        final var arguments = methodCallExpression.getArguments();
        final var lastIndex = arguments.size() - 1;

        for (int i = 0; i < arguments.size(); i++) {
            arguments.get(i).accept(this, context);
            if (i < lastIndex) {
                printer.print(", ");
            }
        }

        printer.print(")");
        return null;
    }

    @Override
    public Void visitArrayAccessExpression(final ArrayAccessExpression arrayAccessExpression,
                                           final CodeContext context) {
        arrayAccessExpression.getArrayExpression().accept(this, context);
        printer.print("[");
        arrayAccessExpression.getIndexExpression().accept(this, context);
        printer.print("]");
        return null;
    }

    @Override
    public Void visitArrayInitializerExpression(final ArrayInitializerExpression arrayInitializerExpression,
                                                final CodeContext context) {
        return visitUnknown(arrayInitializerExpression, context);
    }

    //Types
    @Override
    public Void visitVoidType(final VoidType voidType,
                              final CodeContext context) {
        printer.print("void");
        return null;
    }

    @Override
    public Void visitDeclaredType(final DeclaredType declaredType,
                                  final CodeContext context) {
        final var annotations = declaredType.getAnnotations();

        if (annotations.size() > 0) {
            printAnnotations(annotations,false, context);
            printer.print(" ");
        }

        printer.print(resolveClassName(declaredType.getElement().getQualifiedName(), context));

        final var typeArgsOptional = declaredType.getTypeArguments();

        if (typeArgsOptional.isPresent()) {
            final var typeArgs = typeArgsOptional.get();
            printer.print("<");

            final int lastIndex = typeArgs.size() - 1;

            for (int i = 0; i < typeArgs.size(); i++) {
                final var typeArg = typeArgs.get(i);
                typeArg.accept(this, context);

                if (i < lastIndex) {
                    printer.print(",");
                }
            }

            printer.print(">");
        }

        return null;
    }

    @Override
    public Void visitPackageType(final PackageType packageType, final CodeContext context) {
        return null;
    }

    @Override
    public Void visitExecutableType(final ExecutableType executableType, final CodeContext context) {
        return null;
    }

    @Override
    public Void visitUnknownType(final Type<?> type,
                                 final CodeContext context) {
        return null;
    }

    @Override
    public Void visitBoolean(final boolean value, final CodeContext param) {
        printer.print(Boolean.toString(value));
        return null;
    }

    @Override
    public Void visitInt(final int value, final CodeContext param) {
        printer.print(Integer.toString(value));
        return null;
    }

    @Override
    public Void visitLong(final long value, final CodeContext param) {
        printer.print(Long.toString(value));
        return null;
    }

    @Override
    public Void visitString(final String value, final CodeContext param) {
        printer.print("\"");
        printer.print(value);
        printer.print("\"");
        return null;
    }

    @Override
    public Void visitArray(final Attribute[] array, final CodeContext param) {
        printer.print("{");

        final var lastIndex = array.length - 1;

        for (int valueIndex = 0; valueIndex < array.length; valueIndex++) {
            array[valueIndex].accept(this, param);
            if (valueIndex < lastIndex) {
                printer.print(", ");
            }
        }

        printer.print("}");

        return null;
    }

    @Override
    public Void visitType(final Type<?> type, final CodeContext codeContext) {
        final var element = type.getElement();

        if (element instanceof TypeElement te) {
            if (isInImport(te.getQualifiedName(), codeContext)) {
                printer.print(element.getSimpleName());
            } else {
                printer.print(te.getQualifiedName());
            }
        } else {
            printer.print(element.getSimpleName());
        }
        printer.print(".class");
        return null;
    }

    private boolean isInImport(final String className, final CodeContext codeContext) {
        if (className.startsWith("java.lang.")) {
            return true;
        } else {
            final var compilationUnitOptional = codeContext.resolveCompilationUnit();
            if (compilationUnitOptional.isPresent()) {
                final var compilationUnit = compilationUnitOptional.get();
                if (compilationUnit.getImports().contains(className)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public Void visitEnum(final VariableElement enumValue, final CodeContext param) {
        final var enumElement = (TypeElement) enumValue.getEnclosingElement();

        printer
                .print(enumElement.getQualifiedName())
                .print(".")
                .print(enumValue.getSimpleName());

        return null;
    }

    protected boolean useSemiColonAfterStatement() {
        return true;
    }

    protected String resolveClassName(final String className, final CodeContext context) {
        final var compilationUnit = findCompilationUnit(context);

        if (compilationUnit.getImports().contains(className)) {
            return QualifiedName.from(className).simpleName();
        }

        return className;
    }

    private CompilationUnit findCompilationUnit(final CodeContext context) {
        final var astNode = context.getAstNode();
        if (astNode instanceof CompilationUnit cu) {
            return cu;
        } else {
            final var parentContext = context.getParentContext();

            if (parentContext != null) {
                return findCompilationUnit(parentContext);
            } else {
                throw new IllegalStateException("Failed to find compilation unit in context");
            }
        }
    }
}

