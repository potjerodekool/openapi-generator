package io.github.potjerodekool.openapi.internal.generate.api;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.Language;
import io.github.potjerodekool.codegen.io.Filer;
import io.github.potjerodekool.codegen.model.CompilationUnit;
import io.github.potjerodekool.codegen.model.element.ElementKind;
import io.github.potjerodekool.codegen.model.element.Modifier;
import io.github.potjerodekool.codegen.model.element.Name;
import io.github.potjerodekool.codegen.model.element.NestingKind;
import io.github.potjerodekool.codegen.model.symbol.MethodSymbol;
import io.github.potjerodekool.codegen.model.symbol.VariableSymbol;
import io.github.potjerodekool.codegen.model.tree.AnnotationExpression;
import io.github.potjerodekool.codegen.model.tree.MethodDeclaration;
import io.github.potjerodekool.codegen.model.tree.PackageDeclaration;
import io.github.potjerodekool.codegen.model.tree.expression.*;
import io.github.potjerodekool.codegen.model.tree.statement.BlockStatement;
import io.github.potjerodekool.codegen.model.tree.statement.ClassDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.ReturnStatement;
import io.github.potjerodekool.codegen.model.tree.statement.VariableDeclaration;
import io.github.potjerodekool.codegen.model.tree.type.*;
import io.github.potjerodekool.codegen.model.type.DeclaredType;
import io.github.potjerodekool.codegen.model.type.PrimitiveType;
import io.github.potjerodekool.codegen.model.util.QualifiedName;
import io.github.potjerodekool.codegen.model.util.StringUtils;
import io.github.potjerodekool.codegen.model.util.SymbolTable;
import io.github.potjerodekool.codegen.model.util.type.Types;
import io.github.potjerodekool.openapi.ApiConfiguration;
import io.github.potjerodekool.openapi.Features;
import io.github.potjerodekool.openapi.GeneratorConfig;
import io.github.potjerodekool.openapi.HttpMethod;
import io.github.potjerodekool.openapi.internal.ClassNames;
import io.github.potjerodekool.openapi.internal.generate.BasicResolver;
import io.github.potjerodekool.openapi.internal.generate.FullResolver;
import io.github.potjerodekool.openapi.internal.generate.annotation.openapi.OperationAnnotationBuilder;
import io.github.potjerodekool.openapi.internal.generate.annotation.openapi.ParameterAnnotationBuilder;
import io.github.potjerodekool.openapi.internal.generate.annotation.openapi.header.HeaderAnnotationBuilder;
import io.github.potjerodekool.openapi.internal.generate.annotation.openapi.media.*;
import io.github.potjerodekool.openapi.internal.generate.annotation.openapi.response.ApiResponseAnnotationBuilder;
import io.github.potjerodekool.openapi.internal.generate.annotation.openapi.response.ApiResponsesAnnotationBuilder;
import io.github.potjerodekool.openapi.internal.generate.annotation.openapi.security.SecurityRequirementAnnotationBuilder;
import io.github.potjerodekool.openapi.internal.generate.annotation.openapi.security.SecurityRequirementsBuilder;
import io.github.potjerodekool.openapi.internal.generate.annotation.spring.web.*;
import io.github.potjerodekool.openapi.internal.type.OpenApiTypeUtils;
import io.github.potjerodekool.openapi.internal.util.TypeUtils;
import io.github.potjerodekool.openapi.internal.util.Utils;
import io.github.potjerodekool.openapi.log.LogLevel;
import io.github.potjerodekool.openapi.log.Logger;
import io.github.potjerodekool.openapi.tree.*;
import io.github.potjerodekool.openapi.type.OpenApiObjectType;
import io.github.potjerodekool.openapi.type.OpenApiType;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class SpringApiDefinitionGenerator {

    private static final String JSON_CONTENT_TYPE = "application/json";

    private static final Logger LOGGER = Logger.getLogger(SpringApiDefinitionGenerator.class.getName());

    private final Language language;
    private final String basePackageName;
    private final TypeUtils typeUtils;

    private final SymbolTable symbolTable;
    private final Types types;
    private final Filer filer;

    private final OpenApiTypeUtils openApiTypeUtils;

    private final File pathsDir;
    private final Map<String, CompilationUnit> compilationUnitMap = new HashMap<>();

    private final String servletClassName;

    private final String validAnnotationClassName;

    private final Map<String, String> controllers;

    private final BasicResolver basicResolver;

    private final FullResolver fullResolver;

    public SpringApiDefinitionGenerator(final GeneratorConfig generatorConfig,
                                        final ApiConfiguration apiConfiguration,
                                        final TypeUtils typeUtils,
                                        final OpenApiTypeUtils openApiTypeUtils,
                                        final Environment environment) {
        this.language = generatorConfig.language();
        this.basePackageName = Optional.ofNullable(apiConfiguration.basePackageName()).orElse(generatorConfig.basePackageName());
        this.symbolTable = environment.getSymbolTable();
        final var elements = environment.getElementUtils();
        this.types = environment.getTypes();
        this.openApiTypeUtils = openApiTypeUtils;
        this.typeUtils = typeUtils;
        this.filer = environment.getFiler();
        this.pathsDir = apiConfiguration.pathsDir();
        servletClassName = generatorConfig.isFeatureEnabled(Features.FEATURE_JAKARTA)
                ? ClassNames.JAKARTA_HTTP_SERVLET_REQUEST
                : ClassNames.JAVA_HTTP_SERVLET_REQUEST;

        final var validationBasePackage = generatorConfig.isFeatureEnabled(Features.FEATURE_JAKARTA) ? "jakarta" : "javax";
        this.validAnnotationClassName = validationBasePackage + ".validation.Valid";
        this.controllers = apiConfiguration.controllers();
        this.basicResolver = new BasicResolver(elements, types, symbolTable);
        this.fullResolver = new FullResolver(elements, types);
    }

    public void generate(final OpenApi api) {
        api.paths().forEach(this::processPath);
        generateCode();
    }

    private void generateCode() {
        final var compilationUnits = compilationUnitMap.values();
        compilationUnits.forEach(cu -> cu.getDefinitions().forEach(def -> def.accept(basicResolver, null)));

        compilationUnits.stream()
                .flatMap(it -> it.getClassDeclarations().stream())
                .forEach(fullResolver::resolve);

        compilationUnits.forEach(cu -> {
            try {
                filer.writeSource(cu, language);
            } catch (final IOException e) {
                LOGGER.log(LogLevel.SEVERE, "Fail to generate code for spring api definition", e);
            }
        });
    }

    private CompilationUnit createCompilationUnitWithInterface(final Name packageName,
                                                               final Name interfaceName) {
        final var newCU = new CompilationUnit(Language.JAVA);

        final var packageSymbol = symbolTable.findOrCreatePackageSymbol(packageName);
        newCU.setPackageElement(packageSymbol);
        final var packageDeclaration = new PackageDeclaration(new NameExpression(packageName.toString()));
        packageDeclaration.setPackageSymbol(packageSymbol);

        newCU.add(packageDeclaration);

        final var classSymbol = symbolTable.enterClass(ElementKind.INTERFACE, interfaceName, NestingKind.TOP_LEVEL, packageSymbol);
        classSymbol.addModifiers(Modifier.PUBLIC);

        final var classDeclaration = new ClassDeclaration(interfaceName, ElementKind.INTERFACE, Set.of(Modifier.PUBLIC), List.of());
        newCU.add(classDeclaration);

        return newCU;
    }

    private void processPath(final OpenApiPath openApiPath) {
        processOperation(openApiPath, HttpMethod.POST, openApiPath.path(), openApiPath.post());
        processOperation(openApiPath, HttpMethod.GET, openApiPath.path(), openApiPath.get());
        processOperation(openApiPath, HttpMethod.PUT, openApiPath.path(), openApiPath.put());
        processOperation(openApiPath, HttpMethod.PATCH, openApiPath.path(), openApiPath.patch());
        processOperation(openApiPath, HttpMethod.DELETE, openApiPath.path(), openApiPath.delete());
    }

    private QualifiedName resolveApiName(final OpenApiPath openApiPath,
                                         final OpenApiOperation operation) {
        for (final var tag : operation.tags()) {
            final var className = this.controllers.get(tag);
            if (className != null) {
                return QualifiedName.from(className);
            }
        }

        return resolveQualifiedName(openApiPath, operation);
    }

    private String resolvePackageName(final OpenApiPath openApiPath) {
        final var pathUri = Utils.toUriString(this.pathsDir);
        final var creatingReference = openApiPath.creatingReference();

        if (creatingReference.startsWith(pathUri)) {
            final var ref = creatingReference.substring(pathUri.length());
            final var qualifiedName = Utils.resolveQualified(ref);
            return qualifiedName.packageName().toString();
        } else {
            return basePackageName;
        }
    }

    private QualifiedName resolveQualifiedName(final OpenApiPath openApiPath, final OpenApiOperation operation) {
        final var packageName = resolvePackageName(openApiPath);
        final String simpleName;

        final var pathUri = Utils.toUriString(this.pathsDir);
        if (packageName.length() > 0) {
            final var creatingReference = openApiPath.creatingReference();

            if (creatingReference.startsWith(pathUri + "/")) {
                final var ref = creatingReference.substring(pathUri.length());
                simpleName = Utils.resolveQualified(ref).simpleName().toString();
            } else {
                final var start = creatingReference.lastIndexOf('/') + 1;
                final var end = creatingReference.indexOf('.', start);
                simpleName = creatingReference.substring(start, end);
            }
        } else {
            final var creatingReference = openApiPath.creatingReference();
            final var separatorIndex = creatingReference.lastIndexOf("/");
            final var end = creatingReference.lastIndexOf(".");

            if (separatorIndex > -1) {
                simpleName = creatingReference.substring(separatorIndex + 1, end);
            } else {
                simpleName = creatingReference.substring(0, end);
            }
        }

        final String apiName;

        if (operation.tags().isEmpty()) {
            apiName = StringUtils.firstUpper(simpleName) + "Api";
        } else {
            final var firstTag = operation.tags().get(0);
            final var name = Arrays.stream(firstTag.split("-"))
                    .map(StringUtils::firstUpper)
                    .collect(Collectors.joining());

            apiName = name + "Api";
        }

        return new QualifiedName(Name.of(packageName), Name.of(apiName));
    }

    private ClassDeclaration findOrCreateClassDeclaration(final OpenApiPath openApiPath,
                                                          final OpenApiOperation operation) {
        final var apiName = resolveApiName(openApiPath, operation);
        final var qualifiedApiName = apiName.toString();
        final var packageName = apiName.packageName();
        final var simpleName = apiName.simpleName();

        final var cu = this.compilationUnitMap.computeIfAbsent(qualifiedApiName, (key) ->
                createCompilationUnitWithInterface(packageName, simpleName));

        return cu.getDefinitions().stream()
                .filter(it -> it instanceof ClassDeclaration)
                .map(it -> (ClassDeclaration) it)
                .findFirst()
                .orElse(null);
    }

    private AnnotationExpression createApiResponsesAnnotation(final OpenApiOperation operation) {
        final var responses = operation.responses().entrySet().stream()
                        .map(this::createApiResponse)
                .toList();

        return new ApiResponsesAnnotationBuilder()
                .value(responses)
                .build();
    }

    private AnnotationExpression createApiResponse(final Map.Entry<String, OpenApiResponse> entry) {
        final var response = entry.getValue();
        final var description = response.description();

        final List<Expression> contentList = response.contentMediaType().entrySet().stream()
                        .map(contentMediaType -> (Expression) createContentAnnotation(contentMediaType.getKey(), contentMediaType.getValue()))
                .toList();

        return new ApiResponseAnnotationBuilder()
                .responseCode(entry.getKey())
                .description(description)
                .headers(headers(response.headers()))
                .content(contentList)
                .build();
    }

    private AnnotationExpression createContentAnnotation(final String mediaType,
                                                         final OpenApiContent content) {
        final var schemaType = openApiTypeUtils.createTypeExpression(content.schema());
        schemaType.accept(basicResolver, null);

        final var typeMirror = schemaType.getType();

        final var examples = content.examples().entrySet().stream()
                .map(exampleEntry -> createExampleObject(exampleEntry.getKey(), exampleEntry.getValue()))
                .toList();

        final var contentAnnotationBuilder = new ContentAnnotationBuilder();
        contentAnnotationBuilder.mediaType(mediaType);
        contentAnnotationBuilder.examples(examples);

        if (typeUtils.isListType(typeMirror) || typeMirror.isArrayType()) {
            TypeExpression elementType;

            if (typeMirror.isArrayType()) {
                elementType = (TypeExpression) ((ArrayTypeExpression)schemaType).getComponentTypeExpression();
            } else if (schemaType instanceof AnnotatedTypeExpression annotatedTypeExpression) {
                final var parameterizedType = (ParameterizedType) annotatedTypeExpression.getIdentifier();
                elementType = (TypeExpression) parameterizedType.getArguments().get(0);
            } else {
                throw new UnsupportedOperationException();
            }

            contentAnnotationBuilder.array(new ArraySchemaAnnotationBuilder()
                    .schema(
                            new SchemaAnnotationBuilder()
                                    .implementation(resolveImplementationType(elementType))
                                    .requiredMode(false)
                                    .build()
                    )
                    .build());
        } else if (typeUtils.isMapType(typeMirror)) {
            final var schemaPropertyAnnotation = new SchemaPropertyAnnotationBuilder()
                    .name("additionalProp1")
                    .build();

            final var additionalProperties = content.schema().additionalProperties();

            if (additionalProperties == null) {
                throw new IllegalStateException("Missing additionalProperties");
            }

            final var type = additionalProperties.type();
            final var typeName = type.name();
            final var format = type.format();

            contentAnnotationBuilder.schemaProperties(schemaPropertyAnnotation);
            contentAnnotationBuilder.additionalPropertiesSchema(new SchemaAnnotationBuilder()
                    .type(typeName)
                    .format(format)
                    .build());
        } else {
            contentAnnotationBuilder.schema(new SchemaAnnotationBuilder()
                    .implementation(resolveImplementationType(schemaType))
                    .requiredMode(false)
                    .build());
        }

        return contentAnnotationBuilder.build();
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

    private Expression createExampleObject(final String name,
                                           final OpenApiExample openApiExample) {
        return new ExampleObjectAnnotationBuilder()
                .name(name)
                .summary(openApiExample.summary())
                .value(escapeJson(openApiExample.value().toString()))
                .build();
    }

    private String escapeJson(final String s) {
        final var sb = new StringBuilder();
        char pc = (char) -1;

        for (char c : s.toCharArray()) {
            if (c == '"' && pc != '\\') {
                sb.append('\\');
            }
            sb.append(c);
            pc = c;
        }

        return sb.toString();
    }

    private ArrayInitializerExpression headers(final Map<String, OpenApiHeader> headersMap) {
        final List<Expression> headers =  headersMap.entrySet().stream()
                        .map(entry -> {
                            final var header = entry.getValue();
                            final String description = header.description();
                            final var required = header.required();
                            final var deprecated = header.deprecated();

                            final var headerType = openApiTypeUtils.createTypeExpression(header.type());

                            return (Expression) new HeaderAnnotationBuilder()
                                    .name(entry.getKey())
                                    .description(description)
                                    .required(required)
                                    .deprecated(deprecated)
                                    .schema(
                                            new SchemaAnnotationBuilder()
                                                    .implementation(headerType)
                                                    .build()
                                    )
                                    .build();
                        }).toList();

        return new ArrayInitializerExpression(headers);
    }


    private AnnotationExpression createMappingAnnotation(final HttpMethod httpMethod,
                                                         final String path,
                                                         final OpenApiOperation operation) {
        final var requestMappingAnnotationBuilder = switch (httpMethod) {
            case POST -> RequestMappingAnnotationBuilder.post();
            case GET -> RequestMappingAnnotationBuilder.get();
            case PUT -> RequestMappingAnnotationBuilder.put();
            case PATCH -> RequestMappingAnnotationBuilder.patch();
            case DELETE -> RequestMappingAnnotationBuilder.delete();
        };

        final var produces = operation.responses().values().stream()
                .flatMap(it -> it.contentMediaType().keySet().stream())
                .toList();

        final var requestBody = operation.requestBody();

        requestMappingAnnotationBuilder.value(path);

        if (requestBody != null && requestBody.contentMediaType().size() > 0) {
            @SuppressWarnings("assignment")
            final var consumes = new HashSet<String>();
            consumes.addAll(requestBody.contentMediaType().keySet().stream().toList());
            consumes.addAll(requestBody.contentMediaType().keySet().stream()
                            .filter(it -> it.startsWith("image/"))
                            .map(contentMediaType -> contentMediaType + ";charset=UTF-8")
                            .toList());
            requestMappingAnnotationBuilder.consumes(consumes.stream().toList());
        }

        return requestMappingAnnotationBuilder.produces(produces)
                .build();
    }

    private List<VariableDeclaration> createParameters(final OpenApiOperation operation,
                                                       final HttpMethod httpMethod) {
        final var parameters = new ArrayList<>(operation.parameters().stream()
                .map(this::createParameter)
                .toList());

        final OpenApiRequestBody requestBody = operation.requestBody();

        if (requestBody != null) {
            final var bodyMediaType = findJsonMediaType(requestBody.contentMediaType());
            final Expression bodyType;

            boolean addRequestBodyAnnotation = true;
            boolean isMultipart = false;

            if (bodyMediaType != null) {
                if (httpMethod == HttpMethod.PATCH && bodyMediaType instanceof OpenApiObjectType ot) {
                    if (ot.name().toLowerCase().contains("patch")) {
                        bodyType = openApiTypeUtils.createTypeExpression(ot);
                    } else {
                        bodyType = openApiTypeUtils.createTypeExpression(ot.withName("Patch" + ot.name()));
                    }
                } else {
                    bodyType = openApiTypeUtils.createTypeExpression(bodyMediaType);
                }
            } else if (isMultiPart(requestBody.contentMediaType())) {
                bodyType = typeUtils.createMultipartTypeExpression();
                isMultipart = true;
                addRequestBodyAnnotation = false;
            } else {
                bodyType = new ParameterizedType(new NameExpression("java.lang.Object"));
            }

            final var bodyParameter = new VariableDeclaration(
                    ElementKind.PARAMETER,
                    Set.of(Modifier.FINAL),
                    bodyType,
                    "body",
                    null,
                    null
            );

            if (addRequestBodyAnnotation) {
                bodyParameter.addAnnotation(new AnnotationExpression(validAnnotationClassName));
            }

            if (isMultipart) {
                final String contentMediaType = requestBody.contentMediaType().keySet().iterator().next();
                final var schema = requestBody.contentMediaType().get(contentMediaType).schema();
                final String propertyName = schema.properties().keySet().iterator().next();
                final var required = schema.properties().get(propertyName).required();

                final var requestParamAnnotationBuilder = new RequestParamAnnotationBuilder()
                        .name(propertyName);

                if (!required) {
                    requestParamAnnotationBuilder.required(false);
                }

                bodyParameter.addAnnotation(requestParamAnnotationBuilder.build());

                bodyParameter.setName(propertyName);
            }

            if (addRequestBodyAnnotation) {
                bodyParameter.addAnnotation(new RequestBodyAnnotationBuilder()
                                .required(requestBody.required())
                        .build());
            }

            parameters.add(bodyParameter);
        }

        parameters.add(
            new VariableDeclaration(
                    ElementKind.PARAMETER,
                    Set.of(Modifier.FINAL),
                    new NameExpression(servletClassName),
                    "request",
                    null,
                    null
            )
        );

        return parameters;
    }

    private boolean isMultiPart(final Map<String, OpenApiContent> contentMediaType) {
        return contentMediaType.keySet().stream()
                .anyMatch(it -> it.startsWith("multipart/"));
    }

    private VariableDeclaration createParameter(final OpenApiParameter openApiParameter) {
        final var explode = Boolean.TRUE.equals(openApiParameter.explode());
        var type = openApiTypeUtils.createTypeExpression(openApiParameter.type());

        if (Boolean.TRUE.equals(openApiParameter.required())) {
            type = type.asNonNullableType();
        }

        if (explode) {
            final DeclaredType dt;
            if (type instanceof PrimitiveType pt) {
                dt = (DeclaredType) types.boxedClass(pt).asType();
            } else {
                dt = (DeclaredType) type;
            }
            type = new ParameterizedType(new NameExpression("java.util.List"));
        }

        final var parameter = new VariableDeclaration(
                ElementKind.PARAMETER,
                Set.of(Modifier.FINAL),
                type,
                openApiParameter.name(),
                null,
                null
        );

        switch (openApiParameter.in()) {
            case PATH -> parameter.addAnnotation(createSpringPathVariableAnnotation(openApiParameter));
            case QUERY -> {
                parameter.addAnnotation(createSpringRequestParamAnnotation(openApiParameter));
                parameter.addAnnotation(createApiParamAnnotation(openApiParameter));
            }
            case HEADER -> parameter.addAnnotation(createSpringRequestHeaderAnnotation(openApiParameter));
            case COOKIE -> parameter.addAnnotation(createSpringCookieValueAnnotation(openApiParameter));
        }

        return parameter;
    }

    private AnnotationExpression createSpringPathVariableAnnotation(final OpenApiParameter openApiParameter) {
        final var required = openApiParameter.required();

        final var pathVariableAnnotationBuilder = new PathVariableAnnotationBuilder()
                .name(openApiParameter.name());

        if (Utils.isFalse(required)) {
            pathVariableAnnotationBuilder.required(false);
        }

        return pathVariableAnnotationBuilder.build();
    }

    private AnnotationExpression createSpringRequestParamAnnotation(final OpenApiParameter openApiParameter) {
        final var required = openApiParameter.required();

        final var requestParamAnnotationBuilder = new RequestParamAnnotationBuilder()
                .name(openApiParameter.name());

        if (Utils.isFalse((required))) {
            requestParamAnnotationBuilder.required(false);
        }

        return requestParamAnnotationBuilder.build();
    }

    private AnnotationExpression createApiParamAnnotation(final OpenApiParameter openApiParameter) {
        final var required = openApiParameter.required();
        final var explode = openApiParameter.explode();
        final var allowEmptyValue = openApiParameter.allowEmptyValue();
        final var example = openApiParameter.example();
        final var description = openApiParameter.description();

        final var parameterAnnotationBuilder = new ParameterAnnotationBuilder()
                .name(openApiParameter.name())
                .in(new FieldAccessExpression(
                                new ParameterizedType(
                                        new NameExpression("io.swagger.v3.oas.annotations.enums.ParameterIn")
                                ),
                                openApiParameter.in().name()
                        )
                )
                .description(description)
                .example(example);

        if (Utils.isFalse(required)) {
            parameterAnnotationBuilder.required(false);
        }

        if (Utils.isTrue(allowEmptyValue)) {
            parameterAnnotationBuilder.allowEmptyValue(true);
        }

        if (Utils.isTrue(explode)) {
            parameterAnnotationBuilder.explode(new FieldAccessExpression(
                    new ParameterizedType(
                            new NameExpression("io.swagger.v3.oas.annotations.enums.Explode")
                    ),
                    "TRUE"
            ));
        }

        return parameterAnnotationBuilder.build();
    }

    private AnnotationExpression createSpringRequestHeaderAnnotation(final OpenApiParameter openApiParameter) {
        final var required = openApiParameter.required();

        return new RequestHeaderAnnotationBuilder()
                .name(openApiParameter.name())
                .required(required)
                .build();
    }

    private AnnotationExpression createSpringCookieValueAnnotation(final OpenApiParameter openApiParameter) {
        return new CookieValueAnnotationBuilder()
                .name(openApiParameter.name())
                .required(openApiParameter.required())
                .build();
    }

    private void processOperation(final OpenApiPath openApiPath,
                                  final HttpMethod httpMethod,
                                  final String path,
                                  final @Nullable OpenApiOperation operation) {
        if (operation == null) {
            return;
        }

        final var operationId = operation.operationId();

        if (operationId == null || "".equals(operationId)) {
            throw new IllegalArgumentException();
        }

        final var responseTypes = resolveResponseTypes(operation);
        final Expression responseType;

        if (responseTypes.isEmpty()) {
            responseType = new NameExpression("java.lang.Void");
        } else if (responseTypes.size() == 1) {
            responseType = openApiTypeUtils.createTypeExpression(
                    responseTypes.get(0)
            );
        } else {
            responseType = new ParameterizedType(new NameExpression("java.lang.Object"));
        }

        final var classDeclaration = findOrCreateClassDeclaration(openApiPath, operation);

        final var method = classDeclaration.addMethod(responseType, operationId, Set.of());
        createParameters(operation, httpMethod)
                .forEach(method::addParameter);

        final var methodSymbol = MethodSymbol.createMethod(
                operationId
        );
        method.setMethodSymbol(methodSymbol);
        method.getParameters().forEach(parameter -> {
            final var param = VariableSymbol.createParameter(
                    parameter.getName(),
                    null
            );
            parameter.setSymbol(param);
            methodSymbol.addParameter(param);
        });

        method.accept(basicResolver, null);

        postProcessOperation(
                httpMethod,
                path,
                operation,
                method);
    }

    private void postProcessOperation(final HttpMethod httpMethod,
                                      final String path,
                                      final OpenApiOperation operation,
                                      final MethodDeclaration method) {
        final var responseType = method.getReturnType();

        responseType.accept(basicResolver, null);

        final var returnTypeArg = responseType.getType().isVoidType()
                ? new NameExpression("java.lang.Void")
                : responseType;

        final var returnType = new ParameterizedType(
                new NameExpression("org.springframework.http.ResponseEntity"),
                List.of(returnTypeArg)
        );

        basicResolver.resolve(returnType);

        method.setReturnType(returnType);
        method.addModifier(Modifier.DEFAULT);

        method.addAnnotation(
                new OperationAnnotationBuilder()
                        .summary(operation.summary())
                        .operationId(operation.operationId())
                        .tags(operation.tags())
                        .build()
        );

        final var securityRequirements = operation.securityRequirements();

        if (securityRequirements != null) {
            final var securityRequirementsAnnotation = createSecurityRequirementsAnnotation(securityRequirements);
            method.addAnnotation(securityRequirementsAnnotation);
        }

        final var apiResponsesAnnotation = createApiResponsesAnnotation(operation);

        method.addAnnotation(apiResponsesAnnotation);
        method.addAnnotation(createMappingAnnotation(httpMethod, path, operation));

        final var body = new BlockStatement();

        body.add(new ReturnStatement(
                new MethodCallExpression(
                        new MethodCallExpression(
                                new NameExpression("org.springframework.http.ResponseEntity"),
                                "status",
                                List.of(new FieldAccessExpression(
                                        new NameExpression("org.springframework.http.HttpStatus"),
                                        "NOT_IMPLEMENTED"
                                ))
                        ),
                        "build"
                )
        ));

        method.setBody(body);

        method.accept(basicResolver, null);
    }

    private AnnotationExpression createSecurityRequirementsAnnotation(final List<OpenApiSecurityRequirement> securityRequirements) {
        final var annotations = securityRequirements.stream()
                .map(OpenApiSecurityRequirement::requirements)
                .filter(securityParameterMap -> securityParameterMap.size() > 0)
                .map(securityParameterMap -> {
                    final var name = securityParameterMap.keySet().iterator().next();
                    return (Expression) new SecurityRequirementAnnotationBuilder()
                            .name(name)
                            .build();
                })
                .toList();

        return new SecurityRequirementsBuilder()
                .value(annotations)
                .build();
    }

    private List<OpenApiType> resolveResponseTypes(final OpenApiOperation operation) {
        final Map<String, OpenApiResponse> responses = operation.responses();

        final List<OpenApiType> responseTypes = new ArrayList<>();

        responses.entrySet().stream()
            .filter(entry -> !"default".equals(entry.getKey()))
            .forEach(entry -> {
                final var response = entry.getValue();
                final var contentMediaType = resolveResponseMediaType(response.contentMediaType());

                if (contentMediaType != null) {
                    responseTypes.add(contentMediaType);
                }
            });
        return responseTypes;
    }

    private static @Nullable OpenApiType resolveResponseMediaType(final Map<String, OpenApiContent> contentMediaType) {
        final var jsonMediaType = findJsonMediaType(contentMediaType);

        if (jsonMediaType != null) {
            return jsonMediaType;
        } else {
            //Not returning json, maybe image/jpg or */*
            if (contentMediaType.size() == 1) {
                final var content = contentMediaType.values().iterator().next();
                if (content != null) {
                    return content.schema();
                }
            }
            return null;
        }
    }

    public static @Nullable OpenApiType findJsonMediaType(final Map<String, OpenApiContent> contentMediaType) {
        final var content = contentMediaType.get(JSON_CONTENT_TYPE);
        return content != null ? content.schema() : null;
    }

}
