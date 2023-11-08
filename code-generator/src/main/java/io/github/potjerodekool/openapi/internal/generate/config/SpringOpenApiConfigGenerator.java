package io.github.potjerodekool.openapi.internal.generate.config;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.model.element.Modifier;
import io.github.potjerodekool.codegen.model.tree.expression.*;
import io.github.potjerodekool.codegen.model.tree.statement.BlockStatement;
import io.github.potjerodekool.codegen.model.tree.statement.ReturnStatement;
import io.github.potjerodekool.codegen.model.tree.statement.java.JClassDeclaration;
import io.github.potjerodekool.codegen.model.tree.type.ClassOrInterfaceTypeExpression;
import io.github.potjerodekool.openapi.GeneratorConfig;
import io.github.potjerodekool.openapi.internal.ClassNames;
import io.github.potjerodekool.openapi.tree.*;
import io.github.potjerodekool.openapi.tree.media.OpenApiObjectSchema;
import io.github.potjerodekool.openapi.tree.media.OpenApiSchema;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

import static io.github.potjerodekool.codegen.model.tree.expression.MethodCallExpressionBuilder.invoke;

/**
 Generates a configuration class with an OpenApiConfiguration bean.
 */
public class SpringOpenApiConfigGenerator extends AbstractSpringApiConfigGenerator {

    public SpringOpenApiConfigGenerator(final GeneratorConfig generatorConfig,
                                        final Environment environment) {
        super(generatorConfig, environment);
    }

    @Override
    protected String getConfigClassName() {
        return "OpenApiConfiguration";
    }

    @Override
    protected void fillClass(final OpenApi api,
                             final JClassDeclaration classDeclaration) {
        final var method = classDeclaration.addMethod(new ClassOrInterfaceTypeExpression("io.swagger.v3.oas.models.OpenAPI"), "api", Set.of(Modifier.PUBLIC));
        method.annotation("org.springframework.context.annotation.Bean");

        Expression openApiInstance = new NewClassExpression(new ClassOrInterfaceTypeExpression(
                "io.swagger.v3.oas.models.OpenAPI"
        ));

        openApiInstance = createSpecVersion(api, openApiInstance);
        openApiInstance = createServers(api, openApiInstance);
        openApiInstance = invoke(openApiInstance, "info", List.of(createInfo(api))).build();

        final var securityRequirements = api.securityRequirements().stream()
                .map(this::createSecurityRequirement)
                .toList();

        if (!securityRequirements.isEmpty()) {
            for (final var securityRequirement : securityRequirements) {
                openApiInstance = call(openApiInstance, "addSecurityItem", securityRequirement);
            }
        }

        method.setBody(new BlockStatement(new ReturnStatement(openApiInstance)));
    }

    private Expression createSpecVersion(final OpenApi api,
                                         final Expression openApiInstance) {
        final String versionFieldName;

        if ("3.0.0".equals(api.version())) {
            versionFieldName = "V30";
        } else if ("3.1.0".equals(api.version())) {
            versionFieldName = "V31";
        } else {
            return openApiInstance;
        }

        return invoke(
                openApiInstance,
                "specVersion",
                new FieldAccessExpression(
                        new ClassOrInterfaceTypeExpression("io.swagger.v3.oas.models.SpecVersion"),
                        versionFieldName
                )
        ).build();
    }

    private Expression createServers(final OpenApi api,
                                     final Expression openApiInstance) {
        final var serverArgs = api.servers().stream()
                .map(this::createServer)
                .toList();

        return new MethodCallExpression(
                openApiInstance,
                "servers",
                new MethodCallExpression(
                        new ClassOrInterfaceTypeExpression(ClassNames.LIST_CLASS_NAME),
                        "of",
                        serverArgs.toArray(new Expression[0])
                )
        );
    }

    private Expression createServer(final OpenApiServer server) {
        final var url = server.url();
        final var description = server.description();

        var expression = new MethodCallExpression(
                new NewClassExpression(
                        new ClassOrInterfaceTypeExpression("io.swagger.v3.oas.models.servers.Server")
                ),
                "url",
                LiteralExpression.createStringLiteralExpression(url)
        );

        if (description != null) {
            expression = new MethodCallExpression(
                    expression,
                    "description",
                    LiteralExpression.createStringLiteralExpression(description)
            );
        }

        return expression;
    }

    private Expression createInfo(final OpenApi api) {
        final var apiInfo = api.info();

        Expression lastExpression = new NewClassExpression(new ClassOrInterfaceTypeExpression("io.swagger.v3.oas.models.info.Info"));

        final var title = apiInfo.title();

        if (title != null) {
            lastExpression = call(lastExpression, "title", title);
        }

        final String summary = apiInfo.summary();

        if (summary != null) {
            lastExpression = call(lastExpression, "summary", summary);
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

        final var licenseExpression = createLicense(apiInfo);

        if (licenseExpression != null) {
            lastExpression = call(lastExpression, "license", licenseExpression);
        }

        final var contact = apiInfo.contact();

        if (contact != null) {
            lastExpression = call(lastExpression, "contact", createContact(contact));
        }

        return lastExpression;
    }

    private Expression createLicense(final OpenApiInfo apiInfo) {
        final var licence = apiInfo.license();

        if (licence == null) {
            return null;
        }

        final var name = licence.name();
        final var url = licence.url();
        final var extensions = licence.extensions();

        Expression lastExpression = new NewClassExpression(new ClassOrInterfaceTypeExpression("io.swagger.v3.oas.models.info.License"));

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

        Expression lastExpression = new NewClassExpression(new ClassOrInterfaceTypeExpression("io.swagger.v3.oas.models.info.Contact"));

        if (name != null) {
            lastExpression = invoke(
                    lastExpression,
                    "name",
                    List.of(LiteralExpression.createStringLiteralExpression(name))
            ).build();
        }

        if (url != null) {
            lastExpression = new MethodCallExpression(
                    lastExpression,
                    "url",
                    List.of(LiteralExpression.createStringLiteralExpression(url))
            );
        }

        if (email != null) {
            lastExpression = new MethodCallExpression(
                    lastExpression,
                    "email",
                    List.of(LiteralExpression.createStringLiteralExpression(email))
            );
        }

        if (extensions != null && !extensions.isEmpty()) {
            final var type = new ClassOrInterfaceTypeExpression("java.util.Map",
                    List.of(
                            new ClassOrInterfaceTypeExpression("java.lang.String"),
                            new ClassOrInterfaceTypeExpression("java.lang.Object")
                    ));

            final var entries = extensions.entrySet().stream()
                    .map(entry -> (Expression) new MethodCallExpression(
                            type,
                            "entry",
                            List.of(
                                    LiteralExpression.createStringLiteralExpression(entry.getKey()),
                                    LiteralExpression.createStringLiteralExpression(((String) entry.getValue())
                            )
                    )))
                    .toList();

            lastExpression = new MethodCallExpression(
                    lastExpression,
                    "extensions",
                    List.of(
                            new MethodCallExpression(
                                    new ClassOrInterfaceTypeExpression("java.util.Map"),
                                    "ofEntries",
                                    entries
                            )
                    )
            );
        }

        return lastExpression;
    }

    private Expression createSecurityScheme(final OpenApiSecurityScheme openApiSecurityScheme) {
        final var type = openApiSecurityScheme.type().name();
        final var bearerFormat = openApiSecurityScheme.bearerFormat();
        final var description = openApiSecurityScheme.description();
        final var in = openApiSecurityScheme.in();

        Expression lastExpression = new NewClassExpression(new ClassOrInterfaceTypeExpression("io.swagger.v3.oas.models.security.SecurityScheme"));
        lastExpression = call(lastExpression, "name", LiteralExpression.createStringLiteralExpression(openApiSecurityScheme.name()));

        lastExpression = call(lastExpression, "type", new FieldAccessExpression(
                new ClassOrInterfaceTypeExpression("io.swagger.v3.oas.models.security.SecurityScheme.Type"),
                type
        ));
        lastExpression = call(lastExpression, "scheme", LiteralExpression.createStringLiteralExpression(openApiSecurityScheme.schema()));

        if (bearerFormat != null) {
            lastExpression = call(lastExpression, "bearerFormat", LiteralExpression.createStringLiteralExpression(bearerFormat));
        }

        if (description != null) {
            lastExpression = call(lastExpression, "description", LiteralExpression.createStringLiteralExpression(description));
        }

        if (in != null) {
            lastExpression = call(lastExpression, "in", new FieldAccessExpression(
                    new ClassOrInterfaceTypeExpression("io.swagger.v3.oas.models.security.SecurityScheme.In"),
                    in.name()
            ));
        }

        return lastExpression;
    }

    private Expression createSecurityRequirement(final OpenApiSecurityRequirement securityRequirement) {
        final var requirements = securityRequirement.requirements();
        final String name = requirements.keySet().iterator().next();
        final List<String> values = Objects.requireNonNullElse(requirements.get(name), List.of());

        Expression lastExpression = new NewClassExpression(
                new ClassOrInterfaceTypeExpression("io.swagger.v3.oas.models.security.SecurityRequirement")
        );

        final List<Expression> parameters = values.stream()
                .map(value -> (Expression) LiteralExpression.createStringLiteralExpression(value))
                .toList();

        final var arguments = new ArrayList<Expression>();
        arguments.add(LiteralExpression.createStringLiteralExpression(name));

        if (!parameters.isEmpty()) {
            arguments.add(
                    invoke(new ClassOrInterfaceTypeExpression(ClassNames.LIST_CLASS_NAME), "of", parameters)
                            .build()
            );
        }

        return invoke(lastExpression, "addList", arguments).build();
    }

    private MethodCallExpression call(final Expression target,
                                      final String name,
                                      final Object... arguments) {
        final var argumentList =
                Arrays.stream(arguments)
                        .map(this::createExpression)
                        .toList();

        return new MethodCallExpression(target, name, argumentList);
    }

    private Expression createExpression(final Object value) {
        if (value == null) {
            return LiteralExpression.createNullLiteralExpression();
        } else if (value instanceof Expression e) {
            return e;
        } else if (value instanceof String s) {
            return LiteralExpression.createStringLiteralExpression(s);
        } else if (value instanceof Map) {
            final Map<String, Object> map = (Map<String, Object>) value;
            final var entries = new ArrayList<Expression>();

            map.forEach((key, value1) -> entries.add(new MethodCallExpression(
                    new ClassOrInterfaceTypeExpression("java.util.Map"),
                    "entry",
                    List.of(
                            createExpression(key),
                            createExpression(value1)
                    )
            )));

            return new MethodCallExpression(
                    new ClassOrInterfaceTypeExpression("java.util.Map"),
                    "ofEntries",
                    entries
            );
        } else {
            throw new IllegalArgumentException(String.valueOf(value.getClass()));
        }
    }

}