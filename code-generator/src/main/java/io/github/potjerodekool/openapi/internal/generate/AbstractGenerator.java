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
import io.github.potjerodekool.codegen.model.tree.statement.java.JClassDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.java.JVariableDeclaration;
import io.github.potjerodekool.codegen.model.tree.type.ClassOrInterfaceTypeExpression;
import io.github.potjerodekool.codegen.model.util.QualifiedName;
import io.github.potjerodekool.codegen.model.util.StringUtils;
import io.github.potjerodekool.openapi.ApiConfiguration;
import io.github.potjerodekool.openapi.GeneratorConfig;
import io.github.potjerodekool.openapi.HttpMethod;
import io.github.potjerodekool.openapi.internal.type.OpenApiTypeUtils;
import io.github.potjerodekool.openapi.tree.*;
import io.github.potjerodekool.openapi.tree.media.OpenApiObjectSchema;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static io.github.potjerodekool.openapi.internal.generate.OpenApiUtils.findJsonMediaType;
import static io.github.potjerodekool.openapi.internal.generate.OpenApiUtils.isMultiPart;

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

    public void generate(final OpenApi api) {
        api.paths().forEach(this::processPath);
        generateCode();
    }

    protected void generateCode() {
        final var compilationUnits = compilationUnitMap.values();
        environment.getCompilationUnits().addAll(compilationUnits);
    }

    private void processPath(final OpenApiPath openApiPath) {
        processOperation(openApiPath, HttpMethod.POST, openApiPath.path(), openApiPath.post());
        processOperation(openApiPath, HttpMethod.GET, openApiPath.path(), openApiPath.get());
        processOperation(openApiPath, HttpMethod.PUT, openApiPath.path(), openApiPath.put());
        processOperation(openApiPath, HttpMethod.PATCH, openApiPath.path(), openApiPath.patch());
        processOperation(openApiPath, HttpMethod.DELETE, openApiPath.path(), openApiPath.delete());
    }

    protected abstract void processOperation(OpenApiPath openApiPath,
                                             HttpMethod httpMethod,
                                             String path,
                                             @Nullable OpenApiOperation operation);

    protected JClassDeclaration findOrCreateClassDeclaration(final OpenApiPath openApiPath,
                                                             final OpenApiOperation operation) {
        final var className = resolveClassName(openApiPath, operation);
        final var qualifiedName = QualifiedName.from(className);
        final var packageName = qualifiedName.packageName();
        final var simpleName = qualifiedName.simpleName();

        final var cu = this.compilationUnitMap.computeIfAbsent(className, (key) ->
                createCompilationUnitWithClassDeclaration(packageName, simpleName));

        return cu.getDefinitions().stream()
                .filter(it -> it instanceof JClassDeclaration)
                .map(it -> (JClassDeclaration) it)
                .findFirst()
                .orElse(null);
    }

    private CompilationUnit createCompilationUnitWithClassDeclaration(final Name packageName,
                                                                      final Name className) {
        final var newCU = new CompilationUnit(Language.JAVA);
        final var pckName = packageName.toString();

        final var packageDeclaration = new PackageDeclaration(new IdentifierExpression(pckName));
        newCU.add(packageDeclaration);

        final var classDeclaration = createClass(pckName, className);

        classDeclaration.setEnclosing(packageDeclaration);
        newCU.add(classDeclaration);

        return newCU;
    }

    protected abstract ClassDeclaration<?> createClass(String packageName,
                                                       Name simpleName);

    protected String resolveClassName(final OpenApiPath openApiPath,
                                      final OpenApiOperation operation) {
        return generateClasName(openApiPath, operation);
    }

    protected String generateClasName(final OpenApiPath openApiPath,
                                      final OpenApiOperation operation) {
        return resolveQualifiedName(openApiPath, operation);
    }

    private String resolveQualifiedName(final OpenApiPath openApiPath,
                                        final OpenApiOperation operation) {
        final var packageName = resolvePackageName(openApiPath);
        final String simpleName;

        final var creatingReference = openApiPath.creatingReference();

        if ( creatingReference != null) {
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
            final var pathElements = openApiPath.path().split("/");

            final var simpleNameBuilder = new StringBuilder();

            for (final String pathElement : pathElements) {
                if (!pathElement.startsWith("{")) {
                    simpleNameBuilder.append(StringUtils.firstUpper(pathElement));
                }
            }

            simpleName = simpleNameBuilder.toString();
        }

        final String apiName;

        if (operation.tags().isEmpty()) {
            apiName = StringUtils.firstUpper(simpleName);
        } else {
            final var firstTag = operation.tags().get(0);
            apiName = Arrays.stream(firstTag.split("-"))
                    .map(StringUtils::firstUpper)
                    .collect(Collectors.joining());
        }

        return packageName + "." + apiName;
    }

    private String resolvePackageName(final OpenApiPath openApiPath) {
        return basePackageName;
    }

    protected List<JVariableDeclaration> createParameters(final OpenApiOperation operation,
                                                          final HttpMethod httpMethod) {
        final var parameters = new ArrayList<>(operation.parameters().stream()
                .map(this::createParameter)
                .toList());

        final OpenApiRequestBody requestBody = operation.requestBody();

        if (requestBody != null) {
            final var bodyMediaType = findJsonMediaType(requestBody.contentMediaType());
            final Expression bodyType;

            if (bodyMediaType != null) {
                if (httpMethod == HttpMethod.PATCH
                        && bodyMediaType instanceof OpenApiObjectSchema os) {
                    if (os.name().toLowerCase().contains("patch")) {
                        bodyType = openApiTypeUtils.createTypeExpression(
                                ContentTypes.JSON,
                                os
                        );
                    } else {
                        bodyType = openApiTypeUtils.createTypeExpression(
                                ContentTypes.JSON, os.withName("Patch" + os.name())
                        );
                    }
                } else {
                    bodyType = openApiTypeUtils.createTypeExpression(
                            ContentTypes.JSON,
                            bodyMediaType
                    );
                }
            } else if (
                    isMultiPart(requestBody.contentMediaType())
                    || OpenApiUtils.isImageOrVideo(requestBody.contentMediaType())) {
                bodyType = openApiTypeUtils.createMultipartTypeExpression();
            } else {
                bodyType = new ClassOrInterfaceTypeExpression("java.lang.Object");
            }

            final var bodyParameter = new JVariableDeclaration(
                    ElementKind.PARAMETER,
                    Set.of(Modifier.FINAL),
                    bodyType,
                    "body",
                    null,
                    null
            );

            parameters.add(bodyParameter);
        }

        return parameters;
    }

    protected JVariableDeclaration createParameter(final OpenApiParameter openApiParameter) {
        var type = openApiTypeUtils.createTypeExpression("", openApiParameter.type());

        if (Boolean.TRUE.equals(openApiParameter.required())) {
            type = type.asNonNullableType();
        }

        return new JVariableDeclaration(
                ElementKind.PARAMETER,
                Set.of(Modifier.FINAL),
                type,
                openApiParameter.name(),
                null,
                null
        );
    }
}
