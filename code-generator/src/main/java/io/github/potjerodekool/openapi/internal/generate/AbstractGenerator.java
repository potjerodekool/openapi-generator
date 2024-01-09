package io.github.potjerodekool.openapi.internal.generate;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.Language;
import io.github.potjerodekool.codegen.model.CompilationUnit;
import io.github.potjerodekool.codegen.model.element.ElementKind;
import io.github.potjerodekool.codegen.model.element.Modifier;
import io.github.potjerodekool.codegen.model.element.Name;
import io.github.potjerodekool.codegen.model.tree.PackageDeclaration;
import io.github.potjerodekool.codegen.model.tree.expression.Expression;
import io.github.potjerodekool.codegen.model.tree.expression.IdentifierExpression;
import io.github.potjerodekool.codegen.model.tree.statement.ClassDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.VariableDeclaration;
import io.github.potjerodekool.codegen.model.tree.type.ClassOrInterfaceTypeExpression;
import io.github.potjerodekool.codegen.model.util.QualifiedName;
import io.github.potjerodekool.codegen.model.util.StringUtils;
import io.github.potjerodekool.openapi.ApiConfiguration;
import io.github.potjerodekool.openapi.GeneratorConfig;
import io.github.potjerodekool.openapi.internal.type.OpenApiTypeUtils;
import io.swagger.models.HttpMethod;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static io.github.potjerodekool.openapi.internal.generate.OpenApiUtils.findJsonMediaType;
import static io.github.potjerodekool.openapi.internal.generate.OpenApiUtils.isMultiPart;
import static io.github.potjerodekool.openapi.internal.util.OpenApiUtils.findComponentSchemaByName;
import static io.github.potjerodekool.openapi.internal.util.OpenApiUtils.getSchemaName;
import static io.github.potjerodekool.openapi.internal.util.StringUtils.toValidClassName;

public abstract class AbstractGenerator {

    private final String basePackageName;
    private final Map<String, CompilationUnit> compilationUnitMap = new HashMap<>();
    private final Environment environment;
    private final OpenApiTypeUtils openApiTypeUtils;
    private final ApiConfiguration apiConfiguration;

    protected AbstractGenerator(final GeneratorConfig generatorConfig,
                                final ApiConfiguration apiConfiguration,
                                final Environment environment,
                                final OpenApiTypeUtils openApiTypeUtils) {
        this.apiConfiguration = apiConfiguration;
        this.basePackageName = Objects.requireNonNull(apiConfiguration.basePackageName(), generatorConfig::basePackageName);
        this.environment = environment;
        this.openApiTypeUtils = openApiTypeUtils;
    }

    public String getBasePackageName() {
        return basePackageName;
    }

    public ApiConfiguration getApiConfiguration() {
        return apiConfiguration;
    }

    protected OpenApiTypeUtils getOpenApiTypeUtils() {
        return openApiTypeUtils;
    }

    public void generate(final OpenAPI openApi) {
        openApi.getPaths().forEach((path, pathItem) -> processPath(openApi, path, pathItem));
        generateCode();
    }

    protected void generateCode() {
        final var compilationUnits = compilationUnitMap.values();
        environment.getCompilationUnits().addAll(compilationUnits);
    }

    private void processPath(final OpenAPI openAPI,
                             final String path, final PathItem openApiPath) {
        processOperation(openAPI, openApiPath, HttpMethod.POST, path, openApiPath.getPost());
        processOperation(openAPI, openApiPath, HttpMethod.GET, path, openApiPath.getGet());
        processOperation(openAPI, openApiPath, HttpMethod.PUT, path, openApiPath.getPut());
        processOperation(openAPI, openApiPath, HttpMethod.PATCH, path, openApiPath.getPatch());
        processOperation(openAPI, openApiPath, HttpMethod.DELETE, path, openApiPath.getDelete());
    }

    protected abstract void processOperation(final OpenAPI openAPI,
                                             PathItem openApiPath,
                                             HttpMethod httpMethod,
                                             String path,
                                             @Nullable Operation operation);

    protected ClassDeclaration findOrCreateClassDeclaration(final String path,
                                                            final PathItem openApiPath,
                                                            final Operation operation) {
        final var className = resolveClassName(path, openApiPath, operation);
        final var qualifiedName = QualifiedName.from(className);
        final var packageName = qualifiedName.packageName();
        final var simpleName = qualifiedName.simpleName();

        final var cu = this.compilationUnitMap.computeIfAbsent(className, (key) ->
                createCompilationUnitWithClassDeclaration(packageName, simpleName));

        return cu.getDefinitions().stream()
                .filter(it -> it instanceof ClassDeclaration)
                .map(it -> (ClassDeclaration) it)
                .findFirst()
                .orElse(null);
    }

    private CompilationUnit createCompilationUnitWithClassDeclaration(final Name packageName,
                                                                      final Name className) {
        final var newCU = new CompilationUnit(Language.JAVA);
        final var pckName = packageName.toString();

        final var packageDeclaration = new PackageDeclaration(new IdentifierExpression(pckName));
        newCU.packageDeclaration(packageDeclaration);

        final var classDeclaration = createClass(pckName, className);

        classDeclaration.setEnclosing(packageDeclaration);
        newCU.classDeclaration(classDeclaration);

        return newCU;
    }

    protected abstract ClassDeclaration createClass(String packageName,
                                                    Name simpleName);

    protected String resolveClassName(final String path,
                                      final PathItem openApiPath,
                                      final Operation operation) {
        return generateClasName(path, openApiPath, operation);
    }

    protected String generateClasName(final String path,
                                      final PathItem openApiPath,
                                      final Operation operation) {
        return resolveQualifiedName(path, openApiPath, operation);
    }

    private String resolveQualifiedName(final String path,
                                        final PathItem openApiPath,
                                        final Operation operation) {
        final var packageName = basePackageName;
        String simpleName;

        final var creatingReference = openApiPath.get$ref();

        if (creatingReference != null) {
            if (!packageName.isEmpty()) {
                final var start = creatingReference.lastIndexOf('/') + 1;
                final var end = creatingReference.indexOf('.', start);
                simpleName = creatingReference.substring(start, end);
            } else {
                final var separatorIndex = creatingReference.lastIndexOf("/");
                final var end = creatingReference.lastIndexOf(".");

                if (separatorIndex > -1) {
                    simpleName = creatingReference.substring(separatorIndex + 1, end);
                } else {
                    simpleName = creatingReference.substring(0, end);
                }
            }
        } else {
            final var pathElements = path.split("/");

            final var simpleNameBuilder = new StringBuilder();

            for (final String pathElement : pathElements) {
                if (!pathElement.startsWith("{")) {
                    simpleNameBuilder.append(StringUtils.firstUpper(pathElement));
                }
            }

            simpleName = simpleNameBuilder.toString();
        }

        simpleName = toValidClassName(simpleName);

        final String apiName;

        if (operation.getTags() == null || operation.getTags().isEmpty()) {
            apiName = StringUtils.firstUpper(simpleName);
        } else {
            final var firstTag = operation.getTags().getFirst();
            apiName = Arrays.stream(firstTag.split("-"))
                    .map(StringUtils::firstUpper)
                    .collect(Collectors.joining());
        }

        return packageName + "." + apiName;
    }

    protected List<VariableDeclaration> createParameters(final OpenAPI openAPI,
                                                         final Operation operation,
                                                         final HttpMethod httpMethod) {
        final List<VariableDeclaration> parameters = new ArrayList<>();

        if (operation.getParameters() != null) {
            parameters.addAll(
                    operation.getParameters().stream()
                            .map(parameter -> createParameter(openAPI, parameter))
                            .toList()
            );
        }

        final var requestBody = operation.getRequestBody();

        if (requestBody != null) {
            final var bodyMediaType = findJsonMediaType(requestBody.getContent());
            final Expression bodyType;

            if (bodyMediaType != null) {
                final var bodyObjectSchemaPair = asObjectSchema(openAPI, bodyMediaType);

                if (httpMethod == HttpMethod.PATCH
                        && bodyObjectSchemaPair != null) {
                    final var schemaName = bodyObjectSchemaPair.getLeft();
                    final var bodyObjectSchema = bodyObjectSchemaPair.getRight();

                    if (schemaName.toLowerCase().contains("patch")) {
                        bodyType = openApiTypeUtils.createTypeExpression(
                                ContentTypes.JSON,
                                bodyObjectSchema,
                                openAPI
                        );
                    } else {
                        ClassOrInterfaceTypeExpression type = (ClassOrInterfaceTypeExpression) openApiTypeUtils.createTypeExpression(
                                ContentTypes.JSON, bodyObjectSchema, openAPI
                        );
                        final var name = type.getName().toString();
                        final var packageNameEnd = name.lastIndexOf(".");
                        final String packageName = name.substring(0,packageNameEnd);
                        final var simpleName = "Patch" + schemaName;
                        type.name(Name.of(packageName + "." + simpleName));
                        bodyType = type;
                    }
                } else {
                    bodyType = openApiTypeUtils.createTypeExpression(
                            ContentTypes.JSON,
                            bodyMediaType,
                            openAPI
                    );
                }
            } else if (
                    isMultiPart(requestBody.getContent())
                            || OpenApiUtils.isImageOrVideo(requestBody.getContent())) {
                bodyType = openApiTypeUtils.createMultipartTypeExpression(openAPI);
            } else {
                bodyType = new ClassOrInterfaceTypeExpression("java.lang.Object");
            }

            final var bodyParameter = new VariableDeclaration()
                    .kind(ElementKind.PARAMETER)
                    .modifier(Modifier.FINAL)
                    .varType(bodyType)
                    .name("body");

            parameters.add(bodyParameter);
        }

        return parameters;
    }

    private Pair<String, ObjectSchema> asObjectSchema(final OpenAPI openAPI, final Schema<?> schema) {
        if (schema instanceof ObjectSchema objectSchema) {
            return new ImmutablePair<>(objectSchema.getName(), objectSchema);
        }

        final var componentSchema = findComponentSchemaByName(
                openAPI,
                schema.get$ref()
        );

        if (componentSchema instanceof ObjectSchema objectSchema) {
            final var name = getSchemaName(schema.get$ref());

            return new ImmutablePair<>(
                    name,
                    objectSchema
            );
        } else {
            return null;
        }
    }

    protected VariableDeclaration createParameter(final OpenAPI openAPI,
                                                  final Parameter openApiParameter) {
        var type = openApiTypeUtils.createTypeExpression("", openApiParameter.getSchema(), openAPI);

        if (Boolean.TRUE.equals(openApiParameter.getRequired())) {
            type = type.asNonNullableType();
        }

        return new VariableDeclaration()
                .kind(ElementKind.PARAMETER)
                .modifier(Modifier.FINAL)
                .varType(type)
                .name(openApiParameter.getName());
    }
}
