package io.github.potjerodekool.openapi.internal.generate.config;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.Language;
import io.github.potjerodekool.codegen.model.CompilationUnit;
import io.github.potjerodekool.codegen.model.element.ElementKind;
import io.github.potjerodekool.codegen.model.element.Modifier;
import io.github.potjerodekool.codegen.model.element.Name;
import io.github.potjerodekool.codegen.model.tree.AnnotationExpression;
import io.github.potjerodekool.codegen.model.tree.MethodDeclaration;
import io.github.potjerodekool.codegen.model.tree.PackageDeclaration;
import io.github.potjerodekool.codegen.model.tree.expression.*;
import io.github.potjerodekool.codegen.model.tree.statement.BlockStatement;
import io.github.potjerodekool.codegen.model.tree.statement.ReturnStatement;
import io.github.potjerodekool.codegen.model.tree.statement.ClassDeclaration;
import io.github.potjerodekool.codegen.model.tree.type.ClassOrInterfaceTypeExpression;
import io.github.potjerodekool.openapi.GeneratorConfig;
import io.github.potjerodekool.openapi.generate.config.ApiConfigGenerator;
import io.github.potjerodekool.openapi.internal.ClassNames;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;

import java.util.*;

import static io.github.potjerodekool.openapi.internal.util.CollectionUtils.nonNull;

/**
 * Generates a configuration class with an OpenApiConfiguration bean.
 */
public class SpringOpenApiConfigGenerator implements ApiConfigGenerator {

    private final GeneratorConfig generatorConfig;
    private final Environment environment;

    public SpringOpenApiConfigGenerator(final GeneratorConfig generatorConfig,
                                        final Environment environment) {
        this.generatorConfig = generatorConfig;
        this.environment = environment;
    }

    private String getConfigClassName() {
        return "OpenApiConfiguration";
    }

    @Override
    public void generate(final OpenAPI openAPI) {
        final var cu = new CompilationUnit(Language.JAVA);

        final var configPackageName = generatorConfig.configPackageName();

        final var packageDeclaration = new PackageDeclaration(new IdentifierExpression(configPackageName));
        cu.packageDeclaration(packageDeclaration);

        final var classDeclaration = new ClassDeclaration()
                .simpleName(Name.of(getConfigClassName()))
                .kind(ElementKind.CLASS);
        classDeclaration.setEnclosing(packageDeclaration);

        classDeclaration.annotation("org.springframework.context.annotation.Configuration")
                .annotation(new AnnotationExpression("javax.annotation.processing.Generated", LiteralExpression.createStringLiteralExpression(getClass().getName())));

        fillClass(openAPI, classDeclaration);
        cu.classDeclaration(classDeclaration);

        environment.getCompilationUnits().add(cu);
    }

    private void fillClass(final OpenAPI openAPI,
                           final ClassDeclaration classDeclaration) {


        final var method = new MethodDeclaration()
                .kind(ElementKind.METHOD)
                .returnType(new ClassOrInterfaceTypeExpression("io.swagger.v3.oas.models.OpenAPI"))
                .simpleName(Name.of("api"))
                .modifier(Modifier.PUBLIC);

        classDeclaration.addEnclosed(method);
        method.annotation("org.springframework.context.annotation.Bean");

        Expression openApiInstance = new NewClassExpression(new ClassOrInterfaceTypeExpression(
                "io.swagger.v3.oas.models.OpenAPI"
        ));

        final var specVersion = createSpecVersion(openAPI, openApiInstance);
        final var servers = createServers(openAPI, openApiInstance);

        if (specVersion != null) {
            openApiInstance = specVersion;
        }

        if (servers != null) {
            openApiInstance = servers;
        }

        openApiInstance = new MethodCallExpression()
                .target(openApiInstance)
                .methodName("info")
                .arguments(List.of(createInfo(openAPI)));

        final var securityRequirements = nonNull(openAPI.getSecurity()).stream()
                .map(this::createSecurityRequirement)
                .toList();


        if (!securityRequirements.isEmpty()) {
            for (final var securityRequirement : securityRequirements) {
                openApiInstance = new MethodCallExpression()
                        .target(openApiInstance)
                        .methodName("addSecurityItem")
                        .argument(securityRequirement);
            }
        }

        method.body(new BlockStatement(new ReturnStatement(openApiInstance)));
    }

    private Expression createSpecVersion(final OpenAPI openAPI,
                                         final Expression openApiInstance) {
        final var versionFieldName = openAPI.getSpecVersion() == SpecVersion.V30
                ? "V30"
                : "V31";

        return new MethodCallExpression()
                .target(openApiInstance)
                .methodName("specVersion")
                .argument(new FieldAccessExpression(
                        new ClassOrInterfaceTypeExpression("io.swagger.v3.oas.models.SpecVersion"),
                        versionFieldName
                ));
    }

    private Expression createServers(final OpenAPI openAPI,
                                     final Expression openApiInstance) {
        final var servers = openAPI.getServers();

        if (servers == null) {
            return null;
        }

        final var serverArgs = servers.stream()
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

    private Expression createServer(final Server server) {
        final var url = server.getUrl();
        final var description = server.getDescription();

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

    private Expression createInfo(final OpenAPI openAPI) {
        final var apiInfo = openAPI.getInfo();

        Expression lastExpression = new NewClassExpression(new ClassOrInterfaceTypeExpression("io.swagger.v3.oas.models.info.Info"));

        final var title = apiInfo.getTitle();

        if (title != null) {
            lastExpression = new MethodCallExpression()
                    .target(lastExpression)
                    .methodName("title")
                    .argument(createExpression(title));
        }

        final String summary = apiInfo.getSummary();

        if (summary != null) {
            lastExpression = new MethodCallExpression()
                    .target(lastExpression)
                    .methodName("summary")
                    .argument(createExpression(summary));
        }

        final var description = apiInfo.getDescription();

        if (description != null) {
            lastExpression = new MethodCallExpression()
                    .target(lastExpression)
                    .methodName("description")
                    .argument(createExpression(description));
        }

        final var termsOfService = apiInfo.getTermsOfService();

        if (termsOfService != null) {
            lastExpression = new MethodCallExpression()
                    .target(lastExpression)
                    .methodName("termsOfService")
                    .argument(createExpression(termsOfService));
        }

        final var version = apiInfo.getVersion();

        if (version != null) {
            lastExpression = new MethodCallExpression()
                    .target(lastExpression)
                    .methodName("version")
                    .argument(createExpression(version));
        }

        final var extensions = apiInfo.getExtensions();

        if (extensions != null && !extensions.isEmpty()) {
            lastExpression = new MethodCallExpression()
                    .target(lastExpression)
                    .methodName("extensions")
                    .argument(createExpression(extensions));
        }

        final var licenseExpression = createLicense(apiInfo);

        if (licenseExpression != null) {
            lastExpression = new MethodCallExpression()
                    .target(lastExpression)
                    .methodName("license")
                    .argument(createExpression(licenseExpression));
        }

        final var contact = apiInfo.getContact();

        if (contact != null) {
            lastExpression = new MethodCallExpression()
                    .target(lastExpression)
                    .methodName("contact")
                    .argument(createContact(contact));
        }

        return lastExpression;
    }

    private Expression createLicense(final Info apiInfo) {
        final var licence = apiInfo.getLicense();

        if (licence == null) {
            return null;
        }

        final var name = licence.getName();
        final var url = licence.getUrl();
        final var extensions = licence.getExtensions();

        Expression lastExpression = new NewClassExpression(new ClassOrInterfaceTypeExpression("io.swagger.v3.oas.models.info.License"));

        if (name != null) {
            lastExpression = new MethodCallExpression()
                    .target(lastExpression)
                    .methodName("name")
                    .argument(createExpression(name));
        }

        if (url != null) {
            lastExpression = new MethodCallExpression()
                    .target(lastExpression)
                    .methodName("url")
                    .argument(createExpression(url));
        }

        if (extensions != null && !extensions.isEmpty()) {
            lastExpression = new MethodCallExpression()
                    .target(lastExpression)
                    .methodName("extensions")
                    .argument(createExpression(extensions));
        }

        return lastExpression;
    }

    private Expression createContact(final Contact contact) {
        final var name = contact.getName();
        final var url = contact.getUrl();
        final var email = contact.getEmail();
        final var extensions = contact.getExtensions();

        Expression lastExpression = new NewClassExpression(new ClassOrInterfaceTypeExpression("io.swagger.v3.oas.models.info.Contact"));

        if (name != null) {
            lastExpression = new MethodCallExpression()
                    .target(lastExpression)
                    .methodName("name")
                    .arguments(List.of(LiteralExpression.createStringLiteralExpression(name)));
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

    private Expression createSecurityRequirement(final SecurityRequirement securityRequirement) {
        Expression lastExpression = new NewClassExpression(
                new ClassOrInterfaceTypeExpression("io.swagger.v3.oas.models.security.SecurityRequirement")
        );

        for (final Map.Entry<String, List<String>> entry : securityRequirement.entrySet()) {
            final var methodCallArguments = new ArrayList<Expression>();


            if (!entry.getValue().isEmpty()) {
                final var arguments = entry.getValue().stream()
                        .map(LiteralExpression::createStringLiteralExpression)
                        .toList();

                methodCallArguments.add(new MethodCallExpression()
                        .target(new ClassOrInterfaceTypeExpression("java.util.List"))
                        .methodName("of")
                        .arguments(arguments)
                );
            }

            if (!methodCallArguments.isEmpty()) {
                final var methodCall = new MethodCallExpression()
                        .target(lastExpression)
                        .methodName("addList");
                methodCall.arguments(methodCallArguments);
                lastExpression = methodCall;
            }
        }

        return lastExpression;
    }

    private Expression createExpression(final Object value) {
        return switch (value) {
            case null -> LiteralExpression.createNullLiteralExpression();
            case String s -> LiteralExpression.createStringLiteralExpression(s);
            case Expression e -> e;
            case Map ignored -> {
                final Map<Object, Object> map = (Map<Object, Object>) value;
                final var entries = map.entrySet().stream()
                        .map(entry -> new MethodCallExpression(
                                new ClassOrInterfaceTypeExpression("java.util.Map"),
                                "entry",
                                List.of(
                                        createExpression(entry.getKey()),
                                        createExpression(entry.getValue())
                                )
                        ))
                        .toList();

                yield new MethodCallExpression(
                        new ClassOrInterfaceTypeExpression("java.util.Map"),
                        "ofEntries",
                        entries
                );
            }
            default -> throw new IllegalArgumentException(String.valueOf(value.getClass()));
        };
    }

}