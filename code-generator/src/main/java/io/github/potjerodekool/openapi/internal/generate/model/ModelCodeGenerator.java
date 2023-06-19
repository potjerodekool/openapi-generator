package io.github.potjerodekool.openapi.internal.generate.model;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.Language;
import io.github.potjerodekool.codegen.io.Filer;
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
import io.github.potjerodekool.codegen.model.tree.statement.VariableDeclaration;
import io.github.potjerodekool.codegen.model.tree.type.ParameterizedType;
import io.github.potjerodekool.codegen.model.tree.type.PrimitiveTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.TypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.WildCardTypeExpression;
import io.github.potjerodekool.codegen.model.type.ArrayType;
import io.github.potjerodekool.codegen.model.type.PrimitiveType;
import io.github.potjerodekool.codegen.model.type.TypeKind;
import io.github.potjerodekool.codegen.model.util.Elements;
import io.github.potjerodekool.codegen.model.util.StringUtils;
import io.github.potjerodekool.codegen.model.util.SymbolTable;
import io.github.potjerodekool.openapi.generate.model.ModelAdapter;
import io.github.potjerodekool.openapi.internal.di.ApplicationContext;
import io.github.potjerodekool.openapi.internal.generate.BasicResolver;
import io.github.potjerodekool.openapi.internal.generate.FullResolver;
import io.github.potjerodekool.openapi.internal.generate.annotation.openapi.media.ArraySchemaAnnotationBuilder;
import io.github.potjerodekool.openapi.internal.generate.annotation.openapi.media.SchemaAnnotationBuilder;
import io.github.potjerodekool.openapi.internal.type.OpenApiTypeUtils;
import io.github.potjerodekool.openapi.internal.util.GenerateException;
import io.github.potjerodekool.openapi.internal.util.TypeUtils;
import io.github.potjerodekool.openapi.log.LogLevel;
import io.github.potjerodekool.openapi.log.Logger;
import io.github.potjerodekool.openapi.tree.OpenApiProperty;
import io.github.potjerodekool.openapi.type.OpenApiArrayType;
import io.github.potjerodekool.openapi.type.OpenApiObjectType;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class ModelCodeGenerator {

    private static final Logger LOGGER = Logger.getLogger(ModelCodeGenerator.class.getName());
    private static final String JSON_NULLABLE_CLASS_NAME = "org.openapitools.jackson.nullable.JsonNullable";

    private final OpenApiTypeUtils openApiTypeUtils;
    private final TypeUtils typeUtils;
    private final SymbolTable symbolTable;
    private final Filer filer;
    private final Elements elements;
    private final Language language;
    private final ModelAdapter modelAdapter;

    private final BasicResolver basicResolver;
    private final FullResolver fullResolver;

    public ModelCodeGenerator(final TypeUtils typeUtils,
                               final OpenApiTypeUtils openApiTypeUtils,
                               final Environment environment,
                               final Language language,
                               final ApplicationContext applicationContext) {
        this.openApiTypeUtils = openApiTypeUtils;
        this.typeUtils = typeUtils;
        this.symbolTable = environment.getSymbolTable();
        final var types = environment.getTypes();
        this.filer = environment.getFiler();
        this.elements = environment.getElementUtils();
        this.language = language;
        this.modelAdapter = new CombinedModelAdapter(applicationContext);
        this.basicResolver = new BasicResolver(elements, types, environment.getSymbolTable());
        this.fullResolver = new FullResolver(elements, types);
    }


    public void generateSchemaModel(final String schemaName,
                                    final OpenApiObjectType schema,
                                    final boolean isPatchModel) {
        final var cu = new CompilationUnit(Language.JAVA);
        final var pck = schema.pck();

        final var packageDeclaration = new PackageDeclaration(new NameExpression(pck.getName()));
        final var packageSymbol = symbolTable.findOrCreatePackageSymbol(Name.of(pck.getName()));

        packageDeclaration.setPackageSymbol(packageSymbol);
        cu.add(packageDeclaration);
        cu.setPackageElement(packageSymbol);

        final var classDeclaration = new ClassDeclaration(
                Name.of(schemaName),
                ElementKind.CLASS,
                Set.of(),
                List.of()
        );

        cu.add(classDeclaration);
        classDeclaration.addModifier(Modifier.PUBLIC);
        classDeclaration.setEnclosing(packageDeclaration);

        addFields(schema, classDeclaration, isPatchModel);
        addConstructors(schema, classDeclaration, isPatchModel);
        generateAccessors(schema, classDeclaration, isPatchModel);

        classDeclaration.accept(basicResolver, null);
        classDeclaration.accept(fullResolver, null);

        symbolTable.addClass(classDeclaration.getClassSymbol());

        try {
            filer.writeSource(cu, this.language);
        } catch (final IOException e) {
            LOGGER.log(LogLevel.SEVERE, String.format("Failed to write code for schema %s", schemaName), e);
        }
    }

    private void addConstructors(final OpenApiObjectType schema,
                                 final ClassDeclaration classDeclaration,
                                 final boolean isPatchModel) {
        final var generatedDefaultConstructor = !schema.properties().values().stream()
                .allMatch(OpenApiProperty::required);

        if (generatedDefaultConstructor) {
            final var defaultConstructor = classDeclaration.addConstructor(Set.of(Modifier.PUBLIC));
            final var constructorSymbol = MethodSymbol.createConstructor(
                    classDeclaration.getSimpleName()
            );
            constructorSymbol.addModifier(Modifier.PUBLIC);

            defaultConstructor.setMethodSymbol(constructorSymbol);

            defaultConstructor.setBody(new BlockStatement());
            modelAdapter.adaptConstructor(defaultConstructor);
        }

        if (isPatchModel) {
            return;
        }

        final var requiredArgsConstructor = classDeclaration.addConstructor(Set.of(Modifier.PUBLIC));
        final var constructorSymbol = MethodSymbol.createConstructor(classDeclaration.getSimpleName());
        constructorSymbol.addModifier(Modifier.PUBLIC);
        
        requiredArgsConstructor.setMethodSymbol(constructorSymbol);

        schema.properties().entrySet().stream()
                .filter(entry -> entry.getValue().required())
                .forEach(entry -> {
                    final var propertyName = entry.getKey();
                    final var property = entry.getValue();
                    final var paramType = openApiTypeUtils.createTypeExpression(property.type());

                    final var parameter = new VariableDeclaration(
                            ElementKind.PARAMETER,
                            Set.of(Modifier.FINAL),
                            paramType,
                            propertyName,
                            null,
                            null
                    );

                    final var parameterSymbol = VariableSymbol.createParameter(
                            propertyName,
                            null
                    );

                    requiredArgsConstructor.addParameter(parameter);
                    parameter.setSymbol(parameterSymbol);

                    constructorSymbol.addParameter(parameterSymbol);
                });

        final var body = new BlockStatement();

        schema.properties().entrySet().stream()
                .filter(entry -> entry.getValue().required())
                .forEach(entry -> {
                    final var value = new NameExpression(entry.getKey());

                    body.add(new BinaryExpression(
                                    new FieldAccessExpression(
                                            new NameExpression("this"),
                                            entry.getKey()
                                    ),
                                    value,
                                    Operator.ASSIGN
                            )
                    );
                });

        requiredArgsConstructor.setBody(body);
        modelAdapter.adaptConstructor(requiredArgsConstructor);
    }

    private void addFields(final OpenApiObjectType schema,
                           final ClassDeclaration classDeclaration,
                           final boolean isPatchModel) {
        schema.properties().forEach((propertyName, property) ->
                addField(propertyName,  property, classDeclaration, isPatchModel));
    }

    private void addField(final String propertyName,
                          final OpenApiProperty property,
                          final ClassDeclaration classDeclaration,
                          final boolean isPatchModel) {
        if (isPatchModel && Boolean.TRUE.equals(property.readOnly())) {
            return;
        }

        final var propertyType = property.type();
        var fieldType = openApiTypeUtils.createTypeExpression(propertyType);

        fieldType.accept(this.basicResolver, null);

        var fieldTypeMirror = fieldType.getType();

        if (isPatchModel) {
            if (fieldType instanceof PrimitiveTypeExpression pt) {
                fieldType = toBoxedType(pt);
            }

            fieldType = new ParameterizedType(
                    new NameExpression(JSON_NULLABLE_CLASS_NAME),
                    List.of(fieldType)
            );
        }

        final Expression initExpression;

        if (isPatchModel) {
            initExpression = new MethodCallExpression(
                    new NameExpression(JSON_NULLABLE_CLASS_NAME),
                    "undefined");
        } else {
            if (fieldTypeMirror.isDeclaredType() && !fieldType.isNullable()) {
                initExpression = null;
            } else {
                initExpression = getDefaultValue(fieldType);
            }
        }

        fieldType.accept(basicResolver, null);

        final var fieldSymbol = VariableSymbol.createField(propertyName, fieldType.getType());

        var field = new VariableDeclaration(
                ElementKind.FIELD,
                Set.of(Modifier.PRIVATE),
                fieldType,
                propertyName,
                initExpression,
                fieldSymbol
        );

        classDeclaration.addEnclosed(field);

        if (propertyType instanceof OpenApiArrayType) {
            TypeExpression elementType;

            if (fieldType instanceof ArrayType arrayType) {
                //elementType = arrayType.getComponentType();
                throw new UnsupportedOperationException();
            } else {
                //elementType = ((DeclaredType)fieldType).getTypeArguments().get(0);
                throw new UnsupportedOperationException();
            }

            //TODO field.addAnnotation(createArraySchemaAnnotation(elementType));
        } else {
            final var type = openApiTypeUtils.createTypeExpression(propertyType);
            final var typeMirror = openApiTypeUtils.createType(propertyType);
            type.setType(typeMirror);

            final boolean required;
            final Boolean readOnly;
            final Boolean writeOnly;

            if (isPatchModel) {
                required = false;
                readOnly = null;
                writeOnly = true;
            } else {
                required = property.required();
                readOnly = property.readOnly();
                writeOnly = property.writeOnly();
            }

            field.addAnnotation(createSchemaAnnotation(
                    type,
                    required,
                    property.description(),
                    readOnly,
                    writeOnly
            ));
        }
        modelAdapter.adaptField(property, field);
    }

    private TypeExpression toBoxedType(final PrimitiveTypeExpression primitiveTypeExpression) {
        throw new UnsupportedOperationException();
    }

    public AnnotationExpression createSchemaAnnotation(final TypeExpression type,
                                                       final boolean required,
                                                       final String description,
                                                       final Boolean readOnly,
                                                       final Boolean writeOnly) {
        final var implementationType = resolveImplementationType(type);

        return new SchemaAnnotationBuilder()
                .implementation(implementationType)
                .requiredMode(required)
                .description(description)
                .accessMode(readOnly, writeOnly)
                .build();
    }

    private void generateAccessors(final OpenApiObjectType schema,
                                   final ClassDeclaration classDeclaration,
                                   final boolean isPatchModel) {
        schema.properties()
                .forEach((propertyName, property) -> {
                    addGetter(propertyName, property, classDeclaration, isPatchModel);

                    addSetter(propertyName, property, classDeclaration, isPatchModel, false);
                    addSetter(propertyName, property, classDeclaration, isPatchModel, true);
                });
    }

    private void addGetter(final String propertyName,
                           final OpenApiProperty property,
                           final ClassDeclaration classDeclaration,
                           final boolean isPatchModel) {
        if (isPatchModel && Boolean.TRUE.equals(property.readOnly())) {
            return;
        }

        final var methodName = "get" + StringUtils.firstUpper(propertyName);
        final var method = classDeclaration.addMethod(
                methodName,
                Set.of(Modifier.PUBLIC)
        );

        final var propertyType = property.type();
        var returnType = openApiTypeUtils.createTypeExpression(propertyType);

        if (isPatchModel) {
            if (returnType instanceof PrimitiveTypeExpression pt) {
                returnType = toBoxedType(pt);
            }

            returnType = new ParameterizedType(
                    new NameExpression(JSON_NULLABLE_CLASS_NAME),
                    List.of(returnType)
            );
        }

        method.setReturnType(returnType);

        final var methodSymbol = MethodSymbol.createMethod(methodName);
        methodSymbol.addModifier(Modifier.PUBLIC);

        method.setMethodSymbol(methodSymbol);

        final var body = new BlockStatement(
                new ReturnStatement(new FieldAccessExpression(new NameExpression("this"), propertyName))
        );
        method.setBody(body);
        modelAdapter.adaptGetter(property, method);
    }

    private void addSetter(final String propertyName,
                           final OpenApiProperty property,
                           final ClassDeclaration clazz,
                           final boolean isPatchModel,
                           final boolean isBuilder) {
        if (isPatchModel && Boolean.TRUE.equals(property.readOnly())) {
            return;
        }

        final var methodName = isBuilder ? propertyName : "set" + StringUtils.firstUpper(propertyName);
        final var method = clazz.addMethod(
                methodName,
                Set.of(Modifier.PUBLIC)
        );

        final var propertyType = property.type();
        var parameterType = openApiTypeUtils.createTypeExpression(propertyType);

        if (isPatchModel) {
            if (parameterType instanceof PrimitiveTypeExpression pt) {
                parameterType = toBoxedType(pt);
            }

            parameterType = new ParameterizedType(
                    new NameExpression(JSON_NULLABLE_CLASS_NAME),
                    List.of(parameterType)
            );
        }

        final var parameter = new VariableDeclaration(
                ElementKind.PARAMETER,
                Set.of(Modifier.FINAL),
                parameterType,
                propertyName,
                null,
                null
        );

        final var parameterSymbol = VariableSymbol.createParameter(
                propertyName,
                null
        );
        parameter.setSymbol(parameterSymbol);

        method.addParameter(parameter);

        final var methodSymbol = MethodSymbol.createMethod(methodName);
        methodSymbol.addParameter(parameterSymbol);

        method.setMethodSymbol(methodSymbol);

        final var body = new BlockStatement();

        final var varExpression = new NameExpression(propertyName);

        body.add(new BinaryExpression(
                new FieldAccessExpression(new NameExpression("this"), propertyName),
                varExpression,
                Operator.ASSIGN
        ));

        if (isBuilder) {
            body.add(new ReturnStatement(new NameExpression("this")));
            final var returnType = new NameExpression(clazz.getQualifiedName().toString());
            method.setReturnType(returnType);
        }

        method.setBody(body);
        modelAdapter.adaptSetter(property, method);
    }

    public @Nullable Expression getDefaultValue(final TypeExpression type) {
        final var typeMirror = type.getType();

        if (typeMirror.isPrimitiveType()) {
            final var primitiveType = (PrimitiveType) typeMirror;
            return switch (primitiveType.getKind()) {
                case BOOLEAN -> LiteralExpression.createBooleanLiteralExpression();
                case CHAR -> LiteralExpression.createCharLiteralExpression();
                case BYTE, SHORT, INT -> LiteralExpression.createIntLiteralExpression();
                case LONG -> LiteralExpression.createLongLiteralExpression();
                case FLOAT -> LiteralExpression.createFloatLiteralExpression();
                case DOUBLE -> LiteralExpression.createDoubleLiteralExpression();
                default -> throw new GenerateException(String.format("%s is not a primitive", primitiveType.getKind()));
            };
        } else if (typeUtils.isListType(typeMirror)) {
            return new MethodCallExpression(
                    new NameExpression(typeUtils.getListTypeName()),
                    "of"
            );
        } else if (type.getKind() == TypeKind.DECLARED) {
            return LiteralExpression.createNullLiteralExpression();
        } else if (type.getKind() == TypeKind.ARRAY) {
            return new ArrayInitializerExpression();
        } else {
            return null;
        }
    }

    public AnnotationExpression createArraySchemaAnnotation(final TypeExpression elementType) {
        final var implementationType = resolveImplementationType(elementType);

        return new ArraySchemaAnnotationBuilder()
                .schema(new SchemaAnnotationBuilder()
                        .implementation(implementationType)
                        .requiredMode(false)
                        .build())
                .build();
    }

    //TODO duplicate code
    public TypeExpression resolveImplementationType(final TypeExpression type) {
        final TypeExpression implementationType;

        if (type instanceof WildCardTypeExpression wildcardType) {
            implementationType = wildcardType.getTypeExpression();
        } else {
            implementationType = type;
        }
        return implementationType;
    }


}