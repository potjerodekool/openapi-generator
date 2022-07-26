package com.github.potjerodekool.openapi.generate.api;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.potjerodekool.openapi.*;
import com.github.potjerodekool.openapi.generate.GenerateHelper;
import com.github.potjerodekool.openapi.tree.OpenApi;
import com.github.potjerodekool.openapi.tree.OpenApiOperation;
import com.github.potjerodekool.openapi.tree.OpenApiPath;
import com.github.potjerodekool.openapi.util.Utils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
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

    private void processOperation(final HttpMethod httpMethod,
                                  final String path,
                                  final @Nullable OpenApiOperation operation,
                                  final ClassOrInterfaceDeclaration clazz) {
        if (operation == null) {
            return;
        }

        final var operationId = operation.operationId();

        final var method = clazz.addMethod(operationId, Modifier.Keyword.DEFAULT);
        createParameters(
                operation
        ).stream()
                .map(parameter -> parameter.addModifier(Modifier.Keyword.FINAL))
                .forEach(method::addParameter);

        final var requestBody = operation.requestBody();
/*
        if (httpMethod == HttpMethod.POST
                && operation.responses().containsKey("201")
                && requestBody != null) {
            final var mediaType = ApiCodeGeneratorUtils.findJsonMediaType(
                    requestBody.contentMediaType());

            if (mediaType != null) {
                final var idProperty = mediaType.properties().get("id");

                if (idProperty != null) {
                    final var idType = types.createType(idProperty.type(), idProperty.nullable());
                    method.setType(idType);

                    final var body = new BlockStmt();
                    body.addStatement(new ReturnStmt(GenerateHelper.getDefaultValue(idType)));
                    method.setBody(body);
                }
            }
        } else {
            */
            final var okResponseOptional = ApiCodeGeneratorUtils.find2XXResponse(operation.responses());

            if (okResponseOptional.isPresent()) {
                final var okResponse = okResponseOptional.get();
                final var mediaType = ApiCodeGeneratorUtils.findJsonMediaType(okResponse.contentMediaType());

                if (mediaType != null) {
                    final var type = types.createType(mediaType, false);
                    method.setType(type);

                    final var body = new BlockStmt();
                    body.addStatement(new ThrowStmt(new ObjectCreationExpr()
                            .setType(types.createType("java.lang.UnsupportedOperationException"))));
                    method.setBody(body);
                }
            }
        //}
    }
}
