package com.github.potjerodekool.openapi.generate.api;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.Type;
import com.github.potjerodekool.openapi.*;
import com.github.potjerodekool.openapi.generate.GenerateHelper;
import com.github.potjerodekool.openapi.generate.JavaTypes;
import com.github.potjerodekool.openapi.generate.Types;
import com.github.potjerodekool.openapi.tree.*;
import com.github.potjerodekool.openapi.util.GenerateException;
import com.github.potjerodekool.openapi.util.Utils;
import org.checkerframework.checker.nullness.qual.Nullable;

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
        if (!creatingReference.startsWith(pathUri)) {
            throw new GenerateException(String.format("Creating reference %s doesn't start with %s", creatingReference, pathUri));
        }
        final var ref = creatingReference.substring(pathUri.length());
        final var packageNameAndName = Utils.resolvePackageNameAndName(ref);
        final var packageName = packageNameAndName.first();
        final var name = packageNameAndName.second();
        final var apiName = Utils.firstUpper(name) + "Api";

        final var implName = Utils.firstUpper(name) + "Impl";
        final var qualifiedImplName = packageName + "." + implName;

        final var cu = this.compilationUnitMap.computeIfAbsent(qualifiedImplName, (key) -> {
            final var newCU = new CompilationUnit();

            if (!Utils.isNullOrEmpty(packageName)) {
                newCU.setPackageDeclaration(packageName);
            }

            final var implClass = newCU.addClass(implName);

            try {

                implClass.addImplementedType(apiName);
                implClass.addMarkerAnnotation("org.springframework.web.bind.annotation.RestController");
                implClass.addMarkerAnnotation("org.springframework.web.bind.annotation.CrossOrigin");
            } catch (Exception e) {
                throw e;
            }

            return newCU;
        });

        final var clazz = (ClassOrInterfaceDeclaration) cu.getTypes().get(0);

        processOperation(HttpMethod.POST, openApiPath.path(), openApiPath.post(), clazz);
        processOperation(HttpMethod.GET, openApiPath.path(), openApiPath.get(), clazz);
        processOperation(HttpMethod.PUT, openApiPath.path(), openApiPath.put(), clazz);
        processOperation(HttpMethod.PATCH, openApiPath.path(), openApiPath.patch(), clazz);
        processOperation(HttpMethod.DELETE, openApiPath.path(), openApiPath.delete(), clazz);
    }

    private void processOperation(final HttpMethod httpMethod,
                                  final String path,
                                  final @Nullable OpenApiOperation operation,
                                  final ClassOrInterfaceDeclaration clazz) {
        if (operation == null) {
            return;
        }

        final var operationId = operation.operationId();
        final var method = clazz.addMethod(operationId, Modifier.Keyword.PUBLIC);
        createParameters(operation).stream()
                .map(parameter -> parameter.addModifier(Modifier.Keyword.FINAL))
                .forEach(method::addParameter);

        final var returnType = types.createType("org.springframework.http.ResponseEntity");
        final var okResponseOptional = find2XXResponse(operation.responses());

        final var responseType =  okResponseOptional
                .map(this::getResponseType)
                .orElseGet(() -> types.createType("java.lang.Void"));

        returnType.setTypeArguments(responseType);
        method.setType(returnType);
        method.addMarkerAnnotation("java.lang.Override");

        final var body = new BlockStmt();
        body.addStatement(new ReturnStmt(
                new MethodCallExpr(
                        new MethodCallExpr(
                                new NameExpr("org.springframework.http.ResponseEntity"),
                                "status",
                                new NodeList<>(
                                        new FieldAccessExpr(
                                                new NameExpr("org.springframework.http.HttpStatus"),
                                                "NOT_IMPLEMENTED"
                                        )
                                )
                        ),
                        "build"
                )
        ));
        method.setBody(body);
    }

    @Override
    protected Parameter createParameter(final OpenApiParameter openApiParameter) {
        final var parameter = super.createParameter(openApiParameter);

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

                if (openApiParameter.required()) {
                    members.put("required", true);
                }

                yield GenerateHelper.createAnnotation("org.springframework.web.bind.annotation.PathVariable", members);
            }
        });

        return parameter.addAnnotation(annotation);
    }
}
