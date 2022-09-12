package io.github.potjerodekool.openapi.internal.generate.api;

import io.github.potjerodekool.openapi.HttpMethod;
import io.github.potjerodekool.openapi.Language;
import io.github.potjerodekool.openapi.OpenApiGeneratorConfig;
import io.github.potjerodekool.openapi.internal.Filer;
import io.github.potjerodekool.openapi.internal.ast.CompilationUnit;
import io.github.potjerodekool.openapi.internal.ast.Modifier;
import io.github.potjerodekool.openapi.internal.ast.TypeUtils;
import io.github.potjerodekool.openapi.internal.ast.element.MethodElement;
import io.github.potjerodekool.openapi.internal.ast.element.PackageElement;
import io.github.potjerodekool.openapi.internal.ast.element.TypeElement;
import io.github.potjerodekool.openapi.internal.ast.element.VariableElement;
import io.github.potjerodekool.openapi.internal.ast.expression.*;
import io.github.potjerodekool.openapi.internal.ast.statement.BlockStatement;
import io.github.potjerodekool.openapi.internal.ast.statement.ReturnStatement;
import io.github.potjerodekool.openapi.internal.ast.type.DeclaredType;
import io.github.potjerodekool.openapi.internal.ast.type.Type;
import io.github.potjerodekool.openapi.internal.ast.type.VoidType;
import io.github.potjerodekool.openapi.internal.generate.AnnotationMember;
import io.github.potjerodekool.openapi.internal.generate.GenerateUtils;
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

import static io.github.potjerodekool.openapi.internal.util.Utils.requireNonNull;

public class SpringApiDefinitionGenerator {

    private static final String JSON_CONTENT_TYPE = "application/json";

    private static final String CONTENT_CLASS_NAME = "io.swagger.v3.oas.annotations.media.Content";

    private static final String SECURITY_REQUIREMENT_CLASS_NAME = "io.swagger.v3.oas.annotations.security.SecurityRequirement";

    private static final String API_RESPONSE_CLASS_NAME = "io.swagger.v3.oas.annotations.responses.ApiResponse";

    private static final String EXAMPLE_OBJECT_CLASS_NAME = "io.swagger.v3.oas.annotations.media.ExampleObject";

    private static final Logger LOGGER = Logger.getLogger(SpringApiDefinitionGenerator.class.getName());

    private final Language language;
    private final TypeUtils typeUtils;

    private final GenerateUtils generateUtils;

    private final Filer filer;
    private final File pathsDir;
    private final Map<String, CompilationUnit> compilationUnitMap = new HashMap<>();

    private final String servletClassName;

    private final String validAnnotationClassName;

    public SpringApiDefinitionGenerator(final OpenApiGeneratorConfig config,
                                        final TypeUtils typeUtils,
                                        final GenerateUtils generateUtils,
                                        final Filer filer) {
        this.language = config.getLanguage();
        this.typeUtils = typeUtils;
        this.generateUtils = generateUtils;
        this.filer = filer;
        this.pathsDir = config.getPathsDir();
        servletClassName = config.isFeatureEnabled(OpenApiGeneratorConfig.FEATURE_JAKARTA_SERVLET)
                ? "jakarta.servlet.http.HttpServletRequest"
                : "javax.servlet.http.HttpServletRequest";

        final var validationBasePackage = config.isFeatureEnabled(OpenApiGeneratorConfig.FEATURE_JAKARTA_VALIDATION) ? "jakarta" : "javax";
        this.validAnnotationClassName = validationBasePackage + ".validation.Valid";
    }

    public void generate(final OpenApi api) {
        api.paths().forEach(this::processPath);
        generateCode();
    }

    private void generateCode() {
        compilationUnitMap.values().forEach(cu -> {
            try {
                filer.write(cu, language);
            } catch (final IOException e) {
                LOGGER.log(LogLevel.SEVERE, "Fail to generate code for spring api definition", e);
            }
        });
    }

    private CompilationUnit createCompilationUnitWithInterface(final String packageName,
                                                               final String interfaceName) {
        final var newCU = new CompilationUnit(Language.JAVA);

        newCU.setPackageElement(PackageElement.create(packageName));

        newCU.addInterface(interfaceName);
        return newCU;
    }

    private void processPath(final OpenApiPath openApiPath) {
        final var pathUri = Utils.toUriString(this.pathsDir);
        final var creatingReference = openApiPath.creatingReference();
        final var ref = creatingReference.substring(pathUri.length());
        final var qualifiedName = Utils.resolveQualified(ref);
        final var packageName = qualifiedName.packageName();
        final var name = qualifiedName.simpleName();
        final var apiName = Utils.firstUpper(name) + "Api";
        final var qualifiedApiName = packageName + "." + apiName;

        final var cu = this.compilationUnitMap.computeIfAbsent(qualifiedApiName, (key) ->
                createCompilationUnitWithInterface(packageName, apiName));

        final var typeElement = (TypeElement) cu.getElements().get(0);

        processOperation(HttpMethod.POST, openApiPath.path(), openApiPath.post(), typeElement);
        processOperation(HttpMethod.GET, openApiPath.path(), openApiPath.get(), typeElement);
        processOperation(HttpMethod.PUT, openApiPath.path(), openApiPath.put(), typeElement);
        processOperation(HttpMethod.PATCH, openApiPath.path(), openApiPath.patch(), typeElement);
        processOperation(HttpMethod.DELETE, openApiPath.path(), openApiPath.delete(), typeElement);
    }

    private AnnotationExpression createApiOperationAnnotation(final List<AnnotationMember> members) {
        return generateUtils.createAnnotation("io.swagger.v3.oas.annotations.Operation", members);
    }

    private AnnotationExpression createApiResponsesAnnotation(final OpenApiOperation operation) {
        final var responses = operation.responses().entrySet().stream()
                        .map(this::createApiResponse)
                .toList();

        return generateUtils.createAnnotation("io.swagger.v3.oas.annotations.responses.ApiResponses",
                new AnnotationMember("value", generateUtils.createArrayInitializerExprOfAnnotations(responses))
        );
    }

    private AnnotationExpression createApiResponse(final Map.Entry<String, OpenApiResponse> entry) {
        final var response = entry.getValue();
        final var description = response.description();

        final var members = new ArrayList<AnnotationMember>();
        members.add(new AnnotationMember("responseCode", entry.getKey()));
        members.add(new AnnotationMember("description", description != null ? description : ""));

        final var headersMap = response.headers();

        if (headersMap.size() > 0) {
            final var responseHeaders = headers(headersMap);
            members.add(new AnnotationMember(
                    "headers", responseHeaders
            ));
        }

        final List<Expression> contentList = response.contentMediaType().entrySet().stream()
                        .map(contentMediaType -> (Expression) createContentAnnotation(contentMediaType.getKey(), contentMediaType.getValue()))
                .toList();

        if (contentList.size() > 0) {
            members.add(new AnnotationMember("content", new ArrayInitializerExpression(contentList)));
        }

        return generateUtils.createAnnotation(API_RESPONSE_CLASS_NAME,
                members
        );
    }

    private AnnotationExpression createContentAnnotation(final String mediaType,
                                                         final OpenApiContent content) {
        final var schemaType = typeUtils.createType(content.schema());

        final var examples = content.examples().entrySet().stream()
                .map(exampleEntry -> createExampleObject(exampleEntry.getKey(), exampleEntry.getValue()))
                .toList();

        final var contextMembers = new ArrayList<AnnotationMember>();
        contextMembers.add(new AnnotationMember("mediaType", mediaType));

        if (examples.size() > 0) {
            contextMembers.add(new AnnotationMember("examples", generateUtils.createArrayInitializerExpr(examples)));
        }

        if (typeUtils.isListType(schemaType)) {
            final var typeArg = generateUtils.getFirstTypeArg(schemaType);
            contextMembers.add(new AnnotationMember("array", generateUtils.createArraySchemaAnnotation(typeArg)));
        } else if (typeUtils.isMapType(schemaType)) {
            final var schemaPropertyAnnotation = new AnnotationExpression(
                    "io.swagger.v3.oas.annotations.media.SchemaProperty",
                   toMap(List.of(new AnnotationMember("name", "additionalProp1")))
            );

            final var additionalProperties = Utils.requireNonNull(content.schema().additionalProperties());
            final var type = additionalProperties.type();
            final var typeName = type.name();
            final var format = type.format();

            contextMembers.add(new AnnotationMember("schemaProperties",
                    new ArrayInitializerExpression(schemaPropertyAnnotation)
            ));
            contextMembers.add(new AnnotationMember("additionalPropertiesSchema", generateUtils.createSchemaAnnotation(typeName, format)));
        } else {
            contextMembers.add(new AnnotationMember("schema", generateUtils.createSchemaAnnotation(schemaType, false)));
        }

        return new AnnotationExpression(
                CONTENT_CLASS_NAME,
                toMap(contextMembers)
        );
    }

    private Map<String, Expression> toMap(final List<AnnotationMember> list) {
        return list.stream()
                .collect(Collectors.toMap(
                        AnnotationMember::name,
                        AnnotationMember::value
                ));
    }

    private AnnotationExpression createExampleObject(final String name,
                                               final OpenApiExample openApiExample) {
        final var summary = openApiExample.summary();

        final var members = new ArrayList<AnnotationMember>();
        members.add(new AnnotationMember("name", name));

        if (summary != null) {
            members.add(new AnnotationMember("summary", summary));
        }

        members.add(new AnnotationMember("value",
                escapeJson(openApiExample.value().toString()
        )));

        return new AnnotationExpression(
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

    private ArrayInitializerExpression headers(final Map<String, OpenApiHeader> headersMap) {
        final List<Expression> headers =  headersMap.entrySet().stream()
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
                            members.add(new AnnotationMember("name", entry.getKey()));

                            if (description != null) {
                                members.add(new AnnotationMember("description", description));
                            }

                            if (Utils.isTrue(required)) {
                                members.add(new AnnotationMember("required", true));
                            }

                            if (Utils.isTrue(deprecated)) {
                                members.add(new AnnotationMember("deprecated", true));
                            }

                            if (Utils.isTrue(allowEmptyValue)) {
                                members.add(new AnnotationMember("allowEmptyValue", true));
                            }

                            members.add(new AnnotationMember("schema",
                                    new AnnotationExpression(
                                            "io.swagger.v3.oas.annotations.media.Schema",
                                            toMap(List.of(
                                                    new AnnotationMember("implementation", headerType)
                                                )
                                            )
                                    )
                            ));

                            return (Expression) new AnnotationExpression(
                                    "io.swagger.v3.oas.annotations.headers.Header",
                                    toMap(members)
                            );
                        }).toList();

        return new ArrayInitializerExpression(headers);
    }


    private AnnotationExpression createMappingAnnotation(final HttpMethod httpMethod,
                                                   final String path,
                                                   final OpenApiOperation operation) {
        final var annotationName = requireNonNull(switch (httpMethod) {
            case POST -> "org.springframework.web.bind.annotation.PostMapping";
            case GET -> "org.springframework.web.bind.annotation.GetMapping";
            case PUT -> "org.springframework.web.bind.annotation.PutMapping";
            case PATCH -> "org.springframework.web.bind.annotation.PatchMapping";
            case DELETE -> "org.springframework.web.bind.annotation.DeleteMapping";
        });

        final var responseMediaTypes = generateUtils.createArrayInitializerExprOfStrings(operation.responses().values()
                .stream().flatMap(it -> it.contentMediaType().keySet().stream())
                .toList()
        );

        final var requestBody = operation.requestBody();
        final var members = new ArrayList<AnnotationMember>();
        members.add(new AnnotationMember("value",
                new ArrayInitializerExpression(LiteralExpression.createStringLiteralExpression(path))
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

            final var bodyParameter = VariableElement.createParameter("body", bodyType);

            if (addRequestBodyAnnotation) {
                bodyParameter.addAnnotation(validAnnotationClassName);
            }

            if (isMultipart) {
                final String contentMediaType = requestBody.contentMediaType().keySet().iterator().next();
                final var schema = requestBody.contentMediaType().get(contentMediaType).schema();
                final String propertyName = schema.properties().keySet().iterator().next();
                final var required = schema.properties().get(propertyName).required();

                final var members = new ArrayList<AnnotationMember>();
                members.add(new AnnotationMember("value", propertyName));

                if (!required) {
                    members.add(new AnnotationMember("required", false));
                }

                bodyParameter.addAnnotation(new AnnotationExpression(
                        "org.springframework.web.bind.annotation.RequestParam",
                        toMap(members)
                ));

                bodyParameter.setSimpleName(propertyName);
            }

            if (requestBody.required() != null) {
                new AnnotationExpression(
                        "org.springframework.web.bind.annotation.RequestBody",
                        toMap(List.of(
                                new AnnotationMember(
                                        "required",
                                        requestBody.required()
                                )
                            )
                      )
                );
            } else if (addRequestBodyAnnotation) {
                bodyParameter.addAnnotation("org.springframework.web.bind.annotation.RequestBody");
            }

            parameters.add(bodyParameter);
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
        var type = typeUtils.createType(openApiParameter.type());

        if (explode) {
            final DeclaredType dt;
            if (type.isPrimitiveType()) {
                dt = typeUtils.getBoxedType(type);
            } else {
                dt = (DeclaredType) type;
            }
            type = typeUtils.createListType(dt);
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

    private AnnotationExpression createSpringPathVariableAnnotation(final OpenApiParameter openApiParameter) {
        final var required = openApiParameter.required();

        final var members = new ArrayList<AnnotationMember>();

        members.add(new AnnotationMember("name", openApiParameter.name()));

        if (Utils.isFalse(required)) {
            members.add(new AnnotationMember("required", false));
        }

        return new AnnotationExpression("org.springframework.web.bind.annotation.PathVariable", toMap(members));
    }

    private AnnotationExpression createSpringRequestParamAnnotation(final OpenApiParameter openApiParameter) {
        final var required = openApiParameter.required();

        final var members = new ArrayList<AnnotationMember>();

        members.add(new AnnotationMember("name", openApiParameter.name()));

        if (Utils.isFalse((required))) {
            members.add(new AnnotationMember("required", false));
        }

        return new AnnotationExpression("org.springframework.web.bind.annotation.RequestParam", toMap(members));
    }

    private AnnotationExpression createApiParamAnnotation(final OpenApiParameter openApiParameter) {
        final var required = openApiParameter.required();
        final var explode = openApiParameter.explode();
        final var allowEmptyValue = openApiParameter.allowEmptyValue();
        final var example = openApiParameter.example();
        final var description = openApiParameter.description();

        final var members = new ArrayList<AnnotationMember>();

        members.add(new AnnotationMember("name", openApiParameter.name()));

        members.add(new AnnotationMember("in", new FieldAccessExpression(
                new NameExpression("io.swagger.v3.oas.annotations.enums.ParameterIn"),
                openApiParameter.in().name()
        )));

        if (description != null) {
            members.add(new AnnotationMember("description", description));
        }

        if (Utils.isFalse(required)) {
            members.add(new AnnotationMember("required", false));
        }

        if (Utils.isTrue(allowEmptyValue)) {
            members.add(new AnnotationMember("allowEmptyValue", true));
        }

        if (Utils.isTrue(explode)) {
            members.add(new AnnotationMember("explode", new FieldAccessExpression(
                    new NameExpression("io.swagger.v3.oas.annotations.enums.Explode"),
                    "TRUE"
            )));
        }

        if (example != null) {
            members.add(new AnnotationMember("example", example));
        }

        return new AnnotationExpression("io.swagger.v3.oas.annotations.Parameter", toMap(members));
    }

    private AnnotationExpression createSpringRequestHeaderAnnotation(final OpenApiParameter openApiParameter) {
        final var required = openApiParameter.required();

        final var members = new ArrayList<AnnotationMember>();

        members.add(new AnnotationMember("name", openApiParameter.name()));

        if (Utils.isFalse(required)) {
            members.add(new AnnotationMember("required", false));
        }

        return new AnnotationExpression("org.springframework.web.bind.annotation.RequestHeader", toMap(members));
    }

    private AnnotationExpression createSpringCookieValueAnnotation(final OpenApiParameter openApiParameter) {
        final var required = openApiParameter.required();

        final var members = new ArrayList<AnnotationMember>();

        members.add(new AnnotationMember("name", openApiParameter.name()));

        if (Utils.isFalse(required)) {
            members.add(new AnnotationMember("required", false));
        }

        return new AnnotationExpression("org.springframework.web.bind.annotation.CookieValue", toMap(members));
    }

    private void processOperation(final HttpMethod httpMethod,
                                  final String path,
                                  final @Nullable OpenApiOperation operation,
                                  final TypeElement typeElement) {
        if (operation == null) {
            return;
        }

        final var operationId = operation.operationId();

        if (operationId == null || "".equals(operationId)) {
            throw new IllegalArgumentException();
        }

        final var method = typeElement.addMethod(operationId);
        createParameters(operation)
                .forEach(method::addParameter);

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

        method.setReturnType(responseType);

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
                        new AnnotationMember("summary", summary != null ? summary : ""),
                        new AnnotationMember("operationId", operationId),
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

    private AnnotationExpression createSecurityRequirementsAnnotation(final List<OpenApiSecurityRequirement> securityRequirements) {
        final List<Expression> annotations = new ArrayList<>();

        securityRequirements.forEach(securityRequirement -> {
            final var requirements = securityRequirement.requirements();

            if (requirements.size() > 0) {
                final String name = securityRequirement.requirements().keySet().iterator().next();

                final var members = new ArrayList<AnnotationMember>();
                members.add(new AnnotationMember("name", name));

                final var annotation = new AnnotationExpression(
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
                            new ArrayInitializerExpression(annotations)
                    )
            );
        }

        return new AnnotationExpression(
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
