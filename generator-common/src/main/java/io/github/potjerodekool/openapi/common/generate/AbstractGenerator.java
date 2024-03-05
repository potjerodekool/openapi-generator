package io.github.potjerodekool.openapi.common.generate;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.Language;
import io.github.potjerodekool.codegen.io.Location;
import io.github.potjerodekool.codegen.model.element.ElementKind;
import io.github.potjerodekool.codegen.model.element.Modifier;
import io.github.potjerodekool.codegen.model.util.QualifiedName;
import io.github.potjerodekool.codegen.model.util.StringUtils;
import io.github.potjerodekool.codegen.template.ImportOrganiser;
import io.github.potjerodekool.codegen.template.TemplateBasedGenerator;
import io.github.potjerodekool.codegen.template.model.TCompilationUnit;
import io.github.potjerodekool.codegen.template.model.annotation.Annot;
import io.github.potjerodekool.codegen.template.model.element.TypeElem;
import io.github.potjerodekool.codegen.template.model.element.VariableElem;
import io.github.potjerodekool.codegen.template.model.expression.SimpleLiteralExpr;
import io.github.potjerodekool.codegen.template.model.type.ClassOrInterfaceTypeExpr;
import io.github.potjerodekool.codegen.template.model.type.TypeExpr;
import io.github.potjerodekool.openapi.common.ApiConfiguration;
import io.github.potjerodekool.openapi.common.GeneratorConfig;
import io.swagger.models.HttpMethod;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static io.github.potjerodekool.openapi.common.util.OpenApiUtils.findJsonMediaType;
import static io.github.potjerodekool.openapi.common.util.OpenApiUtils.isMultiPart;
import static io.github.potjerodekool.openapi.common.util.OpenApiUtils.isImageOrVideo;
import static io.github.potjerodekool.openapi.common.util.StringUtils.toValidClassName;

public abstract class AbstractGenerator implements OpenApiWalkerListener {

    private final String basePackageName;

    private final String modelPackageName;

    private final Language language;

    private final OpenApiTypeUtils typeUtils;

    private final Map<String, TCompilationUnit> compilationUnitMap = new HashMap<>();

    private final Environment environment;

    protected AbstractGenerator(final GeneratorConfig generatorConfig,
                                final ApiConfiguration apiConfiguration,
                                final OpenApiTypeUtils typeUtils, final Environment environment) {
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

    public OpenApiTypeUtils getTypeUtils() {
        return typeUtils;
    }

    public Collection<TCompilationUnit> getCompilationUnits() {
        return compilationUnitMap.values();
    }

    public void generate(final OpenAPI openAPI) {
        OpenApiWalker.walk(openAPI, this);

        final var generator = new TemplateBasedGenerator();
        final var filer = environment.getFiler();

        final var importOrganiser = new ImportOrganiser();

        compilationUnitMap.values().forEach(cu -> {
            importOrganiser.organiseImports(cu);

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

    protected TypeElem createClass(final String simpleName) {
        final var date = DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now());

        return new TypeElem()
                .modifier(Modifier.PUBLIC)
                .simpleName(simpleName)
                .annotation(
                        new Annot("javax.annotation.processing.Generated")
                                .value(new SimpleLiteralExpr(getClass().getName()))
                                .value("date", new SimpleLiteralExpr(date))
                );
    }

    protected String resolveClassName(final String path,
                                      final Operation operation) {
        return generateClasName(path, operation);
    }

    protected String generateClasName(final String path,
                                      final Operation operation) {
        final var className = resolveQualifiedName(path, operation);
        final var suffix = classNameSuffix();
        return suffix == null ? className : className + suffix;
    }

    protected abstract String classNameSuffix();

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
                                ContentTypes.JSON,
                                requestBody.getRequired()
                        );
                    } else {
                        final var type = (ClassOrInterfaceTypeExpr) typeUtils.createType(
                                api,
                                bodyObjectSchema,
                                modelPackageName,
                                ContentTypes.JSON,
                                requestBody.getRequired()
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
                            ContentTypes.JSON,
                            requestBody.getRequired()
                    );
                }
            } else if (
                    isMultiPart(requestBody.getContent())
                            || isImageOrVideo(requestBody.getContent())) {
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
                null,
                openApiParameter.getRequired()
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
