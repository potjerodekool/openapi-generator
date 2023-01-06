package io.github.potjerodekool.openapi.internal.generate.api;

import io.github.potjerodekool.openapi.Features;
import io.github.potjerodekool.openapi.GeneratorConfig;
import io.github.potjerodekool.openapi.Language;
import io.github.potjerodekool.openapi.internal.ClassNames;
import io.github.potjerodekool.openapi.internal.Filer;
import io.github.potjerodekool.openapi.internal.ast.CompilationUnit;
import io.github.potjerodekool.openapi.internal.ast.Modifier;
import io.github.potjerodekool.openapi.internal.ast.Operator;
import io.github.potjerodekool.openapi.internal.ast.util.TypeUtils;
import io.github.potjerodekool.openapi.internal.ast.element.PackageElement;
import io.github.potjerodekool.openapi.internal.ast.element.VariableElement;
import io.github.potjerodekool.openapi.internal.ast.expression.*;
import io.github.potjerodekool.openapi.internal.ast.statement.BlockStatement;
import io.github.potjerodekool.openapi.internal.ast.statement.ExpressionStatement;
import io.github.potjerodekool.openapi.internal.ast.statement.IfStatement;
import io.github.potjerodekool.openapi.internal.ast.statement.ReturnStatement;
import io.github.potjerodekool.openapi.log.LogLevel;
import io.github.potjerodekool.openapi.log.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class UtilsGenerator {

    private static final Logger LOGGER = Logger.getLogger(UtilsGenerator.class.getName());

    private final Filer filer;
    private final TypeUtils typeUtils;

    private final Language language;
    private final String basePackageName;

    private final String httpServletClassName;

    public UtilsGenerator(final GeneratorConfig generatorConfig,
                          final Filer filer,
                          final TypeUtils typeUtils) {
        this.filer = filer;
        this.typeUtils = typeUtils;
        this.language = generatorConfig.language();
        this.basePackageName = generatorConfig.basePackageName();

        this.httpServletClassName = generatorConfig.isFeatureEnabled(Features.FEATURE_JAKARTA)
                ? ClassNames.JAKARTA_HTTP_SERVLET_REQUEST
                : ClassNames.JAVA_HTTP_SERVLET_REQUEST;
    }

    public void generate() {
        var packageName = this.basePackageName;

        //Try to make the package name end with .api
        if (!packageName.endsWith(".api")) {
            final var index = packageName.lastIndexOf(".api.");
            if (index > 0) {
                packageName = packageName.substring(0, index + 4);
            }
        }

        final var cu = new CompilationUnit(Language.JAVA);
        cu.setPackageElement(PackageElement.create(packageName));

        final var clazz = cu.addClass("ApiUtils", Modifier.PUBLIC, Modifier.FINAL);
        final var constructor = clazz.addConstructor(Modifier.PRIVATE);
        constructor.setBody(new BlockStatement());

        final var createLocationMethod = clazz.addMethod("createLocation", Modifier.PUBLIC, Modifier.STATIC);
        createLocationMethod.setReturnType(typeUtils.createDeclaredType("java.net.URI"));

        VariableElement.createParameter(
                "request",
                typeUtils.createDeclaredType(httpServletClassName)
        ).addModifier(Modifier.FINAL);

        createLocationMethod.addParameter(
                VariableElement.createParameter(
                        "request",
                        typeUtils.createDeclaredType(httpServletClassName)
                ).addModifier(Modifier.FINAL)
        );
        createLocationMethod.addParameter(
                VariableElement.createParameter(
                        "id",
                        typeUtils.createDeclaredType("java.lang.Object")
                ).addModifier(Modifier.FINAL)
        );

        // final StringBuffer location = request.getRequestURL();
        final var body = new BlockStatement();
        body.add(
                new VariableDeclarationExpression(
                        Set.of(Modifier.FINAL),
                        typeUtils.createDeclaredType("java.lang.StringBuffer"),
                        "locationBuffer",
                        new MethodCallExpression(
                                new NameExpression("request"),
                                "getRequestURL"
                        )
                )
        );

        body.add(
                new IfStatement(
                        new BinaryExpression(
                                new MethodCallExpression(
                                        new NameExpression("locationBuffer"),
                                        "charAt",
                                        List.of(
                                                new BinaryExpression(
                                                        new MethodCallExpression(
                                                                new NameExpression("locationBuffer"),
                                                                "length"
                                                        ),
                                                        LiteralExpression.createIntLiteralExpression("1"),
                                                        Operator.MINUS
                                                )
                                        )
                                ),
                                LiteralExpression.createCharLiteralExpression('/'),
                                Operator.NOT_EQUALS
                        ),
                        new BlockStatement(
                                new ExpressionStatement(
                                        new MethodCallExpression(
                                                new NameExpression("locationBuffer"),
                                                "append",
                                                List.of(LiteralExpression.createCharLiteralExpression('/'))
                                        )
                                )
                        )
                )
        );

        // return Uri.create(location.append(id).toString())
        body.add(
                new ReturnStatement(
                        new MethodCallExpression(
                                new NameExpression("java.net.URI"),
                                "create",
                                List.of(
                                        new MethodCallExpression(
                                                new MethodCallExpression(
                                                        new NameExpression("locationBuffer"),
                                                        "append",
                                                        List.of(new NameExpression("id"))
                                                ),
                                                "toString"
                                        )
                                )
                        )
                )
        );

        createLocationMethod.setBody(body);

        try {
            filer.writeSource(cu, language);
        } catch (final IOException e) {
            LOGGER.log(LogLevel.SEVERE, "Fail to generate code for ApiUtils", e);
        }
    }
}
