package io.github.potjerodekool.openapi.internal.generate.model;

import io.github.potjerodekool.codegen.Language;
import io.github.potjerodekool.codegen.model.CompilationUnit;
import io.github.potjerodekool.codegen.model.element.ElementKind;
import io.github.potjerodekool.codegen.model.element.Modifier;
import io.github.potjerodekool.codegen.model.element.Name;
import io.github.potjerodekool.codegen.model.tree.AnnotationExpression;
import io.github.potjerodekool.codegen.model.tree.MethodDeclaration;
import io.github.potjerodekool.codegen.model.tree.PackageDeclaration;
import io.github.potjerodekool.codegen.model.tree.expression.*;
import io.github.potjerodekool.codegen.model.tree.statement.BlockStatement;
import io.github.potjerodekool.codegen.model.tree.statement.ClassDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.ReturnStatement;
import io.github.potjerodekool.codegen.model.tree.statement.VariableDeclaration;
import io.github.potjerodekool.codegen.model.tree.type.ClassOrInterfaceTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.NoTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.PrimitiveTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.TypeExpression;
import io.github.potjerodekool.codegen.model.type.TypeKind;
import io.github.potjerodekool.codegen.model.util.StringUtils;
import io.github.potjerodekool.openapi.internal.generate.model.adapt.CheckerModelAdapter;
import io.github.potjerodekool.openapi.internal.generate.model.adapt.ModelAdapter;
import io.github.potjerodekool.openapi.internal.generate.model.adapt.SpringModelAdapter;
import io.github.potjerodekool.openapi.internal.generate.model.adapt.StandardValidationModelAdapter;
import io.swagger.models.HttpMethod;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelsGenerator extends AbstractOpenApiWalkerListener {

    private final Map<String, CompilationUnit> models = new HashMap<>();

    private final Map<String, CompilationUnit> patchModels = new HashMap<>();

    private final String packageName;

    private final List<ModelAdapter> adapters = new ArrayList<>();

    public ModelsGenerator(final String packageName) {
        this.packageName = packageName;
    }

    public void registerDefaultModelAdapters() {
        registerModelAdapter(new SpringModelAdapter());
        registerModelAdapter(new CheckerModelAdapter());
        registerModelAdapter(new StandardValidationModelAdapter());
    }

    public void registerModelAdapter(final ModelAdapter adapter) {
        adapters.add(adapter);
    }

    public List<CompilationUnit> getModels() {
        final var list = new ArrayList<CompilationUnit>();
        list.addAll(models.values());
        list.addAll(patchModels.values());
        return list;
    }

    @Override
    protected void visitSchema(final OpenAPI api,
                               final HttpMethod method,
                               final String path,
                               final Operation operation,
                               final Schema<?> schema) {

        final var schemaPair = findSchema(api, schema);

        if (schemaPair != null) {
            if (schemaPair.getRight() instanceof ObjectSchema objectSchema) {
                final var schemaName = schemaPair.getLeft();

                generateModel(
                        method,
                        schemaName,
                        objectSchema
                );
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    private Pair<String, Schema<?>> findSchema(final OpenAPI api,
                                               final Schema<?> schema) {
        if (schema instanceof ArraySchema arraySchema) {
            return findSchema(api, arraySchema.getItems());
        }

        final var ref = schema.get$ref();
        final var componentSchemasPrefix = "#/components/schemas/";

        if (ref == null) {
            return null;
        } else if (ref.startsWith(componentSchemasPrefix)) {
            final var schemaName = ref.substring(componentSchemasPrefix.length());
            final var resoledSchema = (Schema<?>) api.getComponents().getSchemas().get(schemaName);

            return resoledSchema != null
                    ? new ImmutablePair<>(schemaName, resoledSchema)
                    : null;
        }

        return null;
    }

    public CompilationUnit generateModel(final HttpMethod method,
                                         final String name, final ObjectSchema schema) {
        CompilationUnit unit;

        if (method == HttpMethod.PATCH) {
            unit = patchModels.get(name);
        } else {
            unit = models.get(name);
        }

        if (unit == null) {
            unit = createCompilationUnit(name, schema, method);
            adaptModel(method, schema, unit);

            if (method == HttpMethod.PATCH) {
                patchModels.put(name, unit);
            } else {
                models.put(name, unit);
            }
        }

        return unit;
    }

    private void adaptModel(final HttpMethod method,
                            final ObjectSchema schema,
                            final CompilationUnit unit) {
        adapters.forEach(adapter -> adapter.adapt(method, schema, unit));
    }

    private CompilationUnit createCompilationUnit(final String name,
                                                  final ObjectSchema schema,
                                                  final HttpMethod method) {
        final String simpleName = HttpMethod.PATCH == method
                ? "Patch" + name
                : name;

        final var cu = new CompilationUnit(Language.JAVA);
        final var packageDeclaration = new PackageDeclaration(new IdentifierExpression(packageName));
        cu.packageDeclaration(packageDeclaration);

        final var clazz = new ClassDeclaration()
                .kind(ElementKind.CLASS)
                .modifier(Modifier.PUBLIC)
                .simpleName(Name.of(simpleName))
                .annotation(
                        new AnnotationExpression()
                                .annotationType(new ClassOrInterfaceTypeExpression("javax.annotation.processing.Generated"))
                                .argument("value", LiteralExpression.createStringLiteralExpression(getClass().getName()))
                                .argument("date", LiteralExpression.createStringLiteralExpression(DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now())))
                );

        clazz.setEnclosing(packageDeclaration);

        schema.getProperties().forEach((propertyName, propertySchema) ->
                clazz.addEnclosed(createField(propertyName, propertySchema, method)));

        schema.getProperties().forEach((propertyName, propertySchema) -> {
                    clazz.addEnclosed(createGetter(propertyName, propertySchema, method));
                    clazz.addEnclosed(createSetter(propertyName, propertySchema, method, false, clazz));
                    clazz.addEnclosed(createSetter(propertyName, propertySchema, method, true, clazz));
                }
        );

        cu.classDeclaration(clazz);

        return cu;
    }

    private MethodDeclaration createGetter(final String name,
                                           final Schema<?> propertySchema,
                                           final HttpMethod method) {
        final var methodName = "get" + StringUtils.firstUpper(name);
        var type = createTypeExpr(propertySchema);

        if (method == HttpMethod.PATCH) {
            type = createPatchTypeExpression(type);
        }

        final var body = new BlockStatement(
                new ReturnStatement(
                        new FieldAccessExpression(new IdentifierExpression("this"),
                                name
                        ))
        );

        return new MethodDeclaration()
                .kind(ElementKind.METHOD)
                .modifier(Modifier.PUBLIC)
                .simpleName(Name.of(methodName))
                .returnType(type)
                .body(body);
    }

    private MethodDeclaration createSetter(final String name,
                                           final Schema<?> propertySchema,
                                           final HttpMethod method,
                                           final boolean builder,
                                           final ClassDeclaration clazz) {
        final var methodName =
                builder ? name
                        : "set" + StringUtils.firstUpper(name);
        var type = createTypeExpr(propertySchema);

        final BlockStatement body = new BlockStatement();

        final Expression expression;

        if (method == HttpMethod.PATCH) {
            expression = new MethodCallExpression()
                    .target(new ClassOrInterfaceTypeExpression("org.openapitools.jackson.nullable.JsonNullable"))
                    .methodName("of")
                    .argument(new IdentifierExpression(name));
        } else {
            expression = new IdentifierExpression(name);
        }

        body.add(new BinaryExpression(
                        new FieldAccessExpression(new IdentifierExpression("this"), name),
                        expression,
                        Operator.ASSIGN
                )
        );

        if (builder) {
            body.add(new ReturnStatement(new IdentifierExpression("this")));
        }

        final var parameter = new VariableDeclaration()
                .kind(ElementKind.PARAMETER)
                .modifier(Modifier.FINAL)
                .varType(type)
                .name(name);

        final TypeExpression returnType;

        if (builder) {
            returnType = new ClassOrInterfaceTypeExpression(clazz.getQualifiedName());
        } else {
            returnType = new NoTypeExpression(TypeKind.VOID);
        }

        return new MethodDeclaration()
                .kind(ElementKind.METHOD)
                .modifier(Modifier.PUBLIC)
                .simpleName(Name.of(methodName))
                .parameter(parameter)
                .returnType(returnType)
                .body(body);
    }

    private VariableDeclaration createField(final String name,
                                            final Schema<?> schema,
                                            final HttpMethod method) {
        var type = createTypeExpr(schema);
        Expression initExpression = null;

        if (HttpMethod.PATCH == method) {
            type = createPatchTypeExpression(type);

            initExpression = new MethodCallExpression()
                    .target(new ClassOrInterfaceTypeExpression("org.openapitools.jackson.nullable.JsonNullable"))
                    .methodName("undefined");
        }

        return new VariableDeclaration()
                .kind(ElementKind.FIELD)
                .modifier(Modifier.PRIVATE)
                .name(name)
                .varType(type)
                .annotation(
                        new AnnotationExpression()
                                .annotationType(new ClassOrInterfaceTypeExpression("com.fasterxml.jackson.annotation.JsonProperty"))
                                .argument("value", LiteralExpression.createStringLiteralExpression(name))
                )
                .initExpression(initExpression);
    }

    private ClassOrInterfaceTypeExpression createPatchTypeExpression(final TypeExpression typeExpression) {
        final TypeExpression typeArg;

        if (typeExpression instanceof PrimitiveTypeExpression primitiveTypeExpression) {
            typeArg = switch (primitiveTypeExpression.getKind()) {
                case BOOLEAN -> createBooleanTypeExpr(false);
                case INT ->  createIntegerTypeExpr(false, "int32");
                case LONG ->  createIntegerTypeExpr(false, "int64");
                default -> throw new UnsupportedOperationException("" + primitiveTypeExpression.getKind());
            };
        } else {
            typeArg = typeExpression;
        }


        return new ClassOrInterfaceTypeExpression("org.openapitools.jackson.nullable.JsonNullable")
                .typeArgument(typeArg);
    }

    private TypeExpression createTypeExpr(final Schema<?> schema) {
        return switch (schema) {
            case StringSchema ignored -> createStringTypeExpr();
            case EmailSchema ignored -> createStringTypeExpr();
            case PasswordSchema ignored -> createStringTypeExpr();
            case IntegerSchema integerSchema -> createIntegerTypeExpr(integerSchema.getNullable(), integerSchema.getFormat());
            case DateSchema ignored -> createDateTypeExpr();
            case DateTimeSchema ignored -> createClassOrInterfaceTypeExpr("java.time.OffsetDateTime");
            case UUIDSchema ignored -> createClassOrInterfaceTypeExpr("java.util.UUID");
            case BooleanSchema booleanSchema -> createBooleanTypeExpr(booleanSchema.getNullable());
            case ArraySchema ignored -> createClassOrInterfaceTypeExpr("java.util.List");
            case MapSchema ignored -> createClassOrInterfaceTypeExpr("java.util.Map");
            case BinarySchema ignored -> throw new UnsupportedOperationException();
            case ByteArraySchema ignored -> throw new UnsupportedOperationException();
            case ComposedSchema ignored -> throw new UnsupportedOperationException();
            case FileSchema ignored -> throw new UnsupportedOperationException();
            case JsonSchema ignored-> throw new UnsupportedOperationException();
            case NumberSchema ignored -> throw new UnsupportedOperationException();
            case ObjectSchema ignored -> throw new UnsupportedOperationException();
            default -> throw new IllegalStateException("Unexpected value: " + schema);
        };
    }

    private TypeExpression createBooleanTypeExpr(final Boolean nullable) {
        if (Boolean.FALSE.equals(nullable)) {
            return createClassOrInterfaceTypeExpr("java.lang.Boolean");
        } else {
            return PrimitiveTypeExpression.booleanTypeExpression();
        }
    }

    private TypeExpression createDateTypeExpr() {
        return new ClassOrInterfaceTypeExpression("java.time.LocalDate");
    }

    private TypeExpression createStringTypeExpr() {
        return new ClassOrInterfaceTypeExpression("java.lang.String");
    }

    private TypeExpression createClassOrInterfaceTypeExpr(final String className) {
        return new ClassOrInterfaceTypeExpression(className);
    }

    private TypeExpression createIntegerTypeExpr(final Boolean nullable,
                                                 final String format) {
        if (Boolean.FALSE.equals(nullable)) {
            return "int64".equals(format)
                    ? PrimitiveTypeExpression.longTypeExpression()
                    : PrimitiveTypeExpression.intTypeExpression();
        } else {
            final var className = "int64".equals(format) ? "java.lang.Long" : "java.lang.Integer";
            return new ClassOrInterfaceTypeExpression(className);
        }
    }
}
