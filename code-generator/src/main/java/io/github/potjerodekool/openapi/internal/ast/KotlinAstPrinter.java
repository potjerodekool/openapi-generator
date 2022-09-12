package io.github.potjerodekool.openapi.internal.ast;

import io.github.potjerodekool.openapi.internal.Printer;
import io.github.potjerodekool.openapi.internal.ast.element.*;
import io.github.potjerodekool.openapi.internal.ast.expression.*;
import io.github.potjerodekool.openapi.internal.ast.statement.IfStatement;
import io.github.potjerodekool.openapi.internal.ast.type.*;
import io.github.potjerodekool.openapi.internal.ast.type.kotlin.KotlinArray;
import io.github.potjerodekool.openapi.internal.util.Counter;
import io.github.potjerodekool.openapi.internal.util.Utils;

import java.util.List;
import java.util.Optional;

public class KotlinAstPrinter extends AbstractAstPrinter {

    public KotlinAstPrinter(final Printer printer) {
        super(printer);
    }

    //Elements
    @Override
    public Void visitTypeElement(final TypeElement typeElement,
                                 final CodeContext context) {
        printer.printIndent();

        printModifiers(typeElement.getModifiers());

        if (!typeElement.getModifiers().isEmpty()) {
            printer.print(" ");
        }

        switch (typeElement.getKind()) {
            case CLASS -> printer.print("class ");
            case INTERFACE -> printer.print("interface ");
        }

        printer.print(typeElement.getSimpleName());

        final var primaryConstructor = typeElement.getPrimaryConstructor();

        if (primaryConstructor != null) {
            visitPrimaryConstructor(primaryConstructor, context);
        }

        final List<Element> enclosedElements = typeElement.getEnclosedElements();

        if (enclosedElements.size() > 0) {
            printer.printLn(" {");
            printer.indent();

            printer.printLn();
            final var lastIndex = enclosedElements.size() - 1;

            for (int i = 0; i < enclosedElements.size(); i++) {
                final var enclosedElement = enclosedElements.get(i);

                enclosedElement.accept(this, context);

                if (i < lastIndex) {
                    printer.printLn();
                }
            }

            printer.deIndent();

            printer.printLn("}");
        }

        printer.deIndent();
        return null;
    }

    void visitPrimaryConstructor(final MethodElement methodElement,
                                 final CodeContext context) {
        if (methodElement.getAnnotations().size() > 0) {
            printer.print(" ");
            printAnnotations(
                    methodElement.getAnnotations(),
                    false,
                    context
            );
            printer.print(" constructor");
        }
        visitMethodParameters(methodElement.getParameters(), context);
    }

    @Override
    public Void visitExecutableElement(final MethodElement methodElement,
                                       final CodeContext context) {
        if (methodElement.getKind() == ElementKind.CONSTRUCTOR) {
            visitSecondaryConstructor(methodElement, context);
        } else {
            visitMethod(methodElement, context);
        }
        return null;
    }

    private void visitMethod(final MethodElement methodElement,
                             final CodeContext context) {
        final var annotations = methodElement.getAnnotations();
        if (annotations.size() > 0) {
            printAnnotations(annotations, true, context);
            printer.print(" ");
        }

        final var modifiers = methodElement.getModifiers();
        printModifiers(modifiers);

        if (modifiers.size() > 0) {
            printer.print(" ");
        }

        printer.printIndent();
        printer.print("fun ");

        printer.print(methodElement.getSimpleName());

        visitMethodParameters(methodElement.getParameters(), context);

        if (methodElement.getKind() != ElementKind.CONSTRUCTOR) {
            final var returnType = methodElement.getReturnType();

            if (returnType.getKind() != TypeKind.VOID) {
                printer.print(" : ");
                returnType.accept(this, context);
                printer.print(" ");
            }
        }

        methodElement.getBody().ifPresent(body -> body.accept(this, context));
    }

    private void visitSecondaryConstructor(final MethodElement methodElement,
                                           final CodeContext context) {
        final var modifiers = methodElement.getModifiers();
        printModifiers(modifiers);

        if (modifiers.size() > 0) {
            printer.print(" constructor");
            visitMethodParameters(methodElement.getParameters(), context);
            printer.print(": ");
            methodElement.getBody().ifPresent(body -> {
                final var statements = body.getStatements();
                if (!statements.isEmpty()) {
                    statements.get(0).accept(this, context);
                }
            });
        }
    }

    @Override
    public Void visitVariableElement(final VariableElement variableElement,
                                     final CodeContext context) {
        final var isField = variableElement.getKind() == ElementKind.FIELD;
        final var annotations = variableElement.getAnnotations();

        if (annotations.size() > 0
                && isField) {
            printer.printIndent();
        }

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

        printer.print(variableElement.getSimpleName());
        printer.print(" : ");

        final var variableType = variableElement.getType();

        variableType.accept(this, context);

        final var initExpression = variableElement.getInitExpression();

        if (initExpression != null) {
            printer.print(" = ");
            initExpression.accept(this, context);
        }
        return null;
    }

    //Expressions
    @Override
    public Void visitLiteralExpression(final LiteralExpression literalExpression,
                                       final CodeContext context) {
        if (literalExpression.getLiteralType() == LiteralType.CLASS) {
            printer.print(resolveClassName(literalExpression.getValue(), context) + "::class");
            return null;
        } else {
            return super.visitLiteralExpression(literalExpression, context);
        }
    }

    @Override
    public Void visitNamedMethodArgumentExpression(final NamedMethodArgumentExpression namedMethodArgumentExpression,
                                                   final CodeContext context) {
        final var name = namedMethodArgumentExpression.getName();
        final var argument = namedMethodArgumentExpression.getArgument();

        printer.print(name);
        printer.print(" ");
        argument.accept(this, context);
        return null;
    }

    @Override
    public Void visitVariableDeclarationExpression(final VariableDeclarationExpression variableDeclarationExpression,
                                                   final CodeContext context) {
        printModifiers(variableDeclarationExpression.getModifiers());
        printer.print(" ");
        printer.print(variableDeclarationExpression.getName());

        variableDeclarationExpression.getInitExpression().ifPresent(initExpression -> {
            printer.print(" = ");
            initExpression.accept(this, context);
        });
        printer.printLn();
        return null;
    }


    @Override
    public Void visitNewClassExpression(final NewClassExpression newClassExpression,
                                        final CodeContext context) {
        newClassExpression.getClassType().accept(this, context);
        printer.print("()");
        return null;
    }

    @Override
    public Void visitArrayInitializerExpression(final ArrayInitializerExpression arrayInitializerExpression,
                                                final CodeContext context) {
        final var literalTypeOptional = detectTypeOfValues(arrayInitializerExpression);
        final String arrayOfMethodName;

        if (literalTypeOptional.isPresent()) {
            final var literalType = literalTypeOptional.get();
            arrayOfMethodName = switch (literalType) {
                case BYTE -> "byteArrayOf";
                case CHAR -> "charArrayOf";
                case SHORT -> "shortArrayOf";
                case INT -> "intArrayOf";
                case LONG -> "longArrayOf";
                case FLOAT -> "floatArrayOf";
                case DOUBLE -> "doubleArrayOf";
                case BOOLEAN -> "booleanArrayOf";
                default -> "arrayOf";
            };
        } else {
            arrayOfMethodName = "arrayOf";
        }

        printer.print(arrayOfMethodName);
        printer.print("(");

        final var values = arrayInitializerExpression.getValues();

        final var lastIndex = values.size() -1 ;

        final var childContext = context.child(arrayInitializerExpression);

        for (int i = 0; i < values.size(); i++) {
            final var value = values.get(i);
            value.accept(this, childContext);
            if (i < lastIndex) {
                printer.print(", ");
            }
        }
        printer.print(")");
        return null;
    }

    private Optional<LiteralType> detectTypeOfValues(final ArrayInitializerExpression arrayInitializerExpression) {
        final var values = arrayInitializerExpression.getValues();

        if (values.size() > 0) {
            final var firstValue = values.get(0);
            if (firstValue instanceof LiteralExpression le) {
                return Optional.of(le.getLiteralType());
            }
        }

        return Optional.empty();
    }

    private boolean isPartOfAnnotationExpressionOrArrayInitializerExpression(final CodeContext context) {
        final var astNode = context.getAstNode();

        if ((astNode instanceof AnnotationExpression || astNode instanceof ArrayInitializerExpression)) {
            return true;
        }

        final var parentContext = context.getParentContext();
        return parentContext != null && isPartOfAnnotationExpressionOrArrayInitializerExpression(parentContext);
    }

    @Override
    public Void visitAnnotationExpression(final AnnotationExpression annotationExpression,
                                          final CodeContext context) {
        final var elementValues = annotationExpression.getElementValues();

        if (!isPartOfAnnotationExpressionOrArrayInitializerExpression(context)) {
            printer.print("@");
        }

        final var className = resolveClassName(annotationExpression.getAnnotationClassName(), context);

        printer.print(className);

        if (elementValues.size() > 0) {
            printer.print("(");

            final var lastIndex = elementValues.size() - 1;
            final var counter = new Counter();

            final var childContext = context.child(annotationExpression);

            elementValues.forEach((name,value) -> {
                printer.print(name);
                printer.print(" = ");
                value.accept(this, childContext);

                if (counter.getValue() < lastIndex) {
                    printer.print(", ");
                }
                counter.increment();
            });
            printer.print(")");
        }

        return null;
    }

    //Types
    @Override
    public Void visitDeclaredType(final DeclaredType declaredType,
                                  final CodeContext context) {
        final var result = super.visitDeclaredType(declaredType, context);

        if (declaredType.isNullable()) {
            printer.print("?");
        }

        return result;
    }

    @Override
    public Void visitJavaArrayType(final JavaArrayType javaArrayType,
                                   final CodeContext context) {
        printer.print("Array<");
        javaArrayType.getComponentType().accept(this, context);
        printer.print(">");
        return null;
    }

    @Override
    public Void visitKotlinArray(final KotlinArray kotlinArray, final CodeContext context) {
        final var componentType = kotlinArray.getComponentType();
        switch (componentType.getKind()) {
            case BYTE -> printer.print("ByteArray");
            case CHAR -> printer.print("CharArray");
            case SHORT -> printer.print("ShortArray");
            case INT -> printer.print("IntArray");
            case LONG -> printer.print("LongArray");
            case FLOAT -> printer.print("FloatArray");
            case DOUBLE -> printer.print("DoubleArray");
            case BOOLEAN -> printer.print("BooleanArray");
            default -> {
                printer.print("Array<");
                componentType.accept(this, context);
                printer.print(">");
            }
        }
        return null;
    }

    @Override
    public Void visitCharType(final PrimitiveType charType,
                              final CodeContext context) {
        printer.print("Char");
        if (charType.isNullable()) {
            printer.print("?");
        }
        return null;
    }

    @Override
    public Void visitDoubleType(final PrimitiveType doubleType,
                                final CodeContext context) {
        printer.print("Double");
        if (doubleType.isNullable()) {
            printer.print("?");
        }
        return null;
    }

    @Override
    public Void visitFloatType(final PrimitiveType floatType,
                               final CodeContext context) {
        printer.print("Float");
        if (floatType.isNullable()) {
            printer.print("?");
        }
        return null;
    }

    @Override
    public Void visitIntType(final PrimitiveType intType,
                             final CodeContext context) {
        printer.print("Int");
        if (intType.isNullable()) {
            printer.print("?");
        }
        return null;
    }

    @Override
    public Void visitLongType(final PrimitiveType longType,
                              final CodeContext context) {
        printer.print("Long");
        if (longType.isNullable()) {
            printer.print("?");
        }
        return null;
    }

    @Override
    public Void visitBooleanType(final PrimitiveType booleanType,
                                 final CodeContext context) {
        printer.print("Boolean");
        if (booleanType.isNullable()) {
            printer.print("?");
        }
        return null;
    }

    @Override
    public Void visitShortType(final PrimitiveType shortType,
                               final CodeContext context) {
        printer.print("Short");
        if (shortType.isNullable()) {
            printer.print("?");
        }
        return null;
    }

    @Override
    public Void visitByteType(final PrimitiveType byteType,
                              final CodeContext context) {
        printer.print("Byte");
        if (byteType.isNullable()) {
            printer.print("?");
        }
        return null;
    }

    @Override
    public Void visitWildcardType(final WildcardType wildcardType,
                                  final CodeContext context) {
        final var extendsBoundOptional = wildcardType.getExtendsBound();
        final var superBoundOptional = wildcardType.getSuperBound();

        if (extendsBoundOptional.isPresent()) {
            final var extendsBound = extendsBoundOptional.get();
            printer.print("out ");
            extendsBound.accept(this, context);
        } else if (superBoundOptional.isPresent()) {
            superBoundOptional.get().accept(this, context);
        } else {
            throw new UnsupportedOperationException("wildcard without any bound is not supported");
        }
        return null;
    }

    @Override
    public Void visitUnitType(final UnitType unitType,
                              final CodeContext context) {
        printer.print("Unit");
        return null;
    }

    //Statements
    @Override
    public Void visitIfStatement(final IfStatement ifStatement,
                                 final CodeContext context) {
        return super.visitIfStatement(ifStatement, context);
    }

    @Override
    protected boolean useSemiColonAfterStatement() {
        return false;
    }

    @Override
    protected String resolveClassName(final String className,
                                      final CodeContext context) {
        final var qualifiedName = Utils.resolveQualifiedName(className);
        if ("kotlin".equals(qualifiedName.packageName())) {
            return qualifiedName.simpleName();
        }
        return super.resolveClassName(className, context);
    }
}
