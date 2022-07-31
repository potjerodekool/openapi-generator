package com.github.potjerodekool.openapi.generate;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.potjerodekool.openapi.Filer;
import com.github.potjerodekool.openapi.LogLevel;
import com.github.potjerodekool.openapi.Logger;
import com.github.potjerodekool.openapi.OpenApiGeneratorConfig;
import com.github.potjerodekool.openapi.tree.OpenApi;
import com.github.potjerodekool.openapi.tree.OpenApiInfo;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

public class ConfigGenerator implements CodeGenerator {

    private static final Logger LOGGER = Logger.getLogger(CodeGenerator.class.getName());
    private final Types types = new JavaTypes();
    private final OpenApiGeneratorConfig config;
    private final Filer filer;

    public ConfigGenerator(final OpenApiGeneratorConfig config,
                           final Filer filer) {
        this.config = config;
        this.filer = filer;
    }

    @Override
    public void generate(final OpenApi api) {
        final var cu = new CompilationUnit();

        final var configPackageName = config.getConfigPackageName();

        if (configPackageName != null) {
            cu.setPackageDeclaration(configPackageName);
        }

        final var clazz = cu.addClass("OpenApiConfiguration");
        clazz.addMarkerAnnotation("org.springframework.context.annotation.Configuration");

        final var method = clazz.addMethod("api", Modifier.Keyword.PUBLIC);
        method.addAnnotation(new MarkerAnnotationExpr("org.springframework.context.annotation.Bean"));

        final var openApiType = types.createType("io.swagger.v3.oas.models.OpenAPI");

        final var openApiInstance = new ObjectCreationExpr()
                .setType(openApiType);

        method.setType(openApiType);

        final var apiInfo = api.info();

        Expression lastExpression = new ObjectCreationExpr()
                .setType("io.swagger.v3.oas.models.info.Info");

        final var title = apiInfo.title();

        if (title != null) {
            lastExpression = call(lastExpression, "title", title);
        }

        final var description = apiInfo.description();

        if (description != null) {
            lastExpression = call(lastExpression, "description", description);
        }

        final var termsOfService = apiInfo.termsOfService();

        if (termsOfService != null) {
            lastExpression = call(lastExpression, "termsOfService", termsOfService);
        }

        final var version = apiInfo.version();

        if (version != null) {
            lastExpression = call(lastExpression, "version", version);
        }

        final var extensions = apiInfo.extensions();

        if (extensions != null && !extensions.isEmpty()) {
            lastExpression = call(lastExpression, "extensions", extensions);
        }

        if (hasLicenceInfo(apiInfo)) {
            final var licenseExpression = createLicense(apiInfo);
            lastExpression = call(lastExpression, "license", licenseExpression);
        }

        var lastCall = info(openApiInstance, lastExpression);

        method.setBody(new BlockStmt(NodeList.nodeList(new ReturnStmt(lastCall))));

        try {
            filer.write(cu);
        } catch (final IOException e) {
            LOGGER.log(LogLevel.SEVERE, "Fail to generate code for spring api definition", e);
        }
    }

    private Expression info(final ObjectCreationExpr target,
                            final Expression argument) {
        return new MethodCallExpr(target, "info", NodeList.nodeList(argument));
    }

    private MethodCallExpr call(final Expression target,
                                final String name,
                                final Object... arguments) {
        final var argumentList = new NodeList<>(
                Arrays.stream(arguments)
                        .map(this::createExpression)
                        .toList()
        );

        return new MethodCallExpr(target, name, argumentList);
    }

    private boolean hasLicenceInfo(final OpenApiInfo apiInfo) {
        final var licence = apiInfo.license();

        final var name = licence.name();
        final var url = licence.url();
        final var extensions = licence.extensions();

        return name != null || url != null || (extensions != null &&
                !extensions.isEmpty());
    }

    private Expression createLicense(final OpenApiInfo apiInfo) {
        final var licence = apiInfo.license();

        final var name = licence.name();
        final var url = licence.url();
        final var extensions = licence.extensions();

        Expression lastExpression = new ObjectCreationExpr()
                .setType("io.swagger.v3.oas.models.info.License");

        if (name != null) {
            lastExpression = call(lastExpression, "name", name);
        }

        if (url != null) {
            lastExpression = call(lastExpression, "url", url);
        }

        if (extensions != null && !extensions.isEmpty()) {
            lastExpression = call(lastExpression, "extensions", extensions);
        }

        return lastExpression;
    }

    private Expression createExpression(final Object value) {
        if (value == null) {
            return new NullLiteralExpr();
        } else if (value instanceof Expression e) {
            return e;
        } else if (value instanceof String s) {
            return new StringLiteralExpr(s);
        } else if (value instanceof Map) {
            final Map<String, Object> map = (Map<String, Object>) value;
            final var entries = new NodeList<Expression>();

            map.forEach((key, value1) -> entries.add(new MethodCallExpr(
                    new NameExpr("java.util.Map"),
                    "entry",
                    NodeList.nodeList(
                            createExpression(key),
                            createExpression(value1)
                    )
            )));

            return new MethodCallExpr(
                    new NameExpr("java.util.Map"),
                    "ofEntries",
                    NodeList.nodeList(entries)
            );
        } else {
            throw new IllegalArgumentException("" + value.getClass());
        }
    }
}
