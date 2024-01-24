package io.github.potjerodekool.openapi.internal.generate.incurbation;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.Language;
import io.github.potjerodekool.codegen.io.Location;
import io.github.potjerodekool.codegen.model.element.ElementKind;
import io.github.potjerodekool.codegen.model.element.Modifier;
import io.github.potjerodekool.codegen.model.util.QualifiedName;
import io.github.potjerodekool.codegen.model.util.StringUtils;
import io.github.potjerodekool.codegen.template.TemplateBasedGenerator;
import io.github.potjerodekool.codegen.template.model.TCompilationUnit;
import io.github.potjerodekool.codegen.template.model.element.TypeElem;
import io.github.potjerodekool.codegen.template.model.element.VariableElem;
import io.github.potjerodekool.codegen.template.model.type.ClassOrInterfaceTypeExpr;
import io.github.potjerodekool.codegen.template.model.type.TypeExpr;
import io.github.potjerodekool.openapi.ApiConfiguration;
import io.github.potjerodekool.openapi.GeneratorConfig;
import io.github.potjerodekool.openapi.internal.generate.ContentTypes;
import io.github.potjerodekool.openapi.internal.generate.OpenApiUtils;
import io.github.potjerodekool.openapi.internal.generate.model.OpenApiWalker;
import io.github.potjerodekool.openapi.internal.generate.model.OpenApiWalkerListener;
import io.github.potjerodekool.openapi.internal.generate.model.SchemaResolver;
import io.swagger.models.HttpMethod;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;

import java.util.*;
import java.util.stream.Collectors;

import static io.github.potjerodekool.openapi.internal.generate.OpenApiUtils.findJsonMediaType;
import static io.github.potjerodekool.openapi.internal.generate.OpenApiUtils.isMultiPart;
import static io.github.potjerodekool.openapi.internal.util.StringUtils.toValidClassName;

public abstract class AbstractGenerator implements OpenApiWalkerListener {

    private final String basePackageName;

    private final String modelPackageName;

    private final Language language;

    private final TypeUtils typeUtils;

    private final Map<String, TCompilationUnit> compilationUnitMap = new HashMap<>();

    private final Environment environment;

    protected AbstractGenerator(final GeneratorConfig generatorConfig,
                                final ApiConfiguration apiConfiguration,
                                final TypeUtils typeUtils, final Environment environment) {
        this.basePackageName = Objects.requireNonNull(
                apiConfiguration.basePackageName(),
                generatorConfig::basePackageName
        );
        this.modelPackageName = apiConfiguration.modelPackageName();
        this.language = generatorConfig.language();
        this.typeUtils = typeUtils;
        this.environment = environment;
    }

    public String getBasePackageName() {
        return basePackageName;
    }

    public String getModelPackageName() {
        return modelPackageName;
    }

    public TypeUtils getTypeUtils() {
        return typeUtils;
    }

    public Collection<TCompilationUnit> getCompilationUnits() {
        return compilationUnitMap.values();
    }

    public void generate(final OpenAPI openAPI) {
        OpenApiWalker.walk(openAPI, this);

        final var generator = new TemplateBasedGenerator();
        final var filer = environment.getFiler();

        compilationUnitMap.values().forEach(cu -> {
            final var code = generator.doGenerate(cu);
            final var clazz = cu.getElements().getFirst();

            final var resource = filer.createResource(
                    Location.SOURCE_OUTPUT,
                    cu.getPackageName(), clazz.getSimpleName() + ".java");

            resource.writeToOutputStream(code.getBytes());
        });
    }

    protected TypeElem findOrCreateClass(final String path,
                                         final Operation operation) {
        final var className = resolveClassName(path, operation);
        final var qualifiedName = QualifiedName.from(className);
        final var packageName = qualifiedName.packageName().toString();
        final var simpleName = qualifiedName.simpleName().toString();

        final var cu = this.compilationUnitMap.computeIfAbsent(className, (key) ->
                createCompilationUnit(packageName, simpleName));

        return cu.getElements().stream()
                .findFirst()
                .orElse(null);
    }

    private TCompilationUnit createCompilationUnit(final String packageName,
                                                   final String simpleName) {
        final var cu = new TCompilationUnit(language);
        cu.packageName(packageName);

        final var clazz = createClass(simpleName);
        cu.element(clazz);
        return cu;
    }

    protected abstract TypeElem createClass(final String simpleName);

    protected String resolveClassName(final String path,
                                      final Operation operation) {
        return generateClasName(path, operation);
    }

    protected String generateClasName(final String path,
                                      final Operation operation) {
        return resolveQualifiedName(path, operation);
    }

    private String resolveQualifiedName(final String path,
                                        final Operation operation) {
        String simpleName;

        final var pathElements = path.split("/");

        final var simpleNameBuilder = new StringBuilder();

        for (final String pathElement : pathElements) {
            if (!pathElement.startsWith("{")) {
                simpleNameBuilder.append(StringUtils.firstUpper(pathElement));
            }
        }

        simpleName = simpleNameBuilder.toString();

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

        return basePackageName + "." + apiName;
    }

    protected List<VariableElem> createParameters(final OpenAPI api,
                                                  final Operation operation,
                                                  final HttpMethod httpMethod) {
        final List<VariableElem> parameters = new ArrayList<>();

        if (operation.getParameters() != null) {
            parameters.addAll(
                    operation.getParameters().stream()
                            .map(parameter -> createParameter(api, parameter))
                            .toList()
            );
        }

        final var requestBody = operation.getRequestBody();

        if (requestBody != null) {
            final var bodyMediaType = findJsonMediaType(requestBody.getContent());
            final TypeExpr bodyType;

            if (bodyMediaType != null) {
                final var resolved = SchemaResolver.resolve(api, bodyMediaType);

                if (httpMethod == HttpMethod.PATCH) {
                    final var schemaName = resolved.name();
                    final var bodyObjectSchema = resolved.schema();

                    if (schemaName.toLowerCase().contains("patch")) {
                        bodyType = typeUtils.createType(
                                api,
                                bodyObjectSchema,
                                modelPackageName,
                                ContentTypes.JSON
                        );
                    } else {
                        final var type = (ClassOrInterfaceTypeExpr) typeUtils.createType(
                                api,
                                bodyObjectSchema,
                                modelPackageName,
                                ContentTypes.JSON
                        );

                        final var simpleName = "Patch" + schemaName;
                        type.name(modelPackageName + "." + simpleName);
                        bodyType = type;
                    }
                } else {
                    bodyType = typeUtils.createType(
                            api,
                            bodyMediaType,
                            modelPackageName,
                            ContentTypes.JSON
                    );
                }
            } else if (
                    isMultiPart(requestBody.getContent())
                            || OpenApiUtils.isImageOrVideo(requestBody.getContent())) {
                bodyType = typeUtils.createMultipartTypeExpression(api);
            } else {
                bodyType = new ClassOrInterfaceTypeExpr("java.lang.Object");
            }

            final var bodyParameter = new VariableElem()
                    .kind(ElementKind.PARAMETER)
                    .modifier(Modifier.FINAL)
                    .type(bodyType)
                    .simpleName("body");

            parameters.add(bodyParameter);
        }

        return parameters;
    }

    protected VariableElem createParameter(final OpenAPI openAPI,
                                           final Parameter openApiParameter) {
        var type = typeUtils.createType(
                openAPI,
                openApiParameter.getSchema(),
                modelPackageName,
                null
        );

        if (Boolean.TRUE.equals(openApiParameter.getRequired())) {
            type = typeUtils.asNonNull(type);
        }

        return new VariableElem()
                .kind(ElementKind.PARAMETER)
                .modifier(Modifier.FINAL)
                .type(type)
                .simpleName(openApiParameter.getName());
    }
}
