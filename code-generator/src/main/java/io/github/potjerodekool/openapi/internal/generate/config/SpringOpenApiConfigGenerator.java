package io.github.potjerodekool.openapi.internal.generate.config;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import io.github.potjerodekool.openapi.OpenApiGeneratorConfig;
import io.github.potjerodekool.openapi.internal.Filer;
import io.github.potjerodekool.openapi.internal.generate.Types;
import io.github.potjerodekool.openapi.tree.*;
import io.github.potjerodekool.openapi.internal.util.NodeListCollectors;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Arrays;
import java.util.Map;

/**
 Generates a configuration class with an OpenApiConfiguration bean.
 */
public class SpringOpenApiConfigGenerator extends AbstractSpringConfigGenerator {

    public SpringOpenApiConfigGenerator(final OpenApiGeneratorConfig config,
                                        final Types types,
                                        final Filer filer) {
        super(config, types, filer);
    }

    @Override
    protected String getConfigClassName() {
        return "OpenApiConfiguration";
    }

    @Override
    protected void fillClass(final OpenApi api,
                             final ClassOrInterfaceDeclaration clazz) {
        final var method = clazz.addMethod("api", Modifier.Keyword.PUBLIC);
        method.addAnnotation(new MarkerAnnotationExpr("org.springframework.context.annotation.Bean"));

        final var openApiType = getTypes().createType("io.swagger.v3.oas.models.OpenAPI");

        Expression openApiInstance = new ObjectCreationExpr()
                .setType(openApiType);
        method.setType(openApiType);

        openApiInstance = new MethodCallExpr(openApiInstance, "info", NodeList.nodeList(createInfo(api)));

        final var components = createComponents(api);
        if (components != null) {
            openApiInstance = new MethodCallExpr(openApiInstance, "components", NodeList.nodeList(components));
        }

        final var securityRequirements = api.securityRequirements().stream()
                .map(this::createSecurityRequirement)
                .toList();

        if (securityRequirements.size() > 0) {
            for (final var securityRequirement : securityRequirements) {
                openApiInstance = call(openApiInstance, "addSecurityItem", securityRequirement);
            }
        }

        method.setBody(new BlockStmt(NodeList.nodeList(new ReturnStmt(openApiInstance))));
    }

    private Expression createInfo(final OpenApi api) {
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

        final var contact = apiInfo.contact();

        if (contact != null) {
            lastExpression = call(lastExpression, "contact", createContact(contact));
        }

        return lastExpression;
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

    private Expression createContact(final OpenApiContact contact) {
        final var name = contact.name();
        final var url = contact.url();
        final var email = contact.email();
        final var extensions = contact.extensions();

        Expression lastExpression = new ObjectCreationExpr().setType("io.swagger.v3.oas.models.info.Contact");

        if (name != null) {
            lastExpression = new MethodCallExpr(
                    lastExpression,
                    "name",
                    NodeList.nodeList(new StringLiteralExpr(name))
            );
        }

        if (url != null) {
            lastExpression = new MethodCallExpr(
                    lastExpression,
                    "url",
                    NodeList.nodeList(new StringLiteralExpr(url))
            );
        }

        if (email != null) {
            lastExpression = new MethodCallExpr(
                    lastExpression,
                    "email",
                    NodeList.nodeList(new StringLiteralExpr(email))
            );
        }

        if (extensions != null) {
            final NodeList<Expression> entries = extensions.entrySet().stream()
                    .map(entry -> new MethodCallExpr(
                            new NameExpr("java.util.Map"),
                            "entry",
                            NodeList.nodeList(
                                    new StringLiteralExpr(entry.getKey()),
                                    new StringLiteralExpr((String) entry.getValue())
                            )
                    ))
                    .collect(NodeListCollectors.collector());

            lastExpression = new MethodCallExpr(
                    lastExpression,
                    "extensions",
                    NodeList.nodeList(
                            new MethodCallExpr(
                                    new NameExpr("java.util.Map"),
                                    "ofEntries",
                                    entries
                            )
                    )
            );
        }

        return lastExpression;
    }

    private @Nullable Expression createComponents(final OpenApi openApi) {
        var created = false;

        Expression lastExpression = new ObjectCreationExpr().setType("io.swagger.v3.oas.models.Components");

        final var securitySchemas = openApi.securitySchemas();

        if (securitySchemas.size() > 0) {
            created = true;
            for (final var entry : securitySchemas.entrySet()) {
                lastExpression = call(lastExpression, "addSecuritySchemes",
                        new StringLiteralExpr(entry.getKey()),
                        createSecurityScheme(entry.getValue())
                );
            }
        }

        return created ? lastExpression : null;
    }

    private Expression createSecurityScheme(final OpenApiSecurityScheme openApiSecurityScheme) {
        final var type = openApiSecurityScheme.type().name();
        final var bearerFormat = openApiSecurityScheme.bearerFormat();
        final var description = openApiSecurityScheme.description();
        final var in = openApiSecurityScheme.in();

        Expression lastExpression = new ObjectCreationExpr().setType("io.swagger.v3.oas.models.security.SecurityScheme");
        lastExpression = call(lastExpression, "name", new StringLiteralExpr(openApiSecurityScheme.name()));
        lastExpression = call(lastExpression, "type", new FieldAccessExpr(
                new NameExpr("io.swagger.v3.oas.models.security.SecurityScheme.Type"),
                type
        ));
        lastExpression = call(lastExpression, "scheme", new StringLiteralExpr(openApiSecurityScheme.schema()));

        if (bearerFormat != null) {
            lastExpression = call(lastExpression, "bearerFormat", new StringLiteralExpr(bearerFormat));
        }

        if (description != null) {
            lastExpression = call(lastExpression, "description", new StringLiteralExpr(description));
        }

        if (in != null) {
            lastExpression = call(lastExpression, "in", new FieldAccessExpr(
                    new NameExpr("io.swagger.v3.oas.models.security.SecurityScheme.In"),
                    in.name()
            ));
        }

        return lastExpression;
    }

    private Expression createSecurityRequirement(final OpenApiSecurityRequirement securityRequirement) {
        final var requirements = securityRequirement.requirements();
        final String name = requirements.keySet().iterator().next();
        final var securityParameter = requirements.get(name);

        Expression lastExpression = new ObjectCreationExpr()
                .setType("io.swagger.v3.oas.models.security.SecurityRequirement");

        final NodeList<Expression> parameters = securityParameter.parameters().stream()
                .map(StringLiteralExpr::new)
                .collect(NodeListCollectors.collector());

        final var arguments = new NodeList<Expression>();
        arguments.add(new StringLiteralExpr(name));

        if (parameters.size() > 0) {
            arguments.add(
                new MethodCallExpr(
                        new NameExpr("java.util.List"),
                        "of",
                        parameters
                )
            );
        }

        return new MethodCallExpr(lastExpression, "addList", arguments);
    }

    private MethodCallExpr call(final Expression target,
                                final String name,
                                final Object... arguments) {
        final var argumentList =
                Arrays.stream(arguments)
                        .map(this::createExpression)
                        .collect(NodeListCollectors.collector());

        return new MethodCallExpr(target, name, argumentList);
    }



    private Expression createExpression(final Object value) {
        if (value == null) {
            return new NullLiteralExpr();
        } else if (value instanceof Expression e) {
            return e;
        } else if (value instanceof String s) {
            return new StringLiteralExpr(s);
        } else if (value instanceof Map) {
            @SuppressWarnings("unchecked")
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