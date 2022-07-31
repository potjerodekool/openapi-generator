package com.github.potjerodekool.openapi.generate.api;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.potjerodekool.openapi.*;
import com.github.potjerodekool.openapi.generate.GenerateHelper;
import com.github.potjerodekool.openapi.tree.OpenApi;
import com.github.potjerodekool.openapi.tree.OpenApiHeader;
import com.github.potjerodekool.openapi.tree.OpenApiOperation;
import com.github.potjerodekool.openapi.tree.OpenApiPath;
import com.github.potjerodekool.openapi.util.MapBuilder;
import com.github.potjerodekool.openapi.util.Utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.github.potjerodekool.openapi.util.Utils.requireNonNull;

public class SpringApiDefinitionGenerator extends AbstractSpringGenerator {

    private static final Logger LOGGER = Logger.getLogger(SpringApiDefinitionGenerator.class.getName());
    private final Filer filer;
    private final File pathsDir;
    private final Map<String, CompilationUnit> compilationUnitMap = new HashMap<>();

    public SpringApiDefinitionGenerator(final OpenApiGeneratorConfig config,
                                        final Filer filer) {
        super(config);
        this.filer = filer;
        this.pathsDir = requireNonNull(config.getPathsDir());
    }

    @Override
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
        final var packageNameAndName = Utils.resolvePackageNameAndName(ref);
        final var packageName = packageNameAndName.first();
        final var name = packageNameAndName.second();
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

    private AnnotationExpr createApiOperationAnnotation(final Map<String, Object> members) {
        return GenerateHelper.createAnnotation("io.swagger.annotations.ApiOperation", members);
    }

    private NormalAnnotationExpr createApiResponsesAnnotation(final OpenApiOperation operation) {
        final var responses = operation.responses().entrySet().stream()
                .filter(it -> !"default".equals(it.getKey()))
                        .map(entry -> {
                            final var response = entry.getValue();
                            final var type = getResponseType(response);
                            final var message = response.description();

                            final var mapBuilder = new MapBuilder<String, Object>()
                                    .entry("code", Integer.parseInt(entry.getKey()))
                                    .entry("message", message != null ? message : "");

                            if (!type.isVoidType()) {
                                mapBuilder.entry("response", new ClassExpr(type));
                            }

                            final var headersMap = response.headers();

                            if (headersMap.size() > 0) {
                                final var responseHeaders = headers(headersMap);
                                mapBuilder.entry("responseHeaders", responseHeaders);
                            }

                            return GenerateHelper.createAnnotation(
                                    "io.swagger.annotations.ApiResponse",
                                    mapBuilder.build()
                            );
                        })
                .toList();

        return GenerateHelper.createAnnotation("io.swagger.annotations.ApiResponses",
                Map.of(
                        "value", GenerateHelper.createArrayInitializerExpr(responses)
                ));
    }

    private ArrayInitializerExpr headers(final Map<String, OpenApiHeader> headersMap) {
        final NodeList<Expression> headers =  headersMap.entrySet().stream()
                        .map(entry -> {
                            final var header = entry.getValue();
                            final var type = entry.getValue().type();

                            final var headerType =
                                    types.createType(
                                            entry.getValue().type(),
                                            true
                                    );

                            return GenerateHelper.createAnnotation(
                                    "ResponseHeader",
                                    Map.of(
                                            "name", entry.getKey(),
                                            "response", new ClassExpr(headerType)
                                    )
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

        final var responseMediaTypes = GenerateHelper.createArrayInitializerExpr(operation.responses().values()
                .stream().flatMap(it -> it.contentMediaType().keySet().stream())
                .toList()
        );

        final var requestBody = operation.requestBody();
        final var members = new LinkedHashMap<String, Object>();
        members.put("value", path);

        if (requestBody != null && requestBody.contentMediaType().size() > 0) {
            members.put("consumes",
                    GenerateHelper.createArrayInitializerExpr(
                            requireNonNull(
                                    requestBody.contentMediaType().keySet().stream().toList()
                        )
                    )
            );
        }

        members.put("produces", responseMediaTypes);

        return GenerateHelper.createAnnotation(
                annotationName,
                members
        );
    }

    @Override
    protected void postProcessOperation(final HttpMethod httpMethod,
                                        final String path,
                                        final OpenApiOperation operation,
                                        final ClassOrInterfaceDeclaration clazz,
                                        final MethodDeclaration method) {
        final var responseType = method.getType();
        final var returnType = types.createType("org.springframework.http.ResponseEntity");

        returnType.setTypeArguments(
                responseType.isVoidType()
                        ? types.createType("java.lang.Void")
                        : responseType
        );

        method.setType(returnType);
        method.addModifier(Modifier.Keyword.DEFAULT);

        final var summary = operation.summary();
        final var operationId = operation.operationId();
        final var tags = GenerateHelper.createArrayInitializerExpr(operation.tags());

        method.addAnnotation(createApiOperationAnnotation(new MapBuilder<String, Object>()
                .entry("value", summary != null ? summary : "")
                .entry("nickname", operationId)
                .entry("tags", tags)
                .build()
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

}
