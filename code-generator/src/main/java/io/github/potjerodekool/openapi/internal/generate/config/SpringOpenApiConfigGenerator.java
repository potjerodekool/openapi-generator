package io.github.potjerodekool.openapi.internal.generate.config;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.model.element.Modifier;
import io.github.potjerodekool.codegen.model.symbol.AbstractSymbol;
import io.github.potjerodekool.codegen.model.symbol.ClassSymbol;
import io.github.potjerodekool.codegen.model.tree.expression.*;
import io.github.potjerodekool.codegen.model.tree.statement.BlockStatement;
import io.github.potjerodekool.codegen.model.tree.statement.ReturnStatement;
import io.github.potjerodekool.codegen.model.type.AbstractType;
import io.github.potjerodekool.codegen.model.type.DeclaredType;
import io.github.potjerodekool.openapi.GeneratorConfig;
import io.github.potjerodekool.openapi.tree.*;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
                             final ClassSymbol typeElement) {
        final var openApiType = (AbstractType) getTypes().getDeclaredType(
                getElementUtils().getTypeElement("io.swagger.v3.oas.models.OpenAPI")
        );

        final var method = typeElement.addMethod("api", openApiType, Modifier.PUBLIC);
        method.addAnnotation((ClassSymbol) getElementUtils().getTypeElement("org.springframework.context.annotation.Bean"));

        Expression openApiInstance = new NewClassExpression((DeclaredType) openApiType);

        openApiInstance = new MethodCallExpression(openApiInstance, "info", List.of(createInfo(api)));

        final var components = createComponents(api);
        if (components != null) {
            openApiInstance = new MethodCallExpression(openApiInstance, "components", List.of(components));
        }

        final var securityRequirements = api.securityRequirements().stream()
                .map(this::createSecurityRequirement)
                .toList();

        if (securityRequirements.size() > 0) {
            for (final var securityRequirement : securityRequirements) {
                openApiInstance = call(openApiInstance, "addSecurityItem", securityRequirement);
            }
        }

        method.setBody(new BlockStatement(new ReturnStatement(openApiInstance)));
    }

    private Expression createInfo(final OpenApi api) {
        final var apiInfo = api.info();

        Expression lastExpression = new NewClassExpression(
                getTypes().getDeclaredType(getElementUtils().getTypeElement("io.swagger.v3.oas.models.info.Info"))
        );

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

        Expression lastExpression = new NewClassExpression(
                getTypes().getDeclaredType(
                        getElementUtils().getTypeElement("io.swagger.v3.oas.models.info.License"))
        );

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

        Expression lastExpression = new NewClassExpression(
                getTypes().getDeclaredType(
                        getElementUtils().getTypeElement("io.swagger.v3.oas.models.info.Contact")
                )
        );

        if (name != null) {
            lastExpression = new MethodCallExpression(
                    lastExpression,
                    "name",
                    List.of(LiteralExpression.createStringLiteralExpression(name))
            );
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

        if (extensions != null) {
            final var entries = extensions.entrySet().stream()
                    .map(entry -> (Expression) new MethodCallExpression(
                            new NameExpression("java.util.Map"),
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
                                    new NameExpression("java.util.Map"),
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

        Expression lastExpression = new NewClassExpression(
                getTypes().getDeclaredType(
                        getElementUtils().getTypeElement("io.swagger.v3.oas.models.Components"))
        );

        final var securitySchemas = openApi.securitySchemas();

        if (securitySchemas.size() > 0) {
            created = true;
            for (final var entry : securitySchemas.entrySet()) {
                lastExpression = call(lastExpression, "addSecuritySchemes",
                        LiteralExpression.createStringLiteralExpression(entry.getKey()),
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

        Expression lastExpression = new NewClassExpression(
                getTypes().getDeclaredType(getElementUtils().getTypeElement("io.swagger.v3.oas.models.security.SecurityScheme"))
        );
        lastExpression = call(lastExpression, "name", LiteralExpression.createStringLiteralExpression(openApiSecurityScheme.name()));

        final var securitySchemeType = (AbstractSymbol<?>) getElementUtils().getTypeElement("io.swagger.v3.oas.models.security.SecurityScheme$Type");

        lastExpression = call(lastExpression, "type", new FieldAccessExpression(
                new NameExpression("io.swagger.v3.oas.models.security.SecurityScheme.Type", securitySchemeType),
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
            final var securitySchemaInElement = (AbstractSymbol<?>) getElementUtils().getTypeElement("io.swagger.v3.oas.models.security.SecurityScheme$In");

            lastExpression = call(lastExpression, "in", new FieldAccessExpression(
                    new NameExpression("io.swagger.v3.oas.models.security.SecurityScheme.In", securitySchemaInElement),
                    in.name()
            ));
        }

        return lastExpression;
    }

    private Expression createSecurityRequirement(final OpenApiSecurityRequirement securityRequirement) {
        final var requirements = securityRequirement.requirements();
        final String name = requirements.keySet().iterator().next();
        final var securityParameter = requirements.get(name);

        Expression lastExpression = new NewClassExpression(
                getTypes().getDeclaredType(getElementUtils().getTypeElement("io.swagger.v3.oas.models.security.SecurityRequirement"))
        );

        final List<Expression> parameters = securityParameter.parameters().stream()
                .map(param -> (Expression) LiteralExpression.createStringLiteralExpression(param))
                .toList();

        final var arguments = new ArrayList<Expression>();
        arguments.add(LiteralExpression.createStringLiteralExpression(name));

        if (parameters.size() > 0) {
            final var listElement = (AbstractSymbol<?>) getElementUtils().getTypeElement("java.util.List");

            arguments.add(
                new MethodCallExpression(
                        new NameExpression("java.util.List"),
                        "of",
                        parameters
                )
            );
        }

        return new MethodCallExpression(lastExpression, "addList", arguments);
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
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) value;
            final var entries = new ArrayList<Expression>();

            map.forEach((key, value1) -> entries.add(new MethodCallExpression(
                    new NameExpression("java.util.Map"),
                    "entry",
                    List.of(
                            createExpression(key),
                            createExpression(value1)
                    )
            )));

            return new MethodCallExpression(
                    new NameExpression("java.util.Map"),
                    "ofEntries",
                    entries
            );
        } else {
            throw new IllegalArgumentException("" + value.getClass());
        }
    }

}