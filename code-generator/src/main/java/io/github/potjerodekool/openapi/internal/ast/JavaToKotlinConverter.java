package io.github.potjerodekool.openapi.internal.ast;

import io.github.potjerodekool.openapi.Language;
import io.github.potjerodekool.openapi.internal.ast.element.*;
import io.github.potjerodekool.openapi.internal.ast.expression.*;
import io.github.potjerodekool.openapi.internal.ast.statement.*;
import io.github.potjerodekool.openapi.internal.ast.type.*;
import io.github.potjerodekool.openapi.internal.ast.type.java.*;
import io.github.potjerodekool.openapi.internal.ast.type.kotlin.KotlinArrayType;
import io.github.potjerodekool.openapi.internal.ast.type.kotlin.UnitType;
import io.github.potjerodekool.openapi.internal.ast.util.TypeUtils;
import io.github.potjerodekool.openapi.internal.util.Utils;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeKind;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class JavaToKotlinConverter implements ElementVisitor<Void, CodeContext>,
        StatementVisitor<Statement, CodeContext>,
        ExpressionVisitor<Expression, CodeContext>,
        TypeVisitor<Type<?>, CodeContext>,
        AnnotationValueVisitor<Object, CodeContext> {

    private static final Map<String, String> JAVA_TO_KOTLIN_TYPE = Map.ofEntries(
            Map.entry("java.lang.String", "kotlin.String"),
            Map.entry("java.lang.Integer", "kotlin.Int"),
            Map.entry("java.lang.Boolean", "kotlin.Boolean"),
            Map.entry("java.lang.Byte", "kotlin.Byte"),
            Map.entry("java.lang.Short", "kotlin.Short"),
            Map.entry("java.lang.Character", "kotlin.Char"),
            Map.entry("java.lang.Float", "kotlin.Float"),
            Map.entry("java.lang.Double", "kotlin.Double"),
            Map.entry("java.lang.Long", "kotlin.Long"),
            Map.entry("java.lang.Object", "kotlin.Any"),
            Map.entry("java.util.List", "kotlin.collections.MutableList"),
            Map.entry("java.util.Map", "kotlin.collections.MutableMap")
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

            final var childContext = context.child(typeElement);

            typeElement.removeModifier(Modifier.PUBLIC);
            final var primaryConstructor = convertFirstConstructorToPrimaryConstructor(typeElement, childContext);

            final var enclosedElements = typeElement.getEnclosedElements().stream().toList();
            enclosedElements.forEach(enclosedElement -> enclosedElement.accept(this, childContext));
            removeFieldsWithGetterAndSetters(typeElement, primaryConstructor);

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
                ? MethodElement.createMethod(methodElement.getSimpleName(), methodElement.getReturnType().accept(this, context))
                : MethodElement.createConstructor(methodElement.getSimpleName());

        final var childContext = context.child(newMethod);

        methodElement.getParameters()
                .forEach(parameter -> parameter.accept(this, childContext));

        methodElement.getBody().ifPresent(body -> {
            final var newBody = (BlockStatement) body.accept(this, childContext);
            newMethod.setBody(newBody);
        });

        newMethod.addAnnotations(
                methodElement.getAnnotations().stream()
                        .map(annotation -> (AnnotationMirror) annotation.accept(this, childContext))
                        .toList()
        );

        newMethod.addModifiers(convertModifiers(methodElement.getModifiers(),
                it -> !(it == Modifier.DEFAULT || it == Modifier.STATIC)));

        if (parentAstNode instanceof TypeElement te) {
            te.removeEnclosedElement(methodElement);
            te.addEnclosedElement(newMethod);
        } else if (parentAstNode instanceof CompilationUnit cu) {
            cu.removeElement(methodElement);
            cu.addElement(newMethod);
        }

        return null;
    }

    @Override
    public Void visitVariableElement(final VariableElement variableElement,
                                     final CodeContext context) {
        final VariableElement newVariableElement;
        final var annotations = variableElement.getAnnotations().stream()
                .map(annotation -> (AnnotationMirror) annotation.accept(this, context))
                .toList();

        if (variableElement.getKind() == ElementKind.PARAMETER) {
            newVariableElement = VariableElement.createParameter(
                    variableElement.getSimpleName(),
                    variableElement.getType().accept(this, context)
            );
        } else if (variableElement.getKind() == ElementKind.FIELD) {
            newVariableElement = VariableElement.createField(
                    variableElement.getSimpleName(),
                    variableElement.getType().accept(this, context)
            );
        } else {
            throw new UnsupportedOperationException();
        }

        newVariableElement.addAnnotations(annotations);

        if (newVariableElement.getKind() == ElementKind.PARAMETER) {
            final var method = (MethodElement) context.getAstNode();
            method.addParameter(newVariableElement);
        } else if (newVariableElement.getKind() == ElementKind.FIELD) {
            final var clazz = (TypeElement) context.getAstNode();
            clazz.removeEnclosedElement(variableElement);
            clazz.addEnclosedElement(newVariableElement);
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
        } else {
            parameter.setInitExpression(createDefaultParameterValue(paramType));
        }

        convertModifiers(
                field.getModifiers(),
                modifier -> modifier != Modifier.PRIVATE
        ).forEach(parameter::addModifier);

        if (!field.isFinal()) {
            parameter.addModifiers(Modifier.VAR);
        }

        field.getAnnotations().stream()
                .map(annotation -> toFieldAnnotation(annotation, context))
                .forEach(parameter::addAnnotation);
        return parameter;
    }

    private @Nullable Expression createDefaultParameterValue(final Type<?> parameterType) {
        if (parameterType.isDeclaredType()) {
            final var declaredType = (DeclaredType) parameterType;
            final var qualifiedName = declaredType.getElement().getQualifiedName();
            if ("org.openapitools.jackson.nullable.JsonNullable".equals(qualifiedName)) {
                return new MethodCallExpression(
                        new NameExpression("org.openapitools.jackson.nullable.JsonNullable"),
                        "undefined"
                );
            }
        } else if (parameterType.isArrayType()) {
            final var kotlinArrayType = (KotlinArrayType) parameterType;
            final var componentType = (DeclaredType) kotlinArrayType.getComponentType();
            final var componentTypeName = componentType.getElement().getQualifiedName();
            final var methodName = switch (componentTypeName) {
                case "kotlin.Byte" -> "byteArrayOf";
                case "kotlin.Char" -> "charArrayOf";
                case "kotlin.Short" -> "shortArrayOf";
                case "kotlin.Int" -> "intArrayOf";
                case "kotlin.Long" -> "longArrayOf";
                case "kotlin.Float" -> "floatArrayOf";
                case "kotlin.Double" -> "doubleArrayOf";
                case "kotlin.Boolean" -> "booleanArrayOf";
                default -> "arrayOf";
            };
            return new MethodCallExpression(
                    null,
                    methodName
            );
        }

        return null;
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

    private AnnotationMirror toFieldAnnotation(final AnnotationMirror annotation,
                                                   final CodeContext context) {
        final var elementValues = annotation.getElementValues().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        it -> (AnnotationValue) it.getValue().accept(this, context)
                ));

        return new KotlinAnnotationMirror(
                "field",
                annotation.getAnnotationClassName(),
                elementValues
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

        final var fieldMap = fields.stream()
                .collect(Collectors.toMap(
                        it -> it.getSimpleName().toString(),
                        Function.identity()
                ));

        final var getterNames = new ArrayList<String>();
        final var setterNames = new ArrayList<String>();

        fields.forEach(field -> {
            getterNames.add("get" + Utils.firstUpper(field.getSimpleName().toString()));
            getterNames.add(field.getSimpleName().toString());
        });

        fields.forEach(field -> {
            setterNames.add("set" + Utils.firstUpper(field.getSimpleName().toString()));
            setterNames.add(field.getSimpleName().toString());
        });

        var methods = ElementFilter.methods(typeElement).toList();

        methods.stream().filter(this::isGetter)
                .filter(getter -> getterNames.contains(getter.getSimpleName().toString()))
                .filter(getter -> {
                    final String getterName = getter.getSimpleName().toString();
                    final String fieldName = Utils.firstLower(getterName.startsWith("get") ? getterName.substring(3) : getterName);
                    final var field = fieldMap.get(fieldName);
                    return field != null && getter.getReturnType().isSameType(field.getType());
                }).forEach(typeElement::removeEnclosedElement);

        methods.stream().filter(this::isSetter)
                .filter(setter -> setterNames.contains(setter.getSimpleName().toString()))
                .filter(setter -> {
                    final String setterName = setter.getSimpleName().toString();
                    final var isNormalSetter = setterName.startsWith("set");
                    final String fieldName = Utils.firstLower(isNormalSetter ? setterName.substring(3) : setterName);
                    final var field = fieldMap.get(fieldName);
                    return field != null && setter.getParameters().get(0).getType().isSameType(field.getType()) && isNormalSetter;
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
            final var classLiteral = (ClassLiteralExpression) literalExpression;
            final var type = classLiteral.getType();
            final var convertedType = type.accept(this, context);

            if (convertedType.isPrimitiveType()) {
                throw new UnsupportedOperationException();
            }

            return LiteralExpression.createClassLiteralExpression(convertedType);
        }
        return literalExpression;
    }

    @Override
    public AnnotationMirror visitAnnotation(final AnnotationMirror annotationExpression, final CodeContext context) {
        final var convertedElementValues = annotationExpression.getElementValues().entrySet().stream()
                .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            it -> (AnnotationValue) it.getValue().accept(this, context)
                        )
                );

        return Attribute.compound(
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
        return UnitType.INSTANCE;
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
        final var convertedComponentType = componentType.accept(this, context);

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
                    return typeUtils.createKotlinArray(convertedComponentType);
                }
            }
        }

        return typeUtils.createKotlinArray(convertedComponentType);
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

    @Override
    public Object visitBoolean(final boolean value, final CodeContext param) {
        return Attribute.constant(value);
    }

    @Override
    public Object visitInt(final int value, final CodeContext param) {
        return Attribute.constant(value);
    }

    @Override
    public Object visitLong(final long value, final CodeContext param) {
        return Attribute.constant(value);
    }

    @Override
    public Object visitString(final String value, final CodeContext param) {
        return Attribute.constant(value);
    }

    @Override
    public Object visitEnum(final VariableElement enumValue, final CodeContext param) {
        return Attribute.createEnumAttribute(enumValue);
    }

    @Override
    public Object visitArray(final Attribute[] array, final CodeContext param) {
        return Attribute.array(array);
    }

    @Override
    public Object visitType(final Type<?> type, final CodeContext param) {
        return Attribute.clazz(type);
    }
}
