package io.github.potjerodekool.openapi.internal.ast;

import io.github.potjerodekool.openapi.internal.ast.element.*;
import io.github.potjerodekool.openapi.internal.ast.expression.*;
import io.github.potjerodekool.openapi.internal.ast.statement.*;
import io.github.potjerodekool.openapi.internal.ast.type.*;
import io.github.potjerodekool.openapi.internal.ast.type.java.*;
import io.github.potjerodekool.openapi.internal.ast.type.kotlin.KotlinArrayType;
import io.github.potjerodekool.openapi.internal.ast.type.kotlin.UnitType;
import io.github.potjerodekool.openapi.internal.ast.util.TypeUtils;
import io.github.potjerodekool.openapi.internal.util.QualifiedName;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ImportOrganiser implements ElementVisitor<Object, CompilationUnit>,
        TypeVisitor<Type<?>, CompilationUnit>,
        StatementVisitor<Statement, CompilationUnit>,
        ExpressionVisitor<Expression, CompilationUnit>,
        AnnotationValueVisitor<AnnotationValue, CompilationUnit> {

    private final TypeUtils typeUtils;

    public ImportOrganiser(final TypeUtils typeUtils) {
        this.typeUtils = typeUtils;
    }

    public CompilationUnit organiseImports(final CompilationUnit compilationUnit) {
        final var cu = new CompilationUnit(compilationUnit.getLanguage());

        final var packageElement = (PackageElement) compilationUnit.getPackageElement().accept(this, cu);
        cu.setPackageElement(packageElement);

        final var elements = compilationUnit.getElements().stream()
                .map(element -> (Element) element.accept(this, cu))
                .peek(element -> {
                    if (element instanceof TypeElement
                        && packageElement != null) {
                        element.setEnclosingElement(packageElement);
                    }
                })
                .toList();

        elements.forEach(cu::addElement);

        compilationUnit.getImports().forEach(cu::addImport);

        return cu;
    }

    @Override
    public PackageElement visitPackageElement(final PackageElement packageElement,
                                              final CompilationUnit cu) {
        return packageElement;
    }

    @Override
    public TypeElement visitTypeElement(final TypeElement typeElement,
                                        final CompilationUnit compilationUnit) {
        final var annotations = processAnnotations(typeElement.getAnnotations(), compilationUnit);
        final var newTypeElement = TypeElement.create(
                typeElement.getKind(),
                annotations,
                typeElement.getModifiers(),
                typeElement.getSimpleName()
        );

        if (typeElement.getSuperType() != null) {
            final var superType = typeElement.getSuperType().accept(this, compilationUnit);
            newTypeElement.setSuperType(superType);
        }

        typeElement.getInterfaces().stream()
                .map(interfaceType -> interfaceType.accept(this, compilationUnit))
                .forEach(newTypeElement::addInterface);

        final var primaryConstructor = typeElement.getPrimaryConstructor();

        if (primaryConstructor != null) {
            final var method = (MethodElement) primaryConstructor.accept(this, compilationUnit);
            newTypeElement.addPrimaryConstructor(method);
        }

        final var enclosedElements = typeElement.getEnclosedElements().stream()
                .map(element -> (Element) element.accept(this, compilationUnit))
                .toList();

        enclosedElements.forEach(newTypeElement::addEnclosedElement);

        return newTypeElement;
    }

    @Override
    public VariableElement visitVariableElement(final VariableElement variableElement,
                                                final CompilationUnit cu) {
        Expression initExpression = variableElement.getInitExpression();

        if (initExpression != null) {
            initExpression = initExpression.accept(this, cu);
        }

        final var annotations = processAnnotations(variableElement.getAnnotations(), cu);

        return VariableElement.create(
                variableElement.getKind(),
                variableElement.getType().accept(this, cu),
                variableElement.getSimpleName(),
                annotations,
                variableElement.getModifiers(),
                initExpression
        );
    }

    @Override
    public MethodElement visitExecutableElement(final MethodElement methodElement,
                                                final CompilationUnit cu) {
        final var annotations = processAnnotations(methodElement.getAnnotations(), cu);
        final var parameters = methodElement.getParameters().stream()
                .map(parameter -> (VariableElement) parameter.accept(this, cu))
                .toList();

        final var returnType = methodElement.getReturnType().accept(this, cu);

        final var body = methodElement.getBody()
                .map(it -> (BlockStatement) it.accept(this, cu))
                .orElse(null);

        return MethodElement.create(
                methodElement.getKind(),
                annotations,
                methodElement.getModifiers(),
                returnType,
                methodElement.getSimpleName(),
                parameters,
                body
        );
    }

    @Override
    public Object visitUnknown(final Element element,
                               final CompilationUnit cu) {
        log("visitUnknown " + element);
        return ErrorElement.create();
    }

    @Override
    public Type<?> visitVoidType(final VoidType voidType,
                                 final CompilationUnit cu) {
        return voidType;
    }

    @Override
    public Type<?> visitUnitType(final UnitType unitType, final CompilationUnit param) {
        return unitType;
    }

    //Expressions
    @Override
    public Expression visitUnknown(final Expression expression,
                                   final CompilationUnit cu) {
        log("visitUnknown " + expression);
        return new ErrorExpression();
    }

    @Override
    public Expression visitBinaryExpression(final BinaryExpression binaryExpression,
                                            final CompilationUnit cu) {
        final var left = binaryExpression.getLeft().accept(this, cu);
        final var right = binaryExpression.getRight().accept(this, cu);
        return new BinaryExpression(
                left,
                right,
                binaryExpression.getOperator()
        );
    }

    @Override
    public Expression visitFieldAccessExpression(final FieldAccessExpression fieldAccessExpression,
                                                 final CompilationUnit cu) {
        final var scope = fieldAccessExpression.getScope().accept(this, cu);
        final var field = fieldAccessExpression.getField().accept(this, cu);
        return new FieldAccessExpression(
                scope,
                field
        );
    }

    @Override
    public Expression visitNameExpression(final NameExpression nameExpression,
                                          final CompilationUnit cu) {
        return new NameExpression(nameExpression.getName());
    }

    @Override
    public Expression visitMethodCall(final MethodCallExpression methodCallExpression,
                                      final CompilationUnit cu) {
        final var target = methodCallExpression.getTarget()
                .map(it -> it.accept(this, cu))
                .orElse(null);

        return new MethodCallExpression(
                target,
                methodCallExpression.getMethodName(),
                methodCallExpression.getArguments().stream()
                        .map(argument -> argument.accept(this, cu))
                        .toList()
        );
    }

    @Override
    public Expression visitVariableDeclarationExpression(final VariableDeclarationExpression variableDeclarationExpression,
                                                         final CompilationUnit cu) {
        final var initExpression = variableDeclarationExpression.getInitExpression()
                .map(it -> it.accept(this, cu))
                .orElse(null);

        final var type = variableDeclarationExpression.getType().accept(this, cu);

        return new VariableDeclarationExpression(
                variableDeclarationExpression.getModifiers(),
                type,
                variableDeclarationExpression.getName(),
                initExpression
        );
    }


    @Override
    public Expression visitLiteralExpression(final LiteralExpression literalExpression,
                                             final CompilationUnit cu) {
        if (literalExpression.getLiteralType() == LiteralType.CLASS) {
            var type = ((ClassLiteralExpression) literalExpression).getType();

            if (type.isDeclaredType()) {
                final var declaredType = (DeclaredType) type;
                final var className = declaredType.getElement().getQualifiedName();
                importClass(className, cu);
            }
            return LiteralExpression.createClassLiteralExpression(type);
        } else {
            return literalExpression;
        }
    }

    @Override
    public Expression visitNamedMethodArgumentExpression(final NamedMethodArgumentExpression namedMethodArgumentExpression,
                                                         final CompilationUnit cu) {
        return new NamedMethodArgumentExpression(
                namedMethodArgumentExpression.getName(),
                namedMethodArgumentExpression.getArgument().accept(this, cu)
        );
    }

    @Override
    public AnnotationValue visitAnnotation(final AnnotationMirror annotation,
                                           final CompilationUnit cu) {
        importClass(annotation.getAnnotationClassName(), cu);
        annotation.getElementValues().values()
                .forEach(expression -> expression.accept(this, cu));
        return (AnnotationValue) annotation;
    }

    @Override
    public Expression visitArrayAccessExpression(final ArrayAccessExpression arrayAccessExpression,
                                                 final CompilationUnit cu) {
        final var arrayExpression = arrayAccessExpression.getArrayExpression();
        final var indexExpression = arrayAccessExpression.getIndexExpression();
        return new ArrayAccessExpression(arrayExpression, indexExpression);
    }

    @Override
    public Expression visitArrayInitializerExpression(final ArrayInitializerExpression arrayInitializerExpression,
                                                      final CompilationUnit cu) {
        final var newValues = arrayInitializerExpression.getValues().stream()
                .map(it -> it.accept(this, cu))
                .toList();
        return new ArrayInitializerExpression(newValues);
    }

    @Override
    public Expression visitNewClassExpression(final NewClassExpression newClassExpression,
                                              final CompilationUnit cu) {
        final var newClassType = (DeclaredType) newClassExpression.getClassType().accept(this, cu);
        return new NewClassExpression(newClassType);
    }

    //Statements
    @Override
    public Statement visitUnknown(final Statement statement,
                                  final CompilationUnit cu) {
        log("visitUnknown " + statement);
        return new ErrorStatement();
    }

    @Override
    public Statement visitBlockStatement(final BlockStatement blockStatement,
                                         final CompilationUnit cu) {
        final var statements = blockStatement.getStatements().stream()
                .map(it -> it.accept(this, cu))
                .toList();
        return new BlockStatement(statements);
    }

    @Override
    public Statement visitExpressionStatement(final ExpressionStatement expressionStatement,
                                              final CompilationUnit cu) {
        final var expression = expressionStatement.getExpression().accept(this, cu);
        return new ExpressionStatement(expression);
    }

    @Override
    public Statement visitReturnStatement(final ReturnStatement returnStatement,
                                          final CompilationUnit cu) {
        return new ReturnStatement(returnStatement.getExpression().accept(this, cu));
    }

    @Override
    public Statement visitIfStatement(final IfStatement ifStatement,
                                      final CompilationUnit cu) {
        final var condition = ifStatement.getCondition().accept(this, cu);
        final var body = (BlockStatement) ifStatement.getBody().accept(this, cu);
        return new IfStatement(condition, body);
    }

    @Override
    public Type<?> visitDeclaredType(final DeclaredType declaredType,
                                     final CompilationUnit compilationUnit) {
        final var typeArgumentOptional = declaredType.getTypeArguments();
        final var typeArgs = typeArgumentOptional.map(declaredTypes -> declaredTypes.stream()
                .map(it -> it.accept(this, compilationUnit))
                .toList()).orElse(null);
        final var annotations = declaredType.getAnnotations().stream()
                .map(annotation -> (AnnotationMirror) annotation.accept(this, compilationUnit))
                .toList();

        importClass(declaredType.getElement().getQualifiedName(), compilationUnit);

        return TypeFactory.createDeclaredType(
                declaredType.getElement(),
                annotations,
                typeArgs,
                declaredType.isNullable()
        );
    }

    @Override
    public Type<?> visitBooleanType(final PrimitiveType booleanType,
                                    final CompilationUnit cu) {
        return booleanType;
    }

    @Override
    public Type<?> visitByteType(final PrimitiveType byteType,
                                 final CompilationUnit cu) {
        return byteType;
    }

    @Override
    public Type<?> visitCharType(final PrimitiveType charType,
                                 final CompilationUnit cu) {
        return charType;
    }

    @Override
    public Type<?> visitDoubleType(final PrimitiveType doubleType,
                                   final CompilationUnit cu) {
        return doubleType;
    }

    @Override
    public Type<?> visitFloatType(final PrimitiveType floatType,
                                  final CompilationUnit cu) {
        return floatType;
    }

    @Override
    public Type<?> visitPackageType(final PackageType packageType,
                                    final CompilationUnit cu) {
        return packageType;
    }

    @Override
    public Type<?> visitIntType(final PrimitiveType intType,
                                final CompilationUnit cu) {
        return intType;
    }

    @Override
    public Type<?> visitLongType(final PrimitiveType longType,
                                 final CompilationUnit cu) {
        return longType;
    }

    @Override
    public Type<?> visitShortType(final PrimitiveType shortType,
                                  final CompilationUnit cu) {
        return shortType;
    }

    @Override
    public Type<?> visitJavaArrayType(final JavaArrayType javaArrayType,
                                      final CompilationUnit cu) {
        final var newComponentType = javaArrayType.getComponentType().accept(this, cu);
        return typeUtils.createArray(newComponentType);
    }

    @Override
    public Type<?> visitKotlinArray(final KotlinArrayType kotlinArrayType, final CompilationUnit cu) {
        final var newComponentType = kotlinArrayType.getComponentType().accept(this, cu);
        return typeUtils.createKotlinArray(newComponentType);
    }

    @Override
    public Type<?> visitExecutableType(final ExecutableType executableType,
                                       final CompilationUnit cu) {
        return executableType;
    }

    @Override
    public Type<?> visitWildcardType(final WildcardType wildcardType,
                                     final CompilationUnit cu) {
        if (wildcardType.getExtendsBound().isPresent()) {
            final var newExtendsBound = (Type<TypeElement>) wildcardType.getExtendsBound().get().accept(this, cu);
            return WildcardType.withExtendsBound(newExtendsBound);
        } else if (wildcardType.getSuperBound().isPresent()) {
            final var newSuperBound = (Type<TypeElement>) wildcardType.getSuperBound().get().accept(this, cu);
            return WildcardType.withSuperBound(newSuperBound);
        } else {
            return wildcardType;
        }
    }

    @Override
    public Type<?> visitUnknownType(final Type<?> type,
                                    final CompilationUnit cu) {
        log("visitUnknown " + type);
        return typeUtils.createErrorType();
    }

    private List<AnnotationMirror> processAnnotations(final List<AnnotationMirror> annotations,
                                                      final CompilationUnit compilationUnit) {
        return annotations.stream()
                .map(annotation -> processAnnotation(annotation, compilationUnit))
                .toList();
    }

    private AnnotationMirror processAnnotation(final AnnotationMirror annotation,
                                               final CompilationUnit compilationUnit) {
        final String className = annotation.getAnnotationClassName();
        importClass(annotation.getAnnotationClassName(), compilationUnit);

        final String annotationName;

        if (annotation instanceof KotlinAnnotationMirror ka) {
            annotationName = ka.getPrefix() + ":" + className;
        } else {
            annotationName = className;
        }

        final var elementValues = annotation.getElementValues().entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), entry.getValue().accept(this, compilationUnit)))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));

        return Attribute.compound(
                annotationName,
                elementValues
        );
    }

    private void importClass(final String classname,
                             final CompilationUnit compilationUnit) {
        if (!classname.contains(".")) {
            return;
        }

        final var qualifiedName = QualifiedName.from(classname);

        if (qualifiedName.packageName().equals("java.lang")
                || qualifiedName.packageName().equals("kotlin")) {
            return;
        }

        if (matchesPackage(qualifiedName.toString(), compilationUnit)) {
            return;
        }

        final var imports = compilationUnit.getImports();

        final var simpleName = QualifiedName.from(classname).simpleName();
        final var simpleNameWithDot = "." + simpleName;

        for (final var anImport : imports) {
            if (classname.equals(anImport)) {
                return;
            } else if (anImport.endsWith(simpleNameWithDot)) {
                return;
            }
        }

        if (imports.stream()
                .noneMatch(importStr -> classname.equals(importStr) || importStr.endsWith(simpleNameWithDot))) {
            compilationUnit.addImport(classname);
        }
    }

    private boolean matchesPackage(final String classname,
                                   final CompilationUnit compilationUnit) {
        final var qualifiedName = QualifiedName.from(classname);
        final var packageElement = compilationUnit.getPackageElement();
        final var packageName = packageElement.getQualifiedName();
        return qualifiedName.packageName().equals(packageName);
    }

    @Override
    public AnnotationValue visitBoolean(final boolean value, final CompilationUnit param) {
        return Attribute.constant(value);
    }

    @Override
    public AnnotationValue visitInt(final int value, final CompilationUnit param) {
        return Attribute.constant(value);
    }

    @Override
    public AnnotationValue visitLong(final long value, final CompilationUnit param) {
        return Attribute.constant(value);
    }

    @Override
    public AnnotationValue visitString(final String value, final CompilationUnit param) {
        return Attribute.constant(value);
    }

    @Override
    public AnnotationValue visitArray(final Attribute[] array, final CompilationUnit param) {
        return Attribute.array(array);
    }

    @Override
    public AnnotationValue visitEnum(final VariableElement variableElement, final CompilationUnit param) {
        return Attribute.createEnumAttribute(variableElement);
    }

    @Override
    public AnnotationValue visitType(final Type<?> classType, final CompilationUnit param) {
        return Attribute.clazz(classType);
    }

    private void log(final String message) {
        System.out.println(getClass().getName() + " " + message);
    }

}
