package io.github.potjerodekool.openapi.internal.generate.api;

import io.github.potjerodekool.openapi.*;
import io.github.potjerodekool.openapi.internal.ClassNames;
import io.github.potjerodekool.openapi.internal.Filer;
import io.github.potjerodekool.openapi.internal.ast.Attribute;
import io.github.potjerodekool.openapi.internal.ast.CompilationUnit;
import io.github.potjerodekool.openapi.internal.ast.Modifier;
import io.github.potjerodekool.openapi.internal.ast.element.*;
import io.github.potjerodekool.openapi.internal.ast.expression.FieldAccessExpression;
import io.github.potjerodekool.openapi.internal.ast.expression.MethodCallExpression;
import io.github.potjerodekool.openapi.internal.ast.expression.NameExpression;
import io.github.potjerodekool.openapi.internal.ast.statement.BlockStatement;
import io.github.potjerodekool.openapi.internal.ast.statement.ReturnStatement;
import io.github.potjerodekool.openapi.internal.ast.type.ArrayType;
import io.github.potjerodekool.openapi.internal.ast.type.DeclaredType;
import io.github.potjerodekool.openapi.internal.ast.type.Type;
import io.github.potjerodekool.openapi.internal.ast.type.java.VoidType;
import io.github.potjerodekool.openapi.internal.ast.util.TypeUtils;
import io.github.potjerodekool.openapi.internal.generate.AnnotationMember;
import io.github.potjerodekool.openapi.internal.generate.GenerateUtils;
import io.github.potjerodekool.openapi.internal.util.QualifiedName;
import io.github.potjerodekool.openapi.internal.util.Utils;
import io.github.potjerodekool.openapi.log.LogLevel;
import io.github.potjerodekool.openapi.log.Logger;
import io.github.potjerodekool.openapi.tree.*;
import io.github.potjerodekool.openapi.type.OpenApiType;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class SpringApiDefinitionGenerator {

    private static final String JSON_CONTENT_TYPE = "application/json";

    private static final String CONTENT_CLASS_NAME = "io.swagger.v3.oas.annotations.media.Content";

    private static final String SECURITY_REQUIREMENT_CLASS_NAME = "io.swagger.v3.oas.annotations.security.SecurityRequirement";

    private static final String API_RESPONSE_CLASS_NAME = "io.swagger.v3.oas.annotations.responses.ApiResponse";

    private static final String EXAMPLE_OBJECT_CLASS_NAME = "io.swagger.v3.oas.annotations.media.ExampleObject";

    private static final Logger LOGGER = Logger.getLogger(SpringApiDefinitionGenerator.class.getName());

    private final Language language;
    private final String basePackageName;
    private final TypeUtils typeUtils;

    private final GenerateUtils generateUtils;

    private final Filer filer;
    private final File pathsDir;
    private final Map<String, CompilationUnit> compilationUnitMap = new HashMap<>();

    private final String servletClassName;

    private final String validAnnotationClassName;

    private final Map<String, String> controllers;

    public SpringApiDefinitionGenerator(final GeneratorConfig generatorConfig,
                                        final ApiConfiguration apiConfiguration,
                                        final TypeUtils typeUtils,
                                        final GenerateUtils generateUtils,
                                        final Filer filer) {
        this.language = generatorConfig.language();
        this.basePackageName = Optional.ofNullable(apiConfiguration.basePackageName()).orElse(generatorConfig.basePackageName());
        this.typeUtils = typeUtils;
        this.generateUtils = generateUtils;
        this.filer = filer;
        this.pathsDir = apiConfiguration.pathsDir();
        servletClassName = generatorConfig.isFeatureEnabled(Features.FEATURE_JAKARTA)
                ? ClassNames.JAKARTA_HTTP_SERVLET_REQUEST
                : ClassNames.JAVA_HTTP_SERVLET_REQUEST;

        final var validationBasePackage = generatorConfig.isFeatureEnabled(Features.FEATURE_JAKARTA) ? "jakarta" : "javax";
        this.validAnnotationClassName = validationBasePackage + ".validation.Valid";
        this.controllers = apiConfiguration.controllers();
    }

    public void generate(final OpenApi api) {
        api.paths().forEach(this::processPath);
        generateCode();
    }

    private void generateCode() {
        compilationUnitMap.values().forEach(cu -> {
            try {
                filer.writeSource(cu, language);
            } catch (final IOException e) {
                LOGGER.log(LogLevel.SEVERE, "Fail to generate code for spring api definition", e);
            }
        });
    }

    private CompilationUnit createCompilationUnitWithInterface(final String packageName,
                                                               final String interfaceName) {
        final var newCU = new CompilationUnit(Language.JAVA);

        newCU.setPackageElement(PackageElement.create(packageName));

        final var typeElement = newCU.addInterface(interfaceName);
        typeElement.addModifiers(Modifier.PUBLIC);

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
            return qualifiedName.packageName();
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
                simpleName = Utils.resolveQualified(ref).simpleName();
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
            apiName = Utils.firstUpper(simpleName) + "Api";
        } else {
            final var firstTag = operation.tags().get(0);
            final var name = Arrays.stream(firstTag.split("-"))
                    .map(Utils::firstUpper)
                    .collect(Collectors.joining());

            apiName = name + "Api";
        }

        return new QualifiedName(packageName, apiName);
    }

    private TypeElement findOrCreateTypeElement(final OpenApiPath openApiPath,
                                                final OpenApiOperation operation) {
        final var apiName = resolveApiName(openApiPath, operation);
        final var qualifiedApiName = apiName.toString();
        final var packageName = apiName.packageName();
        final var simpleName = apiName.simpleName();

        final var cu = this.compilationUnitMap.computeIfAbsent(qualifiedApiName, (key) ->
                createCompilationUnitWithInterface(packageName, simpleName));

        return  (TypeElement) cu.getElements().get(0);
    }

    private AnnotationMirror createApiOperationAnnotation(final List<AnnotationMember> members) {
        return generateUtils.createAnnotation("io.swagger.v3.oas.annotations.Operation", members);
    }

    private AnnotationMirror createApiResponsesAnnotation(final OpenApiOperation operation) {
        final var responses = operation.responses().entrySet().stream()
                        .map(this::createApiResponse)
                .toList();

        return generateUtils.createAnnotation("io.swagger.v3.oas.annotations.responses.ApiResponses",
                new AnnotationMember("value", generateUtils.createArrayInitializerExprOfAnnotations(responses))
        );
    }

    private Attribute.Compound createApiResponse(final Map.Entry<String, OpenApiResponse> entry) {
        final var response = entry.getValue();
        final var description = response.description();

        final var members = new ArrayList<AnnotationMember>();
        members.add(new AnnotationMember("responseCode", Attribute.constant(entry.getKey())));
        members.add(new AnnotationMember("description", Attribute.constant(description != null ? description : "")));

        final var headersMap = response.headers();

        if (headersMap.size() > 0) {
            final var responseHeaders = headers(headersMap);
            members.add(new AnnotationMember(
                    "headers", responseHeaders
            ));
        }

        final List<Attribute> contentList = response.contentMediaType().entrySet().stream()
                        .map(contentMediaType -> (Attribute) createContentAnnotation(contentMediaType.getKey(), contentMediaType.getValue()))
                .toList();

        if (contentList.size() > 0) {
            members.add(new AnnotationMember("content", Attribute.array(contentList)));
        }

        return generateUtils.createAnnotation(API_RESPONSE_CLASS_NAME,
                members
        );
    }

    private Attribute.Compound createContentAnnotation(final String mediaType,
                                                         final OpenApiContent content) {
        final var schemaType = typeUtils.createType(content.schema());

        final var examples = content.examples().entrySet().stream()
                .map(exampleEntry -> createExampleObject(exampleEntry.getKey(), exampleEntry.getValue()))
                .toList();

        final var contextMembers = new ArrayList<AnnotationMember>();
        contextMembers.add(new AnnotationMember("mediaType", Attribute.constant(mediaType)));

        if (examples.size() > 0) {
            contextMembers.add(new AnnotationMember("examples", Attribute.array(examples)));
        }

        if (typeUtils.isListType(schemaType) || schemaType.isArrayType()) {
            Type<?> elementType;

            if (schemaType.isArrayType()) {
                elementType = ((ArrayType)schemaType).getComponentType();
            } else {
                elementType = generateUtils.getFirstTypeArg(schemaType);
            }
            contextMembers.add(new AnnotationMember("array", generateUtils.createArraySchemaAnnotation(elementType)));
        } else if (typeUtils.isMapType(schemaType)) {
            final var schemaPropertyAnnotation = Attribute.compound(
                    "io.swagger.v3.oas.annotations.media.SchemaProperty",
                   toMap(List.of(new AnnotationMember("name", Attribute.constant("additionalProp1"))))
            );

            final var additionalProperties = content.schema().additionalProperties();

            if (additionalProperties == null) {
                throw new IllegalStateException("Missing additionalProperties");
            }

            final var type = additionalProperties.type();
            final var typeName = type.name();
            final var format = type.format();

            contextMembers.add(new AnnotationMember("schemaProperties",
                    Attribute.array(schemaPropertyAnnotation)
            ));
            contextMembers.add(new AnnotationMember("additionalPropertiesSchema", generateUtils.createSchemaAnnotation(typeName, format)));
        } else {
            contextMembers.add(new AnnotationMember("schema", generateUtils.createSchemaAnnotation(schemaType, false)));
        }

        return Attribute.compound(
                CONTENT_CLASS_NAME,
                toMap(contextMembers)
        );
    }

    private Map<ExecutableElement, AnnotationValue> toMap(final List<AnnotationMember> list) {
        return list.stream()
                .collect(Collectors.toMap(
                        it -> MethodElement.createMethod(it.name()),
                        AnnotationMember::value
                ));
    }

    private Attribute.Compound createExampleObject(final String name,
                                                 final OpenApiExample openApiExample) {
        final var summary = openApiExample.summary();

        final var members = new ArrayList<AnnotationMember>();
        members.add(new AnnotationMember("name", Attribute.constant(name)));

        if (summary != null) {
            members.add(new AnnotationMember("summary", Attribute.constant(summary)));
        }

        members.add(new AnnotationMember("value",
                Attribute.constant(escapeJson(openApiExample.value().toString())
        )));

        return Attribute.compound(
                EXAMPLE_OBJECT_CLASS_NAME,
                toMap(members)
        );
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

    private Attribute.Array headers(final Map<String, OpenApiHeader> headersMap) {
        final List<Attribute> headers =  headersMap.entrySet().stream()
                        .map(entry -> {
                            final var header = entry.getValue();
                            final String description = header.description();
                            final var required = header.required();
                            final var allowEmptyValue = header.allowEmptyValue();
                            final var deprecated = header.deprecated();

                            final var headerType = (DeclaredType)
                                    typeUtils.createType(
                                            header.type()
                                    );

                            final var members = new ArrayList<AnnotationMember>();
                            members.add(new AnnotationMember("name", Attribute.constant(entry.getKey())));

                            if (description != null) {
                                members.add(new AnnotationMember("description", Attribute.constant(description)));
                            }

                            if (Utils.isTrue(required)) {
                                members.add(new AnnotationMember("required", Attribute.constant(true)));
                            }

                            if (Utils.isTrue(deprecated)) {
                                members.add(new AnnotationMember("deprecated", Attribute.constant(true)));
                            }

                            if (Utils.isTrue(allowEmptyValue)) {
                                members.add(new AnnotationMember("allowEmptyValue", Attribute.constant(true)));
                            }

                            members.add(new AnnotationMember("schema",
                                    Attribute.compound(
                                            "io.swagger.v3.oas.annotations.media.Schema",
                                            toMap(List.of(
                                                    new AnnotationMember("implementation", Attribute.clazz(headerType))
                                                )
                                            )
                                    )
                            ));

                            return (Attribute) Attribute.compound(
                                    "io.swagger.v3.oas.annotations.headers.Header",
                                    toMap(members)
                            );
                        }).toList();

        return Attribute.array(headers);
    }


    private AnnotationMirror createMappingAnnotation(final HttpMethod httpMethod,
                                                     final String path,
                                                     final OpenApiOperation operation) {
        final var annotationName = switch (httpMethod) {
            case POST -> "org.springframework.web.bind.annotation.PostMapping";
            case GET -> "org.springframework.web.bind.annotation.GetMapping";
            case PUT -> "org.springframework.web.bind.annotation.PutMapping";
            case PATCH -> "org.springframework.web.bind.annotation.PatchMapping";
            case DELETE -> "org.springframework.web.bind.annotation.DeleteMapping";
        };

        final var responseMediaTypes = generateUtils.createArrayInitializerExprOfStrings(operation.responses().values()
                .stream().flatMap(it -> it.contentMediaType().keySet().stream())
                .toList()
        );

        final var requestBody = operation.requestBody();
        final var members = new ArrayList<AnnotationMember>();
        members.add(new AnnotationMember("value",
                Attribute.array(Attribute.constant(path))
        ));

        if (requestBody != null && requestBody.contentMediaType().size() > 0) {
            @SuppressWarnings("assignment")
            final var consumes = new HashSet<String>();
            consumes.addAll(requestBody.contentMediaType().keySet().stream().toList());
            consumes.addAll(requestBody.contentMediaType().keySet().stream()
                            .filter(it -> it.startsWith("image/"))
                            .map(contentMediaType -> contentMediaType + ";charset=UTF-8")
                            .toList());

            members.add(new AnnotationMember("consumes", generateUtils.createArrayInitializerExprOfStrings(
                    new ArrayList<>(consumes)
            )));
        }

        members.add(new AnnotationMember("produces", responseMediaTypes));

        return generateUtils.createAnnotation(
                annotationName,
                members
        );
    }

    private List<VariableElement> createParameters(final OpenApiOperation operation) {
        final var parameters = new ArrayList<>(operation.parameters().stream()
                .map(this::createParameter)
                .toList());

        final OpenApiRequestBody requestBody = operation.requestBody();

        if (requestBody != null) {
            final var bodyMediaType = findJsonMediaType(requestBody.contentMediaType());
            final Type<?> bodyType;

            boolean addRequestBodyAnnotation = true;
            boolean isMultipart = false;

            if (bodyMediaType != null) {
                bodyType = typeUtils.createType(bodyMediaType);
            } else if (isMultiPart(requestBody.contentMediaType())) {
                bodyType = typeUtils.createMultipartType();
                isMultipart = true;
                addRequestBodyAnnotation = false;
            } else {
                bodyType = typeUtils.createObjectType();
            }

            final var bodyParameter = VariableElementBuilder.createParameter("body", bodyType);

            if (addRequestBodyAnnotation) {
                bodyParameter.addAnnotation(validAnnotationClassName);
            }

            if (isMultipart) {
                final String contentMediaType = requestBody.contentMediaType().keySet().iterator().next();
                final var schema = requestBody.contentMediaType().get(contentMediaType).schema();
                final String propertyName = schema.properties().keySet().iterator().next();
                final var required = schema.properties().get(propertyName).required();

                final var members = new ArrayList<AnnotationMember>();
                members.add(new AnnotationMember("value", Attribute.constant(propertyName)));

                if (!required) {
                    members.add(new AnnotationMember("required", Attribute.constant(false)));
                }

                bodyParameter.addAnnotation(Attribute.compound(
                        "org.springframework.web.bind.annotation.RequestParam",
                        toMap(members)
                ));

                bodyParameter.setSimpleName(propertyName);
            }

            if (requestBody.required() != null) {
                Attribute.compound(
                        "org.springframework.web.bind.annotation.RequestBody",
                        toMap(List.of(
                                new AnnotationMember(
                                        "required",
                                        Attribute.constant(requestBody.required())
                                )
                            )
                      )
                );
            } else if (addRequestBodyAnnotation) {
                bodyParameter.addAnnotation("org.springframework.web.bind.annotation.RequestBody");
            }

            parameters.add(bodyParameter.build());
        }

        parameters.add(
                VariableElement.createParameter(
                        "request",
                        typeUtils.createDeclaredType(servletClassName)
                )
        );

        return parameters;
    }

    private boolean isMultiPart(final Map<String, OpenApiContent> contentMediaType) {
        return contentMediaType.keySet().stream()
                .anyMatch(it -> it.startsWith("multipart/"));
    }

    private VariableElement createParameter(final OpenApiParameter openApiParameter) {
        final var explode = Boolean.TRUE.equals(openApiParameter.explode());
        var type = typeUtils.createType(openApiParameter.type()).asNonNullableType();

        if (explode) {
            final DeclaredType dt;
            if (type.isPrimitiveType()) {
                dt = typeUtils.getBoxedType(type);
            } else {
                dt = (DeclaredType) type;
            }
            type = typeUtils.createListType(dt, false);
        }

        final var parameter = VariableElement.createParameter(openApiParameter.name(), type);

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

    private AnnotationMirror createSpringPathVariableAnnotation(final OpenApiParameter openApiParameter) {
        final var required = openApiParameter.required();

        final var members = new ArrayList<AnnotationMember>();

        members.add(new AnnotationMember("name", Attribute.constant(openApiParameter.name())));

        if (Utils.isFalse(required)) {
            members.add(new AnnotationMember("required", Attribute.constant(false)));
        }

        return Attribute.compound("org.springframework.web.bind.annotation.PathVariable", toMap(members));
    }

    private AnnotationMirror createSpringRequestParamAnnotation(final OpenApiParameter openApiParameter) {
        final var required = openApiParameter.required();

        final var members = new ArrayList<AnnotationMember>();

        members.add(new AnnotationMember("name", Attribute.constant(openApiParameter.name())));

        if (Utils.isFalse((required))) {
            members.add(new AnnotationMember("required", Attribute.constant(false)));
        }

        return Attribute.compound("org.springframework.web.bind.annotation.RequestParam", toMap(members));
    }

    private AnnotationMirror createApiParamAnnotation(final OpenApiParameter openApiParameter) {
        final var required = openApiParameter.required();
        final var explode = openApiParameter.explode();
        final var allowEmptyValue = openApiParameter.allowEmptyValue();
        final var example = openApiParameter.example();
        final var description = openApiParameter.description();

        final var members = new ArrayList<AnnotationMember>();

        members.add(new AnnotationMember("name", Attribute.constant(openApiParameter.name())));

        members.add(new AnnotationMember("in", Attribute.createEnumAttribute("io.swagger.v3.oas.annotations.enums.ParameterIn", openApiParameter.in().name())));

        if (description != null) {
            members.add(new AnnotationMember("description", Attribute.constant(description)));
        }

        if (Utils.isFalse(required)) {
            members.add(new AnnotationMember("required", Attribute.constant(false)));
        }

        if (Utils.isTrue(allowEmptyValue)) {
            members.add(new AnnotationMember("allowEmptyValue", Attribute.constant(true)));
        }

        if (Utils.isTrue(explode)) {
            members.add(new AnnotationMember("explode", Attribute.createEnumAttribute("io.swagger.v3.oas.annotations.enums.Explode", "TRUE")));
        }

        if (example != null) {
            members.add(new AnnotationMember("example", Attribute.constant(example)));
        }

        return Attribute.compound("io.swagger.v3.oas.annotations.Parameter", toMap(members));
    }

    private AnnotationMirror createSpringRequestHeaderAnnotation(final OpenApiParameter openApiParameter) {
        final var required = openApiParameter.required();

        final var members = new ArrayList<AnnotationMember>();

        members.add(new AnnotationMember("name", Attribute.constant(openApiParameter.name())));

        if (Utils.isFalse(required)) {
            members.add(new AnnotationMember("required", Attribute.constant(false)));
        }

        return Attribute.compound("org.springframework.web.bind.annotation.RequestHeader", toMap(members));
    }

    private AnnotationMirror createSpringCookieValueAnnotation(final OpenApiParameter openApiParameter) {
        final var required = openApiParameter.required();

        final var members = new ArrayList<AnnotationMember>();

        members.add(new AnnotationMember("name", Attribute.constant(openApiParameter.name())));

        if (Utils.isFalse(required)) {
            members.add(new AnnotationMember("required", Attribute.constant(false)));
        }

        return Attribute.compound("org.springframework.web.bind.annotation.CookieValue", toMap(members));
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
        final Type<?> responseType;

        if (responseTypes.isEmpty()) {
            responseType = VoidType.INSTANCE;
        } else if (responseTypes.size() == 1) {
            responseType = typeUtils.createType(
                    responseTypes.get(0)
            );
        } else {
            responseType = typeUtils.createObjectType();
        }

        final var typeElement = findOrCreateTypeElement(openApiPath, operation);

        final var method = typeElement.addMethod(operationId, responseType);
        createParameters(operation)
                .forEach(method::addParameter);

        postProcessOperation(
                httpMethod,
                path,
                operation,
                method);
    }

    private void postProcessOperation(final HttpMethod httpMethod,
                                      final String path,
                                      final OpenApiOperation operation,
                                      final MethodElement method) {
        final var responseType = method.getReturnType();
        var returnType = typeUtils.createDeclaredType("org.springframework.http.ResponseEntity");

        returnType = returnType.withTypeArgument(
                responseType.isVoidType()
                        ? typeUtils.createVoidType()
                        : responseType
        );

        method.setReturnType(returnType);
        method.addModifier(Modifier.DEFAULT);

        final var summary = operation.summary();
        final var operationId = operation.operationId();
        final var tags = generateUtils.createArrayInitializerExprOfStrings(operation.tags());

        method.addAnnotation(createApiOperationAnnotation(
                List.of(
                        new AnnotationMember("summary", Attribute.constant(summary != null ? summary : "")),
                        new AnnotationMember("operationId", Attribute.constant(operationId)),
                        new AnnotationMember("tags", tags)
                )
        ));

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
    }

    private Attribute.Compound createSecurityRequirementsAnnotation(final List<OpenApiSecurityRequirement> securityRequirements) {
        final List<Attribute> annotations = new ArrayList<>();

        securityRequirements.forEach(securityRequirement -> {
            final var requirements = securityRequirement.requirements();

            if (requirements.size() > 0) {
                final String name = securityRequirement.requirements().keySet().iterator().next();

                final var members = new ArrayList<AnnotationMember>();
                members.add(new AnnotationMember("name", Attribute.constant(name)));

                final var annotation = Attribute.compound(
                        SECURITY_REQUIREMENT_CLASS_NAME,
                        toMap(members)
                );
                annotations.add(annotation);
            }
        });

        final var securityRequirementsMembers = new ArrayList<AnnotationMember>();

        if (annotations.size() > 0) {
            securityRequirementsMembers.add(
                    new AnnotationMember(
                            "value",
                            Attribute.array(annotations)
                    )
            );
        }

        return Attribute.compound(
                "io.swagger.v3.oas.annotations.security.SecurityRequirements",
                toMap(securityRequirementsMembers)
        );
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
