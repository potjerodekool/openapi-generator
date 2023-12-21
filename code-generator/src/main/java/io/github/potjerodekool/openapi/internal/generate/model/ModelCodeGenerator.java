package io.github.potjerodekool.openapi.internal.generate.model;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.Language;
import io.github.potjerodekool.codegen.model.CompilationUnit;
import io.github.potjerodekool.codegen.model.element.ElementKind;
import io.github.potjerodekool.codegen.model.element.Modifier;
import io.github.potjerodekool.codegen.model.element.Name;
import io.github.potjerodekool.codegen.model.symbol.MethodSymbol;
import io.github.potjerodekool.codegen.model.symbol.VariableSymbol;
import io.github.potjerodekool.codegen.model.tree.AnnotationExpression;
import io.github.potjerodekool.codegen.model.tree.PackageDeclaration;
import io.github.potjerodekool.codegen.model.tree.expression.*;
import io.github.potjerodekool.codegen.model.tree.statement.BlockStatement;
import io.github.potjerodekool.codegen.model.tree.statement.ClassDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.ReturnStatement;
import io.github.potjerodekool.codegen.model.tree.statement.java.JClassDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.java.JVariableDeclaration;
import io.github.potjerodekool.codegen.model.tree.type.*;
import io.github.potjerodekool.codegen.model.type.TypeKind;
import io.github.potjerodekool.codegen.model.type.java.immutable.JavaVoidType;
import io.github.potjerodekool.codegen.model.util.StringUtils;
import io.github.potjerodekool.codegen.resolve.Enter;
import io.github.potjerodekool.codegen.resolve.Resolver;
import io.github.potjerodekool.openapi.generate.model.ModelAdapter;
import io.github.potjerodekool.openapi.internal.ClassNames;
import io.github.potjerodekool.openapi.internal.GenerateException;
import io.github.potjerodekool.openapi.internal.di.ApplicationContext;
import io.github.potjerodekool.openapi.internal.generate.annotation.openapi.extension.ExtensionAnnotationBuilder;
import io.github.potjerodekool.openapi.internal.generate.annotation.openapi.extension.ExtensionPropertyAnnotationBuilder;
import io.github.potjerodekool.openapi.internal.generate.annotation.openapi.media.SchemaAnnotationBuilder;
import io.github.potjerodekool.openapi.internal.type.OpenApiTypeUtils;
import io.github.potjerodekool.openapi.tree.media.OpenApiArraySchema;
import io.github.potjerodekool.openapi.tree.media.OpenApiObjectSchema;
import io.github.potjerodekool.openapi.tree.media.OpenApiSchema;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

public class ModelCodeGenerator {

    private static final String JSON_NULLABLE_CLASS_NAME = "org.openapitools.jackson.nullable.JsonNullable";

    private final OpenApiTypeUtils openApiTypeUtils;
    private final ModelAdapter modelAdapter;
    private final Environment environment;
    private final Enter enter;
    private final Resolver resolver;

    public ModelCodeGenerator(final OpenApiTypeUtils openApiTypeUtils,
                              final Environment environment,
                              final ApplicationContext applicationContext) {
        this.environment = environment;
        this.openApiTypeUtils = openApiTypeUtils;
        this.modelAdapter = new CombinedModelAdapter(applicationContext);
        this.enter = new Enter(environment.getSymbolTable());
        this.resolver = new Resolver(
                environment.getElementUtils(),
                environment.getTypes(),
                environment.getSymbolTable()
        );
    }


    public void generateSchemaModel(final String schemaName,
                                    final OpenApiObjectSchema schema,
                                    final boolean isPatchModel) {
        final var cu = new CompilationUnit(Language.JAVA);
        final var pck = schema.pck();

        final var packageDeclaration = new PackageDeclaration(new IdentifierExpression(pck.getName()));
        cu.add(packageDeclaration);

        final var classDeclaration = new JClassDeclaration(Name.of(schemaName), ElementKind.CLASS)
                .modifier(Modifier.PUBLIC)
                        .annotation(new AnnotationExpression("javax.annotation.processing.Generated", LiteralExpression.createStringLiteralExpression(getClass().getName())));

        cu.add(classDeclaration);
        classDeclaration.setEnclosing(packageDeclaration);

        addFields(schema, classDeclaration, isPatchModel);
        //addConstructors(schema, classDeclaration, isPatchModel);
        generateAccessors(schema, classDeclaration, isPatchModel);

        cu.accept(enter, null);
        resolver.resolve(cu);

        adaptModel(cu);

        environment.getCompilationUnits().add(cu);
    }

    private void adaptModel(final CompilationUnit cu) {
        cu.getClassDeclarations().forEach(classDeclaration -> classDeclaration.getEnclosed().forEach(modelAdapter::adapt));
    }

    private void addConstructors(final OpenApiObjectSchema schema,
                                 final JClassDeclaration classDeclaration,
                                 final boolean isPatchModel) {

        final var generatedDefaultConstructor = schema.required().isEmpty();

        if (generatedDefaultConstructor) {
            final var defaultConstructor = classDeclaration.addConstructor();
            defaultConstructor.addModifiers(Set.of(Modifier.PUBLIC));
            final var constructorSymbol = new MethodSymbol(ElementKind.CONSTRUCTOR, JavaVoidType.INSTANCE, classDeclaration.getSimpleName());
            constructorSymbol.addModifiers(Modifier.PUBLIC);

            defaultConstructor.setMethodSymbol(constructorSymbol);
            defaultConstructor.setBody(new BlockStatement());
        }

        if (isPatchModel
            || schema.properties().isEmpty()) {
            return;
        }

        generateRequiredArgConstructor(classDeclaration, schema);
    }

    private void generateRequiredArgConstructor(final JClassDeclaration classDeclaration,
                                                final OpenApiObjectSchema schema) {
        final var hasRequiredProperties = schema.properties().entrySet().stream()
                .anyMatch(entry -> schema.required().contains(entry.getKey()));

        if (schema.required().isEmpty()
            ||!hasRequiredProperties) {
            return;
        }

        final var requiredArgsConstructor = classDeclaration.addConstructor();
        requiredArgsConstructor.addModifiers(Set.of(Modifier.PUBLIC));
        final var constructorSymbol = new MethodSymbol(ElementKind.CONSTRUCTOR, JavaVoidType.INSTANCE, classDeclaration.getSimpleName());
        constructorSymbol.addModifiers(Modifier.PUBLIC);

        requiredArgsConstructor.setMethodSymbol(constructorSymbol);

        schema.properties().entrySet().stream()
                .filter(entry -> schema.required().contains(entry.getKey()))
                .forEach(entry -> {
                    final var propertyName = entry.getKey();
                    final var property = entry.getValue();
                    final var paramType = openApiTypeUtils.createTypeExpression(property);

                    final var parameter = new JVariableDeclaration(
                            ElementKind.PARAMETER,
                            Set.of(Modifier.FINAL),
                            paramType,
                            propertyName,
                            null,
                            null
                    );

                    final var parameterSymbol =
                            new VariableSymbol(ElementKind.PARAMETER, propertyName);

                    requiredArgsConstructor.addParameter(parameter);
                    parameter.setSymbol(parameterSymbol);

                    constructorSymbol.addParameter(parameterSymbol);
                });

        final var body = new BlockStatement();

        schema.properties().entrySet().stream()
                .filter(entry -> schema.required().contains(entry.getKey()))
                .forEach(entry -> {
                    final var value = new IdentifierExpression(entry.getKey());

                    body.add(new BinaryExpression(
                                    new FieldAccessExpression(
                                            new IdentifierExpression("this"),
                                            entry.getKey()
                                    ),
                                    value,
                                    Operator.ASSIGN
                            )
                    );
                });

        requiredArgsConstructor.setBody(body);
    }

    private void addFields(final OpenApiObjectSchema schema,
                           final ClassDeclaration<?> classDeclaration,
                           final boolean isPatchModel) {
        schema.properties().forEach((propertyName, property) ->
                addField(
                        propertyName,
                        schema.required().contains(propertyName),
                        property,
                        classDeclaration,
                        isPatchModel));
    }

    private void addField(final String propertyName,
                          final boolean required,
                          final OpenApiSchema<?> schema,
                          final ClassDeclaration<?> classDeclaration,
                          final boolean isPatchModel) {
        if (isPatchModel && Boolean.TRUE.equals(schema.readOnly())) {
            return;
        }

        var fieldType = openApiTypeUtils.createTypeExpression(schema);

        if (isPatchModel) {
            if (fieldType instanceof PrimitiveTypeExpression pt) {
                fieldType = toBoxedType(pt);
            }

            fieldType = new ClassOrInterfaceTypeExpression(JSON_NULLABLE_CLASS_NAME, List.of(fieldType));
        }

        final Expression initExpression;

        if (isPatchModel) {
            initExpression = new MethodCallExpression(
                    new ClassOrInterfaceTypeExpression(JSON_NULLABLE_CLASS_NAME),
                    "undefined");
        } else {
            if (required) {
                initExpression = null;
            } else if (fieldType instanceof ClassOrInterfaceTypeExpression
                    && !fieldType.isNullable()) {
                initExpression = null;
            } else {
                initExpression = getDefaultValue(fieldType);
            }
        }

        final var modifiers = new HashSet<Modifier>();
        modifiers.add(Modifier.PRIVATE);

        var field = new JVariableDeclaration(
                ElementKind.FIELD,
                modifiers,
                fieldType,
                propertyName,
                initExpression,
                null
        );

        classDeclaration.addEnclosed(field);

        if (schema instanceof OpenApiArraySchema) {
            //TODO field.addAnnotation(createArraySchemaAnnotation(elementType));
        } else {
            final var type = openApiTypeUtils.createTypeExpression(schema);

            boolean isRequired = required;
            Boolean readOnly = schema.readOnly();
            Boolean writeOnly = schema.writeOnly();

            if (isPatchModel) {
                isRequired = false;
                readOnly = null;
                writeOnly = true;
            }

            field.annotation(createSchemaAnnotation(
                    type,
                    isRequired,
                    schema,
                    readOnly,
                    writeOnly
            ));
        }

        field.setMetaData(OpenApiSchema.class.getSimpleName(), schema);
    }

    private TypeExpression toBoxedType(final PrimitiveTypeExpression primitiveTypeExpression) {
        return switch (primitiveTypeExpression.getKind()) {
            case BOOLEAN -> new ClassOrInterfaceTypeExpression("java.lang.Boolean");
            case BYTE -> new ClassOrInterfaceTypeExpression("java.lang.Byte");
            case SHORT -> new ClassOrInterfaceTypeExpression("java.lang.Short");
            case INT -> new ClassOrInterfaceTypeExpression("java.lang.Integer");
            case LONG -> new ClassOrInterfaceTypeExpression("java.lang.Long");
            case CHAR -> new ClassOrInterfaceTypeExpression("java.lang.Character");
            case FLOAT -> new ClassOrInterfaceTypeExpression("java.lang.Float");
            case DOUBLE -> new ClassOrInterfaceTypeExpression("java.lang.Double");
            default -> throw new IllegalArgumentException(String.format("%s is not a primitive schema", primitiveTypeExpression.getKind()));
        };
    }

    private AnnotationExpression createSchemaAnnotation(final TypeExpression type,
                                                        final boolean required,
                                                        final OpenApiSchema<?> schema,
                                                        final Boolean readOnly,
                                                        final Boolean writeOnly) {
        final var implementationType = openApiTypeUtils.resolveImplementationType(type);
        final var extensions = createExtensions(schema.extensions());

        return new SchemaAnnotationBuilder()
                .implementation(implementationType)
                .requiredMode(required)
                .description(schema.description())
                .accessMode(readOnly, writeOnly)
                .format(schema.format())
                .nullable(schema.nullable())
                .extensions(extensions)
                .build();
    }

    private ArrayInitializerExpression createExtensions(final Map<String, Object> extensions) {
        if (extensions.isEmpty()) {
            return null;
        }

        final var values = new ArrayList<AnnotationExpression>();

        extensions.forEach((String extensionName, Object properties) ->
                values.add(createExtension(extensionName, properties)));

        return new ArrayInitializerExpression(values);
    }

    private AnnotationExpression createExtension(final String extensionName,
                                                 final Object properties) {
        //Strip x-
        final String name = extensionName.substring(2);
        final Map<String, Object> propertyMap = (Map<String, Object>) properties;
        final var propertyList = propertyMap.entrySet().stream()
                .map(entry -> createExtensionProperty(entry.getKey(), entry.getValue()))
                .toList();


        return new ExtensionAnnotationBuilder()
                .name(name)
                .properties(propertyList)
                .build();
    }

    private AnnotationExpression createExtensionProperty(final String name,
                                                         final Object value) {
        return new ExtensionPropertyAnnotationBuilder()
                .name(name)
                .value(Objects.toString(value))
                .build();
    }

    private void generateAccessors(final OpenApiSchema<?> schema,
                                   final JClassDeclaration classDeclaration,
                                   final boolean isPatchModel) {
        schema.properties()
                .forEach((propertyName, property) -> {
                    final var required = schema.required().contains(propertyName);
                    addGetter(propertyName, property, classDeclaration, isPatchModel);
                    addSetter(propertyName, required, property, classDeclaration, isPatchModel, false);
                    addSetter(propertyName, required, property, classDeclaration, isPatchModel, true);
                });
    }

    private void addGetter(final String propertyName,
                           final OpenApiSchema<?> property,
                           final JClassDeclaration classDeclaration,
                           final boolean isPatchModel) {
        if (isPatchModel && Boolean.TRUE.equals(property.readOnly())) {
            return;
        }

        final var methodName = "get" + StringUtils.firstUpper(propertyName);
        final var method = classDeclaration.addMethod(
                new NoTypeExpression(TypeKind.VOID),
                methodName,
                Set.of(Modifier.PUBLIC)
        );

        //final var propertyType = property.schema();
        var returnType = openApiTypeUtils.createTypeExpression(property);

        if (isPatchModel) {
            if (returnType instanceof PrimitiveTypeExpression pt) {
                returnType = toBoxedType(pt);
            }

            returnType = new ClassOrInterfaceTypeExpression(JSON_NULLABLE_CLASS_NAME, List.of(returnType));
        }

        method.setReturnType(returnType);

        final var methodSymbol = new MethodSymbol(ElementKind.METHOD, JavaVoidType.INSTANCE, methodName);
        methodSymbol.addModifiers(Modifier.PUBLIC);

        method.setMethodSymbol(methodSymbol);

        final var body = new BlockStatement(
                new ReturnStatement(new FieldAccessExpression(new IdentifierExpression("this"), propertyName))
        );
        method.setBody(body);
        method.setMetaData(OpenApiSchema.class.getSimpleName(), property);
    }

    private void addSetter(final String propertyName,
                           final boolean isRequired,
                           final OpenApiSchema<?> schema,
                           final JClassDeclaration clazz,
                           final boolean isPatchModel,
                           final boolean isBuilder) {
        if (isPatchModel) {
            if (Boolean.TRUE.equals(schema.readOnly())) {
                return;
            }
        } else if (isRequired) {
            return;
        }

        final var methodName = isBuilder ? propertyName : "set" + StringUtils.firstUpper(propertyName);
        final var method = clazz.addMethod(
                new NoTypeExpression(TypeKind.VOID),
                methodName,
                Set.of(Modifier.PUBLIC)
        );

        //final var propertyType = schema.schema();
        var parameterType = openApiTypeUtils.createTypeExpression(schema);

        if (isPatchModel) {
            if (parameterType instanceof PrimitiveTypeExpression pt) {
                parameterType = toBoxedType(pt);
            }

            parameterType = new ClassOrInterfaceTypeExpression(JSON_NULLABLE_CLASS_NAME, List.of(parameterType));
        }

        final var parameter = new JVariableDeclaration(
                ElementKind.PARAMETER,
                Set.of(Modifier.FINAL),
                parameterType,
                propertyName,
                null,
                null
        );

        final var parameterSymbol = new VariableSymbol(ElementKind.PARAMETER, propertyName);

        parameter.setSymbol(parameterSymbol);

        method.addParameter(parameter);

        final var methodSymbol = new MethodSymbol(ElementKind.METHOD, JavaVoidType.INSTANCE, methodName);
        methodSymbol.addParameter(parameterSymbol);

        method.setMethodSymbol(methodSymbol);

        final var body = new BlockStatement();

        final var varExpression = new IdentifierExpression(propertyName);

        body.add(new BinaryExpression(
                new FieldAccessExpression(new IdentifierExpression("this"), propertyName),
                varExpression,
                Operator.ASSIGN
        ));

        if (isBuilder) {
            body.add(new ReturnStatement(new IdentifierExpression("this")));
            final var returnType = new ClassOrInterfaceTypeExpression(clazz.getQualifiedName().toString());
            method.setReturnType(returnType);
        }

        method.setBody(body);
        method.setMetaData(OpenApiSchema.class.getSimpleName(), schema);
    }

    private @Nullable Expression getDefaultValue(final TypeExpression type) {
        if (type instanceof PrimitiveTypeExpression primitiveTypeExpression) {
            return switch (primitiveTypeExpression.getKind()) {
                case BOOLEAN -> LiteralExpression.createBooleanLiteralExpression();
                case CHAR -> LiteralExpression.createCharLiteralExpression();
                case BYTE, SHORT, INT -> LiteralExpression.createIntLiteralExpression();
                case LONG -> LiteralExpression.createLongLiteralExpression();
                case FLOAT -> LiteralExpression.createFloatLiteralExpression();
                case DOUBLE -> LiteralExpression.createDoubleLiteralExpression();
                default -> throw new GenerateException(String.format("%s is not a primitive", primitiveTypeExpression.getKind()));
            };
        } else if (type instanceof ClassOrInterfaceTypeExpression classOrInterfaceTypeExpression) {
            if (classOrInterfaceTypeExpression.getName().contentEquals(ClassNames.LIST_CLASS_NAME)) {
                return new MethodCallExpression(new ClassOrInterfaceTypeExpression(ClassNames.LIST_CLASS_NAME), "of");
            } else if (classOrInterfaceTypeExpression.isNullable()) {
                return LiteralExpression.createNullLiteralExpression();
            }
        } else if (type instanceof ArrayTypeExpression) {
            return new ArrayInitializerExpression();
        }

        return null;
    }

}