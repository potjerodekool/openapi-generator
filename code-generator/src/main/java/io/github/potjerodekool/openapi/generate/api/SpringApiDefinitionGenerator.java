package io.github.potjerodekool.openapi.generate.api;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;
import io.github.potjerodekool.openapi.generate.*;
import io.github.potjerodekool.openapi.type.OpenApiType;
import io.github.potjerodekool.openapi.util.NodeListCollectors;
import io.github.potjerodekool.openapi.util.Utils;
import io.github.potjerodekool.openapi.*;
import io.github.potjerodekool.openapi.tree.*;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static io.github.potjerodekool.openapi.generate.GenerateHelper.*;
import static io.github.potjerodekool.openapi.util.Utils.requireNonNull;

public class SpringApiDefinitionGenerator {

    private static final String JSON_CONTENT_TYPE = "application/json";

    private static final Logger LOGGER = Logger.getLogger(SpringApiDefinitionGenerator.class.getName());

    private final Types types;

    private final GenerateUtils generateUtils;

    private final Filer filer;
    private final File pathsDir;
    private final Map<String, CompilationUnit> compilationUnitMap = new HashMap<>();

    private final String servletClassName;

    public SpringApiDefinitionGenerator(final OpenApiGeneratorConfig config,
                                        final Types types,
                                        final GenerateUtils generateUtils,
                                        final Filer filer) {
        this.types = types;
        this.generateUtils = generateUtils;
        this.filer = filer;
        this.pathsDir = config.getPathsDir();
        servletClassName = config.isUseJakartaServlet()
                ? "jakarta.servlet.http.HttpServletRequest"
                : "javax.servlet.http.HttpServletRequest";
    }

    public void generate(final OpenApi api) {
        api.paths().forEach(this::processPath);
        generateCode();
    }

    private void generateCode() {
        compilationUnitMap.values().forEach(cu -> {
            try {
                filer.write(cu);
            } catch (final IOException e) {
                LOGGER.log(LogLevel.SEVERE, "Fail to generate code for spring api definition", e);
            }
        });
    }

    private CompilationUnit createCompilationUnitWithInterface(final String packageName,
                                                               final String interfaceName) {
        final var newCU = new CompilationUnit();

        if (!Utils.isNullOrEmpty(packageName)) {
            newCU.setPackageDeclaration(packageName);
        }

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

        final var clazz = (ClassOrInterfaceDeclaration) cu.getTypes().get(0);

        processOperation(HttpMethod.POST, openApiPath.path(), openApiPath.post(), clazz);
        processOperation(HttpMethod.GET, openApiPath.path(), openApiPath.get(), clazz);
        processOperation(HttpMethod.PUT, openApiPath.path(), openApiPath.put(), clazz);
        processOperation(HttpMethod.PATCH, openApiPath.path(), openApiPath.patch(), clazz);
        processOperation(HttpMethod.DELETE, openApiPath.path(), openApiPath.delete(), clazz);
    }

    private AnnotationExpr createApiOperationAnnotation(final List<AnnotationMember> members) {
        return GenerateHelper.createAnnotation("io.swagger.v3.oas.annotations.Operation", members);
    }

    private NormalAnnotationExpr createApiResponsesAnnotation(final OpenApiOperation operation) {
        final var responses = operation.responses().entrySet().stream()
                        .map(this::createApiResponse)
                .toList();

        return GenerateHelper.createAnnotation("io.swagger.v3.oas.annotations.responses.ApiResponses",
                new AnnotationMember("value", createArrayInitializerExprOfAnnotations(responses))
        );
    }

    private NormalAnnotationExpr createApiResponse(final Map.Entry<String, OpenApiResponse> entry) {
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

        final NodeList<Expression> contentList = response.contentMediaType().entrySet().stream()
                        .map(contentMediaType -> createContentAnnotation(contentMediaType.getKey(), contentMediaType.getValue()))
                .collect(NodeListCollectors.collector());

        if (contentList.size() > 0) {
            members.add(new AnnotationMember("content", new ArrayInitializerExpr(contentList)));
        }

        return GenerateHelper.createAnnotation("io.swagger.v3.oas.annotations.responses.ApiResponse", members);
    }

    private AnnotationExpr createContentAnnotation(final String mediaType,
                                                   final OpenApiContent content) {
        final var schemaType = types.createType(content.schema());

        final var examples = content.examples().entrySet().stream()
                .map(exampleEntry -> createExampleObject(exampleEntry.getKey(), exampleEntry.getValue()))
                .toList();

        final var contextMembers = new NodeList<MemberValuePair>();
        contextMembers.add(new MemberValuePair("mediaType", new StringLiteralExpr(mediaType)));

        if (examples.size() > 0) {
            contextMembers.add(new MemberValuePair("examples", GenerateHelper.toExpression(examples)));
        }

        if (generateUtils.isListType(schemaType)) {
            final var typeArg = getFirstTypeArg(schemaType);
            contextMembers.add(new MemberValuePair("array", createArraySchemaAnnotation(typeArg)));
        } else {
            contextMembers.add(new MemberValuePair("schema", createSchemaAnnotation(schemaType, false)));
        }

        return new NormalAnnotationExpr(
                new Name("io.swagger.v3.oas.annotations.media.Content"),
                contextMembers
        );
    }

    private AnnotationExpr createExampleObject(final String name,
                                               final OpenApiExample openApiExample) {
        final var summary = openApiExample.summary();

        final var members = new NodeList<MemberValuePair>();
        members.add(new MemberValuePair("name", new StringLiteralExpr(name)));

        if (summary != null) {
            members.add(new MemberValuePair("summary", new StringLiteralExpr(summary)));
        }

        members.add(new MemberValuePair("value", new StringLiteralExpr(
                escapeJson(openApiExample.value().toString())
        )));

        return new NormalAnnotationExpr(
                new Name("io.swagger.v3.oas.annotations.media.ExampleObject"),
                members
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

    private ArrayInitializerExpr headers(final Map<String, OpenApiHeader> headersMap) {
        final NodeList<Expression> headers =  headersMap.entrySet().stream()
                        .map(entry -> {
                            final var header = entry.getValue();
                            final String description = header.description();
                            final var required = header.required();
                            final var allowEmptyValue = header.allowEmptyValue();
                            final var deprecated = header.deprecated();

                            final var headerType =
                                    types.createType(
                                            header.type()
                                    );

                            final var members = new NodeList<MemberValuePair>();
                            members.add(new MemberValuePair("name", new StringLiteralExpr(entry.getKey())));

                            if (description != null) {
                                members.add(new MemberValuePair("description", new StringLiteralExpr(description)));
                            }

                            if (Utils.isTrue(required)) {
                                members.add(new MemberValuePair("required", new BooleanLiteralExpr(true)));
                            }

                            if (Utils.isTrue(deprecated)) {
                                members.add(new MemberValuePair("deprecated", new BooleanLiteralExpr(true)));
                            }

                            if (Utils.isTrue(allowEmptyValue)) {
                                members.add(new MemberValuePair("allowEmptyValue", new BooleanLiteralExpr(true)));
                            }

                            members.add(new MemberValuePair("schema",
                                    new NormalAnnotationExpr(
                                            new Name("io.swagger.v3.oas.annotations.media.Schema"),
                                            NodeList.nodeList(
                                                    new MemberValuePair("implementation", new ClassExpr(headerType))
                                            )
                                    )
                            ));

                            return new NormalAnnotationExpr(
                                    new Name("io.swagger.v3.oas.annotations.headers.Header"),
                                    members
                            );
                        }).collect(NodeListCollectors.collector());

        return new ArrayInitializerExpr(
                headers
        );
    }


    private AnnotationExpr createMappingAnnotation(final HttpMethod httpMethod,
                                                   final String path,
                                                   final OpenApiOperation operation) {
        final var annotationName = requireNonNull(switch (httpMethod) {
            case POST -> "org.springframework.web.bind.annotation.PostMapping";
            case GET -> "org.springframework.web.bind.annotation.GetMapping";
            case PUT -> "org.springframework.web.bind.annotation.PutMapping";
            case PATCH -> "org.springframework.web.bind.annotation.PatchMapping";
            case DELETE -> "org.springframework.web.bind.annotation.DeleteMapping";
        });

        final var responseMediaTypes = createArrayInitializerExprOfStrings(operation.responses().values()
                .stream().flatMap(it -> it.contentMediaType().keySet().stream())
                .toList()
        );

        final var requestBody = operation.requestBody();
        final var members = new ArrayList<AnnotationMember>();
        members.add(new AnnotationMember("value", path));

        if (requestBody != null && requestBody.contentMediaType().size() > 0) {
            @SuppressWarnings("assignment")
            final var consumes = requestBody.contentMediaType().keySet().stream().toList();
            members.add(new AnnotationMember("consumes", createArrayInitializerExprOfStrings(
                    consumes
            )));
        }

        members.add(new AnnotationMember("produces", responseMediaTypes));

        return GenerateHelper.createAnnotation(
                annotationName,
                members
        );
    }

    private List<Parameter> createParameters(final OpenApiOperation operation) {
        final var parameters = new ArrayList<>(operation.parameters().stream()
                .map(this::createParameter)
                .toList());

        final OpenApiRequestBody requestBody = operation.requestBody();

        if (requestBody != null) {
            final var bodyMediaType = findJsonMediaType(requestBody.contentMediaType());
            final var bodyType = bodyMediaType != null
                    ? types.createType(bodyMediaType)
                    : types.createObjectType();

            final var bodyParameter = new Parameter(bodyType, "body");

            if (requestBody.required() != null) {
                new NormalAnnotationExpr(
                        new Name("org.springframework.web.bind.annotation.RequestBody"),
                        NodeList.nodeList(
                                new MemberValuePair(
                                        "required",
                                        new BooleanLiteralExpr(requestBody.required())
                                )
                        )
                );
            } else {
                bodyParameter.addAnnotation(new MarkerAnnotationExpr("org.springframework.web.bind.annotation.RequestBody"));
            }

            parameters.add(bodyParameter);
        }

        parameters.add(new Parameter(types.createType(servletClassName), "request"));

        return parameters;
    }

    private Parameter createParameter(final OpenApiParameter openApiParameter) {
        final var explode = Boolean.TRUE.equals(openApiParameter.explode());
        var type = types.createType(openApiParameter.type());

        if (explode) {
            type = types.createListType(type);
        }

        final var parameter = new Parameter(type, openApiParameter.name());

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

    private AnnotationExpr createSpringPathVariableAnnotation(final OpenApiParameter openApiParameter) {
        final var required = openApiParameter.required();

        final var members = new NodeList<MemberValuePair>();

        members.add(new MemberValuePair("name", new StringLiteralExpr(openApiParameter.name())));

        if (Utils.isFalse(required)) {
            members.add(new MemberValuePair("required", new BooleanLiteralExpr(false)));
        }

        return new NormalAnnotationExpr(new Name("org.springframework.web.bind.annotation.PathVariable"), members);
    }


    private AnnotationExpr createSpringRequestParamAnnotation(final OpenApiParameter openApiParameter) {
        final var required = openApiParameter.required();

        final var members = new NodeList<MemberValuePair>();

        members.add(new MemberValuePair("name", new StringLiteralExpr(openApiParameter.name())));

        if (Utils.isFalse((required))) {
            members.add(new MemberValuePair("required", new BooleanLiteralExpr(false)));
        }

        return new NormalAnnotationExpr(new Name("org.springframework.web.bind.annotation.RequestParam"), members);
    }

    private AnnotationExpr createApiParamAnnotation(final OpenApiParameter openApiParameter) {
        final var required = openApiParameter.required();
        final var explode = openApiParameter.explode();
        final var allowEmptyValue = openApiParameter.allowEmptyValue();
        final var example = openApiParameter.example();
        final var description = openApiParameter.description();

        final var members = new NodeList<MemberValuePair>();

        members.add(new MemberValuePair("name", new StringLiteralExpr(openApiParameter.name())));

        members.add(new MemberValuePair("in", new FieldAccessExpr(
                new NameExpr("io.swagger.v3.oas.annotations.enums.ParameterIn"),
                openApiParameter.in().name()
        )));

        if (description != null) {
            members.add(new MemberValuePair("description", new StringLiteralExpr(description)));
        }

        if (Utils.isFalse(required)) {
            members.add(new MemberValuePair("required", new BooleanLiteralExpr(false)));
        }

        if (Utils.isTrue(allowEmptyValue)) {
            members.add(new MemberValuePair("allowEmptyValue", new BooleanLiteralExpr(true)));
        }

        if (Utils.isTrue(explode)) {
            members.add(new MemberValuePair("explode", new FieldAccessExpr(
                    new NameExpr("io.swagger.v3.oas.annotations.enums.Explode"),
                    "TRUE"
            )));
        }

        if (example != null) {
            members.add(new MemberValuePair("example", new StringLiteralExpr(example)));
        }

        return new NormalAnnotationExpr(new Name("io.swagger.v3.oas.annotations.Parameter"), members);
    }

    private AnnotationExpr createSpringRequestHeaderAnnotation(final OpenApiParameter openApiParameter) {
        final var required = openApiParameter.required();

        final var members = new NodeList<MemberValuePair>();

        members.add(new MemberValuePair("name", new StringLiteralExpr(openApiParameter.name())));

        if (Utils.isFalse(required)) {
            members.add(new MemberValuePair("required", new BooleanLiteralExpr(false)));
        }

        return new NormalAnnotationExpr(new Name("org.springframework.web.bind.annotation.RequestHeader"), members);
    }

    private AnnotationExpr createSpringCookieValueAnnotation(final OpenApiParameter openApiParameter) {
        final var required = openApiParameter.required();

        final var members = new NodeList<MemberValuePair>();

        members.add(new MemberValuePair("name", new StringLiteralExpr(openApiParameter.name())));

        if (Utils.isFalse(required)) {
            members.add(new MemberValuePair("required", new BooleanLiteralExpr(false)));
        }

        return new NormalAnnotationExpr(new Name("org.springframework.web.bind.annotation.CookieValue"), members);
    }

    private void processOperation(final HttpMethod httpMethod,
                                  final String path,
                                  final @Nullable OpenApiOperation operation,
                                  final ClassOrInterfaceDeclaration clazz) {
        if (operation == null) {
            return;
        }

        final var operationId = operation.operationId();

        if (operationId == null || "".equals(operationId)) {
            throw new IllegalArgumentException();
        }

        final var method = clazz.addMethod(operationId);
        createParameters(operation)
                .forEach(method::addParameter);

        final var responseTypes = resolveResponseTypes(operation);
        final Type responseType;

        if (responseTypes.isEmpty()) {
            responseType = new VoidType();
        } else if (responseTypes.size() == 1) {
            responseType = types.createType(
                    responseTypes.get(0)
            );
        } else {
            responseType = types.createObjectType();
        }

        method.setType(responseType);

        postProcessOperation(
                httpMethod,
                path,
                operation,
                clazz,
                method);
    }

    private void postProcessOperation(final HttpMethod httpMethod,
                                        final String path,
                                        final OpenApiOperation operation,
                                        final ClassOrInterfaceDeclaration clazz,
                                        final MethodDeclaration method) {
        final var responseType = method.getType();
        final var returnType = types.createType("org.springframework.http.ResponseEntity");

        returnType.setTypeArguments(
                responseType.isVoidType()
                        ? types.createVoidType()
                        : responseType
        );

        method.setType(returnType);
        method.addModifier(Modifier.Keyword.DEFAULT);

        final var summary = operation.summary();
        final var operationId = operation.operationId();
        final var tags = createArrayInitializerExprOfStrings(operation.tags());

        method.addAnnotation(createApiOperationAnnotation(
                List.of(
                        new AnnotationMember("summary", summary != null ? summary : ""),
                        new AnnotationMember("operationId", operationId),
                        new AnnotationMember("tags", tags)
                )
        ));

        final var apiResponsesAnnotation = createApiResponsesAnnotation(operation);

        method.addAnnotation(apiResponsesAnnotation);
        method.addAnnotation(createMappingAnnotation(httpMethod, path, operation));

        final var body = new BlockStmt();

        body.addStatement(new ReturnStmt(
                new MethodCallExpr(
                        new MethodCallExpr(
                                new NameExpr("org.springframework.http.ResponseEntity"),
                                "status",
                                NodeList.nodeList(new FieldAccessExpr(
                                        new NameExpr("org.springframework.http.HttpStatus"),
                                        "NOT_IMPLEMENTED"
                                ))
                        ),
                        "build"
                )
        ));

        method.setBody(body);
    }

    private List<OpenApiType> resolveResponseTypes(final OpenApiOperation operation) {
        final Map<String, OpenApiResponse> responses = operation.responses();

        final List<OpenApiType> responseTypes = new ArrayList<>();

        responses.entrySet().stream()
            .filter(entry -> !"default".equals(entry.getKey()))
            .forEach(entry -> {
                final var response = entry.getValue();
                final var contentMediaType = findJsonMediaType(response.contentMediaType());

                if (contentMediaType != null) {
                    responseTypes.add(contentMediaType);
                }
            });
        return responseTypes;
    }

    public static @Nullable OpenApiType findJsonMediaType(final Map<String, OpenApiContent> contentMediaType) {
        final var content = contentMediaType.get(JSON_CONTENT_TYPE);
        return content != null ? content.schema() : null;
    }

}
