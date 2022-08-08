package io.github.potjerodekool.openapi.generate.api;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import io.github.potjerodekool.openapi.Filer;
import io.github.potjerodekool.openapi.LogLevel;
import io.github.potjerodekool.openapi.Logger;
import io.github.potjerodekool.openapi.OpenApiGeneratorConfig;
import io.github.potjerodekool.openapi.generate.Types;
import io.github.potjerodekool.openapi.tree.OpenApi;
import io.github.potjerodekool.openapi.util.Utils;

import java.io.File;
import java.io.IOException;

public class UtilsGenerator {

    private static final Logger LOGGER = Logger.getLogger(UtilsGenerator.class.getName());

    private final Types types;

    private final Filer filer;
    private final File pathsDir;

    public UtilsGenerator(final OpenApiGeneratorConfig config,
                          final Types types,
                          final Filer filer) {
        this.types = types;
        this.filer = filer;
        pathsDir = config.getPathsDir();
    }

    public void generate(final OpenApi api) {
        final var openApiPath = api.paths().get(0);
        final var pathUri = Utils.toUriString(this.pathsDir);
        final var creatingReference = openApiPath.creatingReference();
        final var ref = creatingReference.substring(pathUri.length());
        final var qualifiedName = Utils.resolveQualified(ref);
        var packageName = qualifiedName.packageName();

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

        body.addStatement(
                new IfStmt().setCondition(
                        new BinaryExpr(
                                new MethodCallExpr(
                                        new NameExpr("locationBuffer"),
                                        "charAt",
                                        NodeList.nodeList(
                                                new BinaryExpr(
                                                        new MethodCallExpr(new NameExpr("locationBuffer"), "length"),
                                                        new IntegerLiteralExpr("1"),
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
}
