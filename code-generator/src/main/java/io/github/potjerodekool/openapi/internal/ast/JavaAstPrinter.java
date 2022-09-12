package io.github.potjerodekool.openapi.internal.ast;

import io.github.potjerodekool.openapi.internal.Printer;
import io.github.potjerodekool.openapi.internal.ast.element.*;
import io.github.potjerodekool.openapi.internal.ast.expression.*;
import io.github.potjerodekool.openapi.internal.ast.type.*;
import io.github.potjerodekool.openapi.internal.util.Counter;
import io.github.potjerodekool.openapi.internal.util.Utils;

import java.util.List;

//literal, declaredtype, AnnotationExpression
public class JavaAstPrinter extends AbstractAstPrinter {

    public JavaAstPrinter(final Printer printer) {
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
            visitPrimaryConstructor(primaryConstructor);
        }

        final List<Element> enclosedElements = typeElement.getEnclosedElements();

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
        printer.deIndent();
        return null;
    }

    void visitPrimaryConstructor(final MethodElement methodElement) {
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

    @Override
    public Void visitExecutableElement(final MethodElement methodElement,
                                       final CodeContext context) {
        final var annotations = methodElement.getAnnotations();
        if (annotations.size() > 0) {
            printAnnotations(annotations, true, context);
            printer.print(" ");
        }

        final var modifiers = methodElement.getModifiers();

        if (modifiers.size() > 0) {
            printer.printIndent();
            printModifiers(modifiers);
            printer.print(" ");
        }

        if (methodElement.getKind() != ElementKind.CONSTRUCTOR) {
            methodElement.getReturnType().accept(this, context);
            printer.print(" ");
        }

        printer.print(methodElement.getSimpleName());

        visitMethodParameters(methodElement.getParameters(), context);

        methodElement.getBody().ifPresent(body -> body.accept(this, context));
        return null;
    }

    //Expressions
    @Override
    public Void visitVariableDeclarationExpression(final VariableDeclarationExpression variableDeclarationExpression,
                                                   final CodeContext context) {
        final var modifiers = variableDeclarationExpression.getModifiers();

        if (modifiers.size() > 0) {
            printModifiers(modifiers);
            printer.print(" ");
        }

        variableDeclarationExpression.getType().accept(this, context);
        printer.print(" ");

        printer.print(variableDeclarationExpression.getName());

        variableDeclarationExpression.getInitExpression().ifPresent(initExpression -> {
            printer.print(" = ");
            initExpression.accept(this, context);
        });

        return null;
    }

    @Override
    public Void visitNamedMethodArgumentExpression(final NamedMethodArgumentExpression namedMethodArgumentExpression,
                                                   final CodeContext context) {
        return null;
    }

    @Override
    public Void visitNewClassExpression(final NewClassExpression newClassExpression,
                                        final CodeContext context) {
        printer.print("new ");
        newClassExpression.getClassType().accept(this, context);
        printer.print("()");
        return null;
    }

    @Override
    public Void visitArrayInitializerExpression(final ArrayInitializerExpression arrayInitializerExpression,
                                                final CodeContext context) {
        final var values = arrayInitializerExpression.getValues();

        printer.print("{");
        final var lastIndex = values.size() - 1;

        for (int i = 0; i < values.size(); i++) {
            final var value = values.get(i);
            value.accept(this, context);
            if (i < lastIndex) {
                printer.print(", ");
            }
        }
        printer.print("}");

        return null;
    }

    @Override
    public Void visitAnnotationExpression(final AnnotationExpression annotationExpression,
                                          final CodeContext context) {
        final var elementValues = annotationExpression.getElementValues();

        printer.print("@");
        printer.print(resolveClassName(annotationExpression.getAnnotationClassName(), context));

        if (elementValues.size() > 0) {
            printer.print("(");

            final var lastIndex = elementValues.size() - 1;
            final var counter = new Counter();

            elementValues.forEach((name,value) -> {
                printer.print(name);
                printer.print(" = ");
                value.accept(this, context);

                if (counter.getValue() < lastIndex) {
                    printer.print(", ");
                }
                counter.increment();
            });
            printer.print(")");
        }

        return null;
    }

    @Override
    public Void visitJavaArrayType(final JavaArrayType javaArrayType,
                                   final CodeContext context) {
        javaArrayType.getComponentType().accept(this, context);
        printer.print("[]");
        return null;
    }

    @Override
    public Void visitBooleanType(final PrimitiveType booleanType,
                                 final CodeContext context) {
        printer.print("boolean");
        return null;
    }

    @Override
    public Void visitByteType(final PrimitiveType byteType,
                              final CodeContext context) {
        printer.print("byte");
        return null;
    }

    @Override
    public Void visitFloatType(final PrimitiveType floatType,
                               final CodeContext context) {
        printer.print("float");
        return null;
    }

    @Override
    public Void visitDoubleType(final PrimitiveType doubleType,
                                final CodeContext context) {
        printer.print("double");
        return null;
    }

    @Override
    public Void visitShortType(final PrimitiveType shortType,
                               final CodeContext context) {
        printer.print("short");
        return null;
    }

    @Override
    public Void visitLongType(final PrimitiveType longType,
                              final CodeContext context) {
        printer.print("long");
        return null;
    }

    @Override
    public Void visitIntType(final PrimitiveType intType,
                             final CodeContext context) {
        printer.print("int");
        return null;
    }

    @Override
    public Void visitCharType(final PrimitiveType charType,
                              final CodeContext context) {
        printer.print("char");
        return null;
    }

    @Override
    public Void visitWildcardType(final WildcardType wildcardType,
                                  final CodeContext context) {
        final var extendsBoundOptional = wildcardType.getExtendsBound();
        final var superBoundOptional = wildcardType.getSuperBound();

        if (extendsBoundOptional.isPresent()) {
            final var extendsBound = extendsBoundOptional.get();
            printer.print("? extends ");
            extendsBound.accept(this, context);
        } else if (superBoundOptional.isPresent()) {
            final var superBound = superBoundOptional.get();
            printer.print("? super ");
            superBound.accept(this, context);
        } else {
            printer.print("?");
        }
        return null;
    }

    @Override
    public Void visitUnitType(final UnitType unitType, final CodeContext context) {
        return null;
    }

    @Override
    protected String resolveClassName(final String className, final CodeContext context) {
        final var qualifiedName = Utils.resolveQualifiedName(className);
        if ("java.lang".equals(qualifiedName.packageName())) {
            return qualifiedName.simpleName();
        }
        return super.resolveClassName(className, context);
    }
}
