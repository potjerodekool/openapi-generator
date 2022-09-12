package io.github.potjerodekool.openapi.internal.ast;

import io.github.potjerodekool.openapi.Language;
import io.github.potjerodekool.openapi.internal.ast.element.*;
import io.github.potjerodekool.openapi.internal.ast.expression.*;
import io.github.potjerodekool.openapi.internal.ast.expression.kotlin.KotlinAnnotationExpression;
import io.github.potjerodekool.openapi.internal.ast.statement.*;
import io.github.potjerodekool.openapi.internal.ast.type.*;
import io.github.potjerodekool.openapi.internal.ast.type.kotlin.KotlinArray;
import io.github.potjerodekool.openapi.internal.util.Utils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class JavaToKotlinConverter implements ElementVisitor<Void, CodeContext>,
        StatementVisitor<Statement, CodeContext>,
        ExpressionVisitor<Expression, CodeContext>,
        TypeVisitor<Type<?>, CodeContext> {

    private static final Map<String, String> JAVA_TO_KOTLIN_TYPE = Map.of(
            "java.lang.String", "kotlin.String",
            "java.lang.Integer", "kotlin.Int",
            "java.lang.Long", "kotlin.Long",
            "java.lang.Object", "kotlin.Any",
            "java.util.List", "kotlin.collections.MutableList",
            "java.util.Map", "kotlin.collections.MutableMap"
    );

    private final TypeUtils typeUtils;

    public JavaToKotlinConverter(final TypeUtils typeUtils) {
        this.typeUtils = typeUtils;
    }

    public CompilationUnit convert(final CompilationUnit compilationUnit) {
        if (compilationUnit.getLanguage() == Language.KOTLIN) {
            return compilationUnit;
        }
        final var newCompilationUnit = new CompilationUnit(Language.KOTLIN);
        newCompilationUnit.setPackageElement(compilationUnit.getPackageElement());

        final var codeContext = new CodeContext(newCompilationUnit);

        compilationUnit.getElements()
                        .forEach(element -> element.accept(this, codeContext));
        return newCompilationUnit;
    }

    //Elements
    @Override
    public Void visitUnknown(final Element element, final CodeContext context) {
        return null;
    }

    @Override
    public Void visitPackageElement(final PackageElement packageElement, final CodeContext context) {
        return null;
    }

    @Override
    public Void visitTypeElement(final TypeElement typeElement, final CodeContext context) {
        if (isUtilityClass(typeElement)) {
            typeElement.getEnclosedElements().stream()
                    .filter(element -> element.getKind() != ElementKind.CONSTRUCTOR)
                    .forEach(element -> element.accept(this, context));
        } else {
            final var parentAstNode = context.getAstNode();

            context.setAstNode(typeElement);

            typeElement.removeModifier(Modifier.PUBLIC);
            final var primaryConstructor = convertFirstConstructorToPrimaryConstructor(typeElement, context);
            removeFieldsWithGetterAndSetters(typeElement, primaryConstructor);

            final var methods = typeElement.getEnclosedElements().stream()
                    .filter(element -> element.getKind() == ElementKind.METHOD)
                    .toList();

            methods.forEach(typeElement::removeEnclosedElement);
            methods.forEach(method -> {
                context.setAstNode(typeElement);
                method.accept(this, context);
            });
            context.setAstNode(typeElement);

            if (parentAstNode instanceof CompilationUnit cu) {
                cu.addElement(typeElement);
            }
        }
        return null;
    }

    @Override
    public Void visitExecutableElement(final MethodElement methodElement,
                                       final CodeContext context) {
        final var parentAstNode = context.getAstNode();

        final var newMethod = methodElement.getKind() == ElementKind.METHOD
                ? MethodElement.createMethod(methodElement.getSimpleName())
                : MethodElement.createConstructor(methodElement.getSimpleName());

        context.setAstNode(newMethod);

        methodElement.getParameters()
                .forEach(parameter -> parameter.accept(this, context));

        methodElement.getBody().ifPresent(body -> {
            final var newBody = (BlockStatement) body.accept(this, context);
            newMethod.setBody(newBody);
        });

        newMethod.addAnnotations(
                methodElement.getAnnotations().stream()
                        .map(annotation -> (AnnotationExpression) annotation.accept(this, context))
                        .toList()
        );

        newMethod.addModifiers(convertModifiers(methodElement.getModifiers(),
                it -> !(it == Modifier.DEFAULT || it == Modifier.STATIC)));

        newMethod.setReturnType(methodElement.getReturnType().accept(this, context));

        if (parentAstNode instanceof TypeElement te) {
            te.addEnclosedElement(newMethod);
        } else if (parentAstNode instanceof CompilationUnit cu) {
            cu.addElement(newMethod);
        }

        return null;
    }

    @Override
    public Void visitVariableElement(final VariableElement variableElement,
                                     final CodeContext context) {
        final VariableElement newVariableElement;

        if (variableElement.getKind() == ElementKind.PARAMETER) {
            newVariableElement = VariableElement.createParameter(
                    variableElement.getSimpleName(),
                    variableElement.getType().accept(this, context)
            );
        } else {
            throw new UnsupportedOperationException();
        }

        if (newVariableElement.getKind() == ElementKind.PARAMETER) {
            final var method = (MethodElement) context.getAstNode();
            method.addParameter(newVariableElement);
        }

        return null;
    }

    private boolean isUtilityClass(final TypeElement typeElement) {
        return typeElement.getEnclosedElements().stream()
                .filter(element -> element.getKind() != ElementKind.CONSTRUCTOR)
                .allMatch(element ->
                        element.getKind() == ElementKind.METHOD
                        && element.hasModifier(Modifier.STATIC)
                );
    }

    private @Nullable MethodElement convertFirstConstructorToPrimaryConstructor(final TypeElement typeElement,
                                                                                final CodeContext context) {
        final var firstConstructor = removeConstructors(typeElement);

        final var fields = ElementFilter.fields(typeElement).toList();

        if (typeElement.getKind() != ElementKind.INTERFACE) {
            final var primaryConstructor = typeElement.addPrimaryConstructor();

            fields.forEach(field -> primaryConstructor.addParameter(createParameter(field, context)));

            if (shouldGenerateOverloads(primaryConstructor)) {
                primaryConstructor.addAnnotation("kotlin.jvm.JvmOverloads");
            }
        }

        return firstConstructor;
    }

    private boolean shouldGenerateOverloads(final MethodElement constructor) {
        return constructor.getParameters().stream()
                .anyMatch(parameter -> parameter.getInitExpression() != null);
    }

    private VariableElement createParameter(final VariableElement field,
                                            final CodeContext context) {
        final var paramType = field.getType().accept(this, context);

        final VariableElement parameter = VariableElement.createParameter(
                field.getSimpleName(),
                paramType
        );

        if (paramType.isNullable()) {
            parameter.setInitExpression(LiteralExpression.createNullLiteralExpression());
        }

        convertModifiers(
                field.getModifiers().stream()
                        .filter(modifier -> modifier != Modifier.PRIVATE)
                        .collect(Collectors.toSet())
        ).forEach(parameter::addModifier);

        if (!field.isFinal()) {
            parameter.addModifiers(Modifier.VAR);
        }

        field.getAnnotations().stream()
                .map(this::toFieldAnnotation)
                .forEach(parameter::addAnnotation);
        return parameter;
    }

    private Set<Modifier> convertModifiers(final Set<Modifier> modifiers) {
        return convertModifiers(modifiers, it -> true);
    }

    private Set<Modifier> convertModifiers(final Set<Modifier> modifiers,
                                           final Predicate<Modifier> filter) {
        final var convertedModifiers = modifiers.stream()
                .filter(filter)
                .filter(modifier -> modifier != Modifier.PUBLIC)
                .map(modifier -> {
                    if (modifier == Modifier.FINAL) {
                        return Modifier.VAL;
                    } else {
                        return modifier;
                    }
                })
                .collect(Collectors.toCollection(HashSet::new));

        if (convertedModifiers.contains(Modifier.VAR) &&
                convertedModifiers.contains(Modifier.VAL)) {
            convertedModifiers.remove(Modifier.VAR);
        }
        return Collections.unmodifiableSet(convertedModifiers);
    }

    private AnnotationExpression toFieldAnnotation(final AnnotationExpression annotation) {
        return new KotlinAnnotationExpression(
                "field",
                annotation.getAnnotationClassName(),
                annotation.getElementValues()
        );
    }

    private @Nullable MethodElement removeConstructors(final TypeElement typeElement) {
        final var constructors = ElementFilter.constructors(typeElement)
                .toList();

        if (constructors.isEmpty()) {
            return null;
        }

        final var firstConstructor = constructors.get(0);

        constructors.forEach(constructor -> {
            typeElement.removeEnclosedElement(constructor);
            constructor.setBody(null);
        });

        return firstConstructor;
    }

    private void removeFieldsWithGetterAndSetters(final TypeElement typeElement,
                                                  final @Nullable MethodElement primaryConstructor) {
        if (primaryConstructor == null) {
            return;
        }

        final var fields = ElementFilter.fields(typeElement).toList();
        removeGetterAndSetters(typeElement, fields);
        fields.forEach(typeElement::removeEnclosedElement);
    }

    private void removeGetterAndSetters(final TypeElement typeElement,
                                        final List<VariableElement> fields) {

        final var fieldAndTypeMap = fields.stream()
                .collect(Collectors.toMap(
                        AbstractElement::getSimpleName,
                        VariableElement::getType
                ));

        final var getterNames = new ArrayList<String>();
        final var setterNames = new ArrayList<String>();

        fields.forEach(field -> {
            getterNames.add("get" + Utils.firstUpper(field.getSimpleName()));
            getterNames.add(field.getSimpleName());
        });

        fields.forEach(field -> {
            setterNames.add("set" + Utils.firstUpper(field.getSimpleName()));
            setterNames.add(field.getSimpleName());
        });

        var methods = ElementFilter.methods(typeElement).toList();

        methods.stream().filter(this::isGetter)
                .filter(getter -> getterNames.contains(getter.getSimpleName()))
                .filter(getter -> {
                    final String getterName = getter.getSimpleName();
                    final String fieldName = Utils.firstLower(getterName.startsWith("get") ? getterName.substring(3) : getterName);
                    final var fieldType = fieldAndTypeMap.get(fieldName);
                    return fieldType != null && getter.getReturnType().isSameType(fieldType);
                }).forEach(typeElement::removeEnclosedElement);

        methods.stream().filter(this::isSetter)
                .filter(setter -> setterNames.contains(setter.getSimpleName()))
                .filter(setter -> {
                    final String setterName = setter.getSimpleName();
                    final String fieldName = Utils.firstLower(setterName.startsWith("set") ? setterName.substring(3) : setterName);
                    final var fieldType = fieldAndTypeMap.get(fieldName);
                    return fieldType != null && setter.getParameters().get(0).getType().isSameType(fieldType);
                }).forEach(typeElement::removeEnclosedElement);
    }

    private boolean isGetter(final MethodElement method) {
        final var returnType = method.getReturnType();

        if (returnType.getKind() == TypeKind.VOID) {
            return false;
        }

        return method.getParameters().isEmpty();
    }

    private boolean isSetter(final MethodElement method) {
        return method.getParameters().size() == 1;
    }

    //Statements

    @Override
    public Statement visitUnknown(final Statement statement, final CodeContext context) {
        return statement;
    }

    @Override
    public Statement visitExpressionStatement(final ExpressionStatement expressionStatement, final CodeContext context) {
        final var convertedExpression = expressionStatement.getExpression()
                .accept(this, context);
        return new ExpressionStatement(convertedExpression);
    }

    @Override
    public Statement visitBlockStatement(final BlockStatement blockStatement, final CodeContext context) {
        final var newStatements = blockStatement.getStatements().stream()
                .map(statement -> statement.accept(this, context))
                .toList();
        return new BlockStatement(newStatements);
    }

    @Override
    public Statement visitReturnStatement(final ReturnStatement returnStatement, final CodeContext context) {
        final var expression = returnStatement.getExpression().accept(this, context);
        return new ReturnStatement(expression);
    }

    @Override
    public Statement visitIfStatement(final IfStatement ifStatement, final CodeContext context) {
        final var childContext = context.child();
        final var condition = ifStatement.getCondition().accept(this, childContext);
        final var body = (BlockStatement) ifStatement.getBody().accept(this, childContext);
        return new IfStatement(condition, body);
    }

    //Expressions
    @Override
    public Expression visitUnknown(final Expression expression, final CodeContext context) {
        return expression;
    }

    @Override
    public Expression visitMethodCall(final MethodCallExpression methodCallExpression, final CodeContext context) {
        final var target = methodCallExpression.getTarget()
                .map(it -> it.accept(this, context))
                .orElse(null);

        final var convertedArguments = methodCallExpression.getArguments().stream()
                .map(argument -> argument.accept(this, context))
                .toList();

        final var convertedMethodCallOptional = convertMethodCallExpression(
                methodCallExpression.getMethodName(),
                target,
                convertedArguments,
                context
        );

        return convertedMethodCallOptional.orElseGet(() ->
            new MethodCallExpression(target, methodCallExpression.getMethodName(), convertedArguments)
        );
    }

    private Optional<Expression> convertMethodCallExpression(final String methodName,
                                                             final @Nullable Expression target,
                                                             final List<Expression> arguments,
                                                             final CodeContext context) {
        if (target == null) {
            return Optional.empty();
        }

        if (target instanceof NameExpression targetName) {
           final var name = targetName.getName();
           final var targetTypeOptional = context.resolveLocalVariable(name);

           if (targetTypeOptional.isEmpty()) {
               return Optional.empty();
           }

           final var targetType = targetTypeOptional.get();

           if (!targetType.isDeclaredType()) {
               return Optional.empty();
           }

           final var targetDeclaredType = (DeclaredType) targetType;
           final String classname = targetDeclaredType.getElement().getQualifiedName();

           if (!"java.lang.StringBuffer".equals(classname)) {
               return Optional.empty();
           }
           return convertStringBufferMethodCallExpression(
                   methodName,
                   target,
                   arguments
           );
        }

        return Optional.empty();
    }

    private Optional<Expression> convertStringBufferMethodCallExpression(final String methodName,
                                                                         final Expression target,
                                                                         final List<Expression> arguments) {
        if ("charAt".equals(methodName)) {
            final var convertedExpression = arguments.get(0);
            return Optional.of(new ArrayAccessExpression(target, convertedExpression));
        } else if ("length".equals(methodName)) {
            return Optional.of(new FieldAccessExpression(
                    target,
                    "length"
            ));
        }

        return Optional.empty();
    }


    @Override
    public Expression visitVariableDeclarationExpression(final VariableDeclarationExpression variableDeclarationExpression, final CodeContext context) {
        final var initExpression = variableDeclarationExpression.getInitExpression()
                .map(it -> it.accept(this, context))
                .orElse(null);
        final var modifiers = convertModifiers(variableDeclarationExpression.getModifiers());

        final var localVariableName = variableDeclarationExpression.getName();
        final var newLocalVariableType = variableDeclarationExpression.getType().accept(this, context);

        context.defineLocalVariable(newLocalVariableType, localVariableName);

        return new VariableDeclarationExpression(
                modifiers,
                newLocalVariableType,
                localVariableName,
                initExpression
        );
    }

    @Override
    public Expression visitNamedMethodArgumentExpression(final NamedMethodArgumentExpression namedMethodArgumentExpression, final CodeContext context) {
        return namedMethodArgumentExpression;
    }

    @Override
    public Expression visitNameExpression(final NameExpression nameExpression, final CodeContext context) {
        return nameExpression;
    }

    @Override
    public Expression visitFieldAccessExpression(final FieldAccessExpression fieldAccessExpression, final CodeContext context) {
        return fieldAccessExpression;
    }

    @Override
    public Expression visitBinaryExpression(final BinaryExpression binaryExpression, final CodeContext context) {
        final var left = binaryExpression.getLeft().accept(this, context);
        final var right = binaryExpression.getRight().accept(this, context);
        return new BinaryExpression(left, right, binaryExpression.getOperator());
    }

    @Override
    public Expression visitLiteralExpression(final LiteralExpression literalExpression, final CodeContext context) {
        if (literalExpression.getLiteralType() == LiteralType.CLASS) {
            final var kotlinClassName = JAVA_TO_KOTLIN_TYPE.get(literalExpression.getValue());
            if (kotlinClassName != null) {
                return LiteralExpression.createClassLiteralExpression(kotlinClassName);
            }
        }
        return literalExpression;
    }

    @Override
    public Expression visitAnnotationExpression(final AnnotationExpression annotationExpression, final CodeContext context) {
        final var convertedElementValues = annotationExpression.getElementValues().entrySet().stream()
                .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            it -> it.getValue().accept(this, context)
                        )
                );

        return new AnnotationExpression(
                annotationExpression.getAnnotationClassName(),
                convertedElementValues
        );
    }

    @Override
    public Expression visitArrayAccessExpression(final ArrayAccessExpression arrayAccessExpression, final CodeContext context) {
        final var arrayExpression = arrayAccessExpression.getArrayExpression().accept(this, context);
        final var indexExpression = arrayAccessExpression.getIndexExpression().accept(this, context);
        return new ArrayAccessExpression(arrayExpression, indexExpression);
    }

    @Override
    public Expression visitArrayInitializerExpression(final ArrayInitializerExpression arrayInitializerExpression, final CodeContext context) {
        final var newValues = arrayInitializerExpression.getValues().stream()
                .map(it -> it.accept(this, context))
                .toList();
        return new ArrayInitializerExpression(newValues);
    }

    @Override
    public Expression visitNewClassExpression(final NewClassExpression newClassExpression, final CodeContext context) {
        final var newClassType = (DeclaredType) newClassExpression.getClassType().accept(this, context);
        return new NewClassExpression(newClassType);
    }

    //Types
    @Override
    public Type<?> visitUnknownType(final Type<?> type,
                                    final CodeContext context) {
        return type;
    }

    @Override
    public Type<?> visitVoidType(final VoidType voidType,
                                 final CodeContext context) {
        return new UnitType();
    }

    @Override
    public Type<?> visitPackageType(final PackageType packageType,
                                    final CodeContext context) {
        return packageType;
    }

    @Override
    public Type<?> visitDeclaredType(final DeclaredType declaredType,
                                     final CodeContext context) {
        final var qualifiedName = declaredType.getElement().getQualifiedName();
        final var kotlinTypeName = JAVA_TO_KOTLIN_TYPE.get(qualifiedName);
        final DeclaredType convertedType;

        if (kotlinTypeName != null) {
            final var type = typeUtils.createDeclaredType(kotlinTypeName);
            convertedType = declaredType.isNullable() ? type.asNullableType() : type;
        } else {
            convertedType = declaredType;
        }

        final var typeArgumentsOptional = declaredType.getTypeArguments();

        if (typeArgumentsOptional.isPresent()) {
            final var typeArguments = typeArgumentsOptional.get();
            final var convertedTypeArgs = typeArguments.stream()
                    .map(typeArgument -> typeArgument.accept(this, context))
                    .toList();
            return convertedType.withTypeArguments(convertedTypeArgs);
        } else {
            return convertedType;
        }
    }

    @Override
    public Type<?> visitExecutableType(final ExecutableType executableType,
                                       final CodeContext context) {
        return executableType;
    }

    @Override
    public Type<?> visitJavaArrayType(final JavaArrayType javaArrayType,
                                      final CodeContext context) {
        final var componentType = javaArrayType.getComponentType();

        if (componentType.isPrimitiveType()) {
            switch (componentType.getKind()) {
                case BYTE,
                     CHAR,
                     SHORT,
                     INT,
                     LONG,
                     FLOAT,
                     DOUBLE,
                     BOOLEAN -> {
                    return new KotlinArray(componentType);
                }
            }
        }

        return new KotlinArray(componentType);
    }

    @Override
    public Type<?> visitBooleanType(final PrimitiveType booleanType,
                                    final CodeContext context) {
        return typeUtils.createDeclaredType("kotlin.Boolean");
    }

    @Override
    public Type<?> visitByteType(final PrimitiveType byteType,
                                 final CodeContext context) {
        return typeUtils.createDeclaredType("kotlin.Byte");
    }

    @Override
    public Type<?> visitShortType(final PrimitiveType shortType,
                                  final CodeContext context) {
        return typeUtils.createDeclaredType("kotlin.Short");
    }

    @Override
    public Type<?> visitIntType(final PrimitiveType intType,
                                final CodeContext context) {
        return typeUtils.createDeclaredType("kotlin.Int");
    }

    @Override
    public Type<?> visitLongType(final PrimitiveType longType,
                                 final CodeContext context) {
        return typeUtils.createDeclaredType("kotlin.Long");
    }

    @Override
    public Type<?> visitCharType(final PrimitiveType charType,
                                 final CodeContext context) {
        return typeUtils.createDeclaredType("kotlin.Char");
    }

    @Override
    public Type<?> visitFloatType(final PrimitiveType floatType,
                                  final CodeContext context) {
        return typeUtils.createDeclaredType("kotlin.Float");
    }

    @Override
    public Type<?> visitDoubleType(final PrimitiveType doubleType,
                                   final CodeContext context) {
        return typeUtils.createDeclaredType("kotlin.Double");
    }

    @Override
    public Type<?> visitWildcardType(final WildcardType wildcardType,
                                     final CodeContext context) {
        return wildcardType;
    }

    @Override
    public Type<?> visitUnitType(final UnitType unitType,
                                 final CodeContext context) {
        return unitType;
    }
}
