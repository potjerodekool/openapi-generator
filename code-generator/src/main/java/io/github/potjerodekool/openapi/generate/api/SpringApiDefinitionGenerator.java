package io.github.potjerodekool.openapi.generate.api;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;
import io.github.potjerodekool.openapi.generate.GenerateHelper;
import io.github.potjerodekool.openapi.generate.Types;
import io.github.potjerodekool.openapi.type.OpenApiType;
import io.github.potjerodekool.openapi.util.NodeListCollectors;
import io.github.potjerodekool.openapi.util.Utils;
import io.github.potjerodekool.openapi.*;
import io.github.potjerodekool.openapi.generate.AnnotationMember;
import io.github.potjerodekool.openapi.generate.Constants;
import io.github.potjerodekool.openapi.tree.*;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static io.github.potjerodekool.openapi.generate.GenerateHelper.createArrayInitializerExprOfAnnotations;
import static io.github.potjerodekool.openapi.generate.GenerateHelper.createArrayInitializerExprOfStrings;
import static io.github.potjerodekool.openapi.util.Utils.requireNonNull;

public class SpringApiDefinitionGenerator {

    private static final Logger LOGGER = Logger.getLogger(SpringApiDefinitionGenerator.class.getName());

    private final Types types;
    private final Filer filer;
    private final File pathsDir;
    private final Map<String, CompilationUnit> compilationUnitMap = new HashMap<>();

    private final String servletClassName;

    public SpringApiDefinitionGenerator(final OpenApiGeneratorConfig config,
                                        final Types types,
                                        final Filer filer) {
        this.types = types;
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

    private void processPath(final OpenApiPath openApiPath) {
        final var pathUri = this.pathsDir.toURI().toString();
        final var creatingReference = openApiPath.creatingReference();
        final var ref = creatingReference.substring(pathUri.length());
        final var qualifiedName = Utils.resolveQualified(ref);
        final var packageName = qualifiedName.packageName();
        final var name = qualifiedName.simpleName();
        final var apiName = Utils.firstUpper(name) + "Api";
        final var qualifiedApiName = packageName + "." + apiName;

        final var cu = this.compilationUnitMap.computeIfAbsent(qualifiedApiName, (key) -> {
            final var newCU = new CompilationUnit();

            if (!Utils.isNullOrEmpty(packageName)) {
                newCU.setPackageDeclaration(packageName);
            }

            newCU.addInterface(apiName);
            return newCU;
        });

        final var clazz = (ClassOrInterfaceDeclaration) cu.getTypes().get(0);

        processOperation(HttpMethod.POST, openApiPath.path(), openApiPath.post(), clazz);
        processOperation(HttpMethod.GET, openApiPath.path(), openApiPath.get(), clazz);
        processOperation(HttpMethod.PUT, openApiPath.path(), openApiPath.put(), clazz);
        processOperation(HttpMethod.PATCH, openApiPath.path(), openApiPath.patch(), clazz);
        processOperation(HttpMethod.DELETE, openApiPath.path(), openApiPath.delete(), clazz);
    }

    private AnnotationExpr createApiOperationAnnotation(final List<AnnotationMember> members) {
        return GenerateHelper.createAnnotation("io.swagger.annotations.ApiOperation", members);
    }

    private NormalAnnotationExpr createApiResponsesAnnotation(final OpenApiOperation operation) {
        final var responses = operation.responses().entrySet().stream()
                .filter(it -> !"default".equals(it.getKey()))
                        .map(entry -> {
                            final var response = entry.getValue();
                            final var type = getResponseType(response);
                            final var message = response.description();

                            final var members = new ArrayList<AnnotationMember>();
                            members.add(new AnnotationMember("code", Integer.parseInt(entry.getKey())));
                            members.add(new AnnotationMember("message", message != null ? message : ""));

                            if (!type.isVoidType()) {
                                members.add(new AnnotationMember("response", new ClassExpr(type)));
                            }

                            final var headersMap = response.headers();

                            if (headersMap.size() > 0) {
                                final var responseHeaders = headers(headersMap);
                                members.add(new AnnotationMember(
                                        "responseHeaders", responseHeaders
                                ));
                            }

                            return GenerateHelper.createAnnotation("io.swagger.annotations.ApiResponse", members);
                        })
                .toList();

        return GenerateHelper.createAnnotation("io.swagger.annotations.ApiResponses",
                new AnnotationMember("value", createArrayInitializerExprOfAnnotations(responses))
        );
    }

    private ArrayInitializerExpr headers(final Map<String, OpenApiHeader> headersMap) {
        final NodeList<Expression> headers =  headersMap.entrySet().stream()
                        .map(entry -> {
                            final var headerType =
                                    types.createType(
                                            entry.getValue().type()
                                    );

                            return GenerateHelper.createAnnotation(
                                    "ResponseHeader",
                                    new AnnotationMember("name", entry.getKey()),
                                    new AnnotationMember("response", new ClassExpr(headerType))
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
                    : types.createType(Constants.OBJECT_CLASS_NAME);

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
        final var type = types.createType(openApiParameter.type());
        return new Parameter(
                type,
                openApiParameter.name()
        );
    }

    private Type getResponseType(final OpenApiResponse response) {
        final var contentMediaType = response.contentMediaType().get(Constants.JSON_CONTENT_TYPE);

        if (contentMediaType == null) {
            return types.createType(Constants.VOID_CLASS_NAME);
        }

        return types.createType(contentMediaType);
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
            responseType = types.createType(Constants.OBJECT_CLASS_NAME);
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
                        ? types.createType(Constants.VOID_CLASS_NAME)
                        : responseType
        );

        method.setType(returnType);
        method.addModifier(Modifier.Keyword.DEFAULT);

        final var summary = operation.summary();
        final var operationId = operation.operationId();
        final var tags = createArrayInitializerExprOfStrings(operation.tags());

        method.addAnnotation(createApiOperationAnnotation(
                List.of(
                        new AnnotationMember("value", summary != null ? summary : ""),
                        new AnnotationMember("nickname", operationId),
                        new AnnotationMember("tags", tags)
                )
        ));

        final var apiResponsesAnnotation = createApiResponsesAnnotation(operation);

        method.addAnnotation(apiResponsesAnnotation);
        method.addAnnotation(createMappingAnnotation(httpMethod, path, operation));

        final var body = new BlockStmt();

        clazz.findCompilationUnit().ifPresent(cu -> {
            cu.addImport("org.springframework.web.client.HttpServerErrorException");
            cu.addImport("org.springframework.http.HttpStatus");
            cu.addImport("org.springframework.http.HttpHeaders");
        });

        body.addStatement(
                new ThrowStmt(
                        new MethodCallExpr(
                                new NameExpr("HttpServerErrorException"),
                                "create",
                                NodeList.nodeList(
                                        new FieldAccessExpr(
                                                new NameExpr("HttpStatus"),
                                                "NOT_IMPLEMENTED"
                                        ),
                                        new StringLiteralExpr("not implemented"),
                                        new ObjectCreationExpr().setType(
                                                types.createType("HttpHeaders")
                                        ),
                                        new ArrayCreationExpr().setElementType(
                                                PrimitiveType.byteType()
                                        ),
                                        new NullLiteralExpr()
                                )
                        )
                )
        );

        method.setBody(body);
    }

    private List<OpenApiType> resolveResponseTypes(final OpenApiOperation operation) {
        final Map<String, OpenApiResponse> responses = operation.responses();

        final List<OpenApiType> responseTypes = new ArrayList<>();

        responses.values().forEach(response -> {
            final var contentMediaType = response.contentMediaType().get(Constants.JSON_CONTENT_TYPE);

            if (contentMediaType != null) {
                responseTypes.add(contentMediaType);
            }
        });

        return responseTypes;
    }

    public static @Nullable OpenApiType findJsonMediaType(final Map<String, OpenApiType> contentMediaType) {
        return contentMediaType.get(Constants.JSON_CONTENT_TYPE);
    }

}
