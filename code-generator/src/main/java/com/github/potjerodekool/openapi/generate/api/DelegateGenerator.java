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
import com.github.potjerodekool.openapi.tree.OpenApi;
import com.github.potjerodekool.openapi.tree.OpenApiOperation;
import com.github.potjerodekool.openapi.tree.OpenApiPath;
import com.github.potjerodekool.openapi.type.OpenApiType;
import com.github.potjerodekool.openapi.util.Utils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.potjerodekool.openapi.util.Utils.requireNonNull;

public class DelegateGenerator extends AbstractSpringGenerator {

    private static final Logger LOGGER = Logger.getLogger(DelegateGenerator.class.getName());
    private final Filer filer;
    private final File pathsDir;
    private final Map<String, CompilationUnit> compilationUnitMap = new HashMap<>();

    public DelegateGenerator(final OpenApiGeneratorConfig config,
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
        final var delegateName = Utils.firstUpper(name) + "Delegate";
        final var qualifiedApiName = packageName + "." + delegateName;

        final var cu = this.compilationUnitMap.computeIfAbsent(qualifiedApiName, (key) -> {
            final var newCU = new CompilationUnit();

            if (!Utils.isNullOrEmpty(packageName)) {
                newCU.setPackageDeclaration(packageName);
            }
            newCU.addInterface(delegateName);
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
        method.addModifier(Modifier.Keyword.DEFAULT);
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

    @Override
    protected List<OpenApiType> resolveResponseTypes(final HttpMethod httpMethod,
                                                     final OpenApiOperation operation) {
        if (httpMethod == HttpMethod.POST) {
            final var requestBody = operation.requestBody();

            if (requestBody != null) {
                final var contentMediaType= requestBody.contentMediaType().get("application/json");

                if (contentMediaType != null) {
                    final var idProperty = contentMediaType.properties().get("id");

                    if (idProperty != null) {
                        return List.of(idProperty.type());
                    }
                }
            }
        }

        return super.resolveResponseTypes(httpMethod, operation);
    }
}
