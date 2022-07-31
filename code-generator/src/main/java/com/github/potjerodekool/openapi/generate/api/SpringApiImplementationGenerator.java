package com.github.potjerodekool.openapi.generate.api;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.potjerodekool.openapi.*;
import com.github.potjerodekool.openapi.generate.GenerateHelper;
import com.github.potjerodekool.openapi.generate.JavaTypes;
import com.github.potjerodekool.openapi.generate.Types;
import com.github.potjerodekool.openapi.tree.*;
import com.github.potjerodekool.openapi.type.OpenApiStandardType;
import com.github.potjerodekool.openapi.util.GenerateException;
import com.github.potjerodekool.openapi.util.Utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.github.potjerodekool.openapi.generate.api.ApiCodeGeneratorUtils.*;
import static com.github.potjerodekool.openapi.util.Utils.requireNonNull;

public class SpringApiImplementationGenerator extends AbstractSpringGenerator {

    private static final Logger LOGGER = Logger.getLogger(SpringApiDefinitionGenerator.class.getName());
    private final Filer filer;
    private final File pathsDir;
    private final Map<String, CompilationUnit> compilationUnitMap = new HashMap<>();
    private final Types types = new JavaTypes();

    public SpringApiImplementationGenerator(final OpenApiGeneratorConfig config,
                                            final Filer filer) {
        super(config);
        this.filer = filer;
        this.pathsDir = requireNonNull(config.getPathsDir());
    }

    @Override
    public void generate(final OpenApi api) {
        final var paths = api.paths();
        paths.forEach(this::processPath);
        generateCode();

        if (!paths.isEmpty()) {
            generateApiUtils(api.paths().get(0));
        }

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
        if (!creatingReference.startsWith(pathUri)) {
            throw new GenerateException(String.format("Creating reference %s doesn't start with %s", creatingReference, pathUri));
        }
        final var ref = creatingReference.substring(pathUri.length());
        final var packageNameAndName = Utils.resolvePackageNameAndName(ref);
        final var packageName = packageNameAndName.first();
        final var name = packageNameAndName.second();
        final var apiName = Utils.firstUpper(name) + "Api";
        final var implName = Utils.firstUpper(name) + "Impl";
        final var delegatename = Utils.firstUpper(name) + "Delegate";
        final var qualifiedImplName = packageName + "." + implName;

        final var cu = this.compilationUnitMap.computeIfAbsent(qualifiedImplName, (key) -> {
            final var newCU = new CompilationUnit();

            if (!Utils.isNullOrEmpty(packageName)) {
                newCU.setPackageDeclaration(packageName);
            }

            final var implClass = newCU.addClass(implName);
            implClass.addImplementedType(apiName);
            implClass.addMarkerAnnotation("org.springframework.web.bind.annotation.RestController");
            implClass.addMarkerAnnotation("org.springframework.web.bind.annotation.CrossOrigin");

            final var delegateType = new ClassOrInterfaceType().setName(delegatename);

            implClass.addField(
                    delegateType,
                    "delegate",
                    Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL
            );

            final var constructor = implClass.addConstructor(Modifier.Keyword.PUBLIC);
            final var parameter = new Parameter(delegateType, "delegate").addModifier(Modifier.Keyword.FINAL);

            constructor.addParameter(parameter);
            final var body = new BlockStmt();

            body.addStatement(new AssignExpr(
                    new FieldAccessExpr(new ThisExpr(), "delegate"),
                    new NameExpr("delegate"),
                    AssignExpr.Operator.ASSIGN
            ));

            constructor.setBody(body);

            return newCU;
        });

        final var clazz = (ClassOrInterfaceDeclaration) cu.getTypes().get(0);

        processOperation(HttpMethod.POST, openApiPath.path(), openApiPath.post(), clazz);
        processOperation(HttpMethod.GET, openApiPath.path(), openApiPath.get(), clazz);
        processOperation(HttpMethod.PUT, openApiPath.path(), openApiPath.put(), clazz);
        processOperation(HttpMethod.PATCH, openApiPath.path(), openApiPath.patch(), clazz);
        processOperation(HttpMethod.DELETE, openApiPath.path(), openApiPath.delete(), clazz);
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
        method.addModifier(Modifier.Keyword.PUBLIC);
        method.addMarkerAnnotation("java.lang.Override");

        NodeList<Expression> parameterNames = method.getParameters()
                .stream()
                .map(parameter -> new NameExpr(parameter.getNameAsString()))
                .collect(NodeListCollectors.collector());

        final var delegateCall = new MethodCallExpr(
                new FieldAccessExpr(
                        new ThisExpr(),
                        "delegate"
                ),
                method.getName(),
                parameterNames
        );

        switch (httpMethod) {
            case POST -> {
                if (hasCreateResponseCode(operation.responses())) {
                    fillBodyForCreated(
                            operation,
                            delegateCall,
                            method
                    );
                } else {
                    fillBodyForPost(
                            delegateCall,
                            method
                    );
                }
            }
            case GET -> {
                final var body = new BlockStmt();
                body.addStatement(
                        new ReturnStmt(
                                new MethodCallExpr(
                                        new NameExpr("ResponseEntity"),
                                        "ok",
                                        NodeList.nodeList(delegateCall)
                                )
                        )
                );
                method.setBody(body);
            }
            case PUT, PATCH, DELETE -> {
                final var body = new BlockStmt();
                body.addStatement(delegateCall);
                body.addStatement(
                        new ReturnStmt(
                                new MethodCallExpr(
                                        new MethodCallExpr(new NameExpr("ResponseEntity"), "noContent"),
                                        "build"
                                )
                        )
                );
                method.setBody(body);
            }
        }
    }

    private void fillBodyForCreated(final OpenApiOperation operation,
                                    final MethodCallExpr delegateCall,
                                    final MethodDeclaration methodDeclaration) {
        final var body = new BlockStmt();

        /* ResponseEntity.created(
                            ApiUtils.createLocation(delegate.createSomething(arg, arg2))
                           ).build();
                         */
        final var requestBody = operation.requestBody();

        if (requestBody != null) {
            final var requestBodyType = Utils.requireNonNull(ApiCodeGeneratorUtils.findJsonMediaType(
                    requestBody.contentMediaType()
            ));

            final var idProperty = Utils.requireNonNull(requestBodyType.properties().get("id"));
            final var idType = idProperty.type();

            final var idVar = new VariableDeclarationExpr(
                    types.createType(idType, idProperty.nullable()),
                    "id"
            );

            idVar.getVariable(0)
                    .setInitializer(delegateCall);

            body.addStatement(idVar);

            final var returnStmt = new ReturnStmt(
                    new MethodCallExpr(
                            new MethodCallExpr(
                                    new NameExpr("ResponseEntity"),
                                    "unprocessableEntity"
                            ),
                            "build"
                    )
            );

            if (idProperty.nullable()) {
                final var ifStmt = new IfStmt()
                        .setCondition(
                                new BinaryExpr(
                                        new NameExpr("id"),
                                        new NullLiteralExpr(),
                                        BinaryExpr.Operator.EQUALS
                                )
                        ).setThenStmt(new BlockStmt(NodeList.nodeList(returnStmt)));
                body.addStatement(ifStmt);
            } else {
                if (idType instanceof OpenApiStandardType st) {
                    if ("integer".equals(st.type())) {
                        if ("int64".equals(st.format())) {
                            final var ifStmt = new IfStmt()
                                    .setCondition(
                                            new BinaryExpr(
                                                    new NameExpr("id"),
                                                    new LongLiteralExpr(),
                                                    BinaryExpr.Operator.EQUALS
                                            )
                                    ).setThenStmt(new BlockStmt(NodeList.nodeList(returnStmt)));
                            body.addStatement(ifStmt);
                        } else {
                            final var ifStmt = new IfStmt()
                                    .setCondition(
                                            new BinaryExpr(
                                                    new NameExpr("id"),
                                                    new IntegerLiteralExpr(),
                                                    BinaryExpr.Operator.EQUALS
                                            )
                                    ).setThenStmt(new BlockStmt(NodeList.nodeList(returnStmt)));
                            body.addStatement(ifStmt);
                        }
                    }
                }
            }

            body.addStatement(
                    new ReturnStmt(
                            new MethodCallExpr(
                                    new MethodCallExpr(
                                            new NameExpr("ResponseEntity"),
                                            "created",
                                            NodeList.nodeList(
                                                    new MethodCallExpr(
                                                            new NameExpr("ApiUtils"),
                                                            "createLocation",
                                                            NodeList.nodeList(
                                                                    new NameExpr("request"),
                                                                    new NameExpr("id")
                                                            )
                                                    )
                                            )
                                    ),
                                    "build"
                            )
                    )
            );
        } else {
            body.addStatement(new ExpressionStmt(delegateCall));
            body.addStatement(
                new ReturnStmt(new MethodCallExpr(
                        new MethodCallExpr(
                                new NameExpr("ResponseEntity"),
                                "status",
                                NodeList.nodeList(
                                        new FieldAccessExpr(
                                                new NameExpr("HttpStatus"),
                                                "CREATED"
                                        )
                                )
                        ),
                        "build"
                ))
            );
        }

        methodDeclaration.setBody(body);
    }

    private void fillBodyForPost(final MethodCallExpr delegateCall,
                                 final MethodDeclaration methodDeclaration) {
        final var body = new BlockStmt();

        // return ResponseEntity.ok().build()

        body.addStatement(delegateCall);
        body.addStatement(new ReturnStmt(
                new MethodCallExpr(
                        new MethodCallExpr(
                                new NameExpr("ResponseEntity"),
                                "ok"
                        ),
                        "build"
                )
        ));
        methodDeclaration.setBody(body);
    }

    @Override
    protected Parameter createParameter(final OpenApiParameter openApiParameter) {
        final var parameter = super.createParameter(openApiParameter);

        /*
        final var annotation = requireNonNull(switch (openApiParameter.in()) {
            case QUERY -> {
                final var members = new HashMap<String, Object>();
                members.put("name", openApiParameter.name());

                if (openApiParameter.required()) {
                    members.put("required", true);
                }

                final var defaultValue = openApiParameter.defaultValue();

                if (defaultValue != null) {
                    members.put("defaultValue", defaultValue);
                }

                yield GenerateHelper.createAnnotation("org.springframework.web.bind.annotation.RequestParam", members);
            }
            case PATH -> {
                final var members = new HashMap<String, Object>();
                members.put("name", openApiParameter.name());

                if (!openApiParameter.required()) {
                    members.put("required", false);
                }

                yield GenerateHelper.createAnnotation("org.springframework.web.bind.annotation.PathVariable", members);
            }
        });

        return parameter.addAnnotation(annotation);
         */
        return parameter;
    }

    private void generateApiUtils(final OpenApiPath openApiPath) {
        final var pathUri = this.pathsDir.toURI().toString();
        final var creatingReference = openApiPath.creatingReference();
        final var ref = creatingReference.substring(pathUri.length());
        final var packageNameAndName = Utils.resolvePackageNameAndName(ref);
        var packageName = packageNameAndName.first();

        //Try to make the package name end with .api
        if (!packageName.endsWith(".api")) {
            final var index = packageName.lastIndexOf(".api.");
            if (index > 0) {
                packageName = packageName.substring(0, index + 4);
            }
        }

        final var cu = new CompilationUnit();
        cu.setPackageDeclaration(packageName);
        cu.addImport("java.net.URI");

        final var clazz = cu.addClass("ApiUtils", Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL);
        clazz.addConstructor(Modifier.Keyword.PRIVATE);

        final var createLocationMethod = clazz.addMethod("createLocation", Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC);
        createLocationMethod.setType(types.createType("java.net.URI"));
        createLocationMethod.addParameter(
                new Parameter(
                        types.createType("javax.servlet.http.HttpServletRequest"),
                        "request"
                ).addModifier(Modifier.Keyword.FINAL)
        );
        createLocationMethod.addParameter(
                new Parameter(
                        types.createType("java.lang.Object"),
                        "id"
                ).addModifier(Modifier.Keyword.FINAL)
        );

        // final StringBuffer location = request.getRequestURL();
        final var body = new BlockStmt();
        body.addStatement(
                new VariableDeclarationExpr(
                        new VariableDeclarator(
                                types.createType("java.lang.StringBuffer"),
                                new SimpleName("locationBuffer"),
                                new MethodCallExpr(
                                        new NameExpr("request"),
                                        "getRequestURL"
                                )
                        ),
                        Modifier.finalModifier()
                )
        );

        /* if (location.charAt(location.length() - 1) != '/') {
              location.append('/');
           }*/
        body.addStatement(
                new IfStmt().setCondition(
                        new BinaryExpr(
                                new MethodCallExpr(
                                        new NameExpr("locationBuffer"),
                                        "charAt",
                                        NodeList.nodeList(
                                                new BinaryExpr(
                                                        new MethodCallExpr(new NameExpr("locationBuffer"), "length"),
                                                        new IntegerLiteralExpr("-1"),
                                                        BinaryExpr.Operator.MINUS
                                                )
                                        )
                                ),
                                new CharLiteralExpr('/'),
                                BinaryExpr.Operator.NOT_EQUALS
                        )
                ).setThenStmt(
                        new BlockStmt(
                                NodeList.nodeList(
                                        new ExpressionStmt(new MethodCallExpr(
                                                new NameExpr("locationBuffer"),
                                                "append",
                                                NodeList.nodeList(new CharLiteralExpr('/'))
                                            )
                                        )
                                )
                        )
                )
        );

        // return Uri.create(location.append(id).toString())
        body.addStatement(
            new ReturnStmt(
                    new MethodCallExpr(
                            new NameExpr("URI"),
                            "create",
                            NodeList.nodeList(
                                    new MethodCallExpr(
                                            new MethodCallExpr(
                                                    new NameExpr("locationBuffer"),
                                                    "append",
                                                    NodeList.nodeList(new NameExpr("id"))
                                            ),
                                            "toString"
                                    )
                            )
                    )
            )
        );

        createLocationMethod.setBody(body);

        try {
            filer.write(cu);
        } catch (final IOException e) {
            LOGGER.log(LogLevel.SEVERE, "Fail to generate code for ApiUtils", e);
        }
    }

    @Override
    protected boolean shouldAddAnnotationsOnParameters() {
        return false;
    }
}
