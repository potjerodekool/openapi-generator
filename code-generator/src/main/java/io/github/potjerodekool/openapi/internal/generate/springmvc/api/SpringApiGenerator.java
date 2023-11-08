package io.github.potjerodekool.openapi.internal.generate.springmvc.api;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.model.element.ElementKind;
import io.github.potjerodekool.codegen.model.element.Modifier;
import io.github.potjerodekool.codegen.model.element.Name;
import io.github.potjerodekool.codegen.model.tree.AnnotationExpression;
import io.github.potjerodekool.codegen.model.tree.expression.ArrayInitializerExpression;
import io.github.potjerodekool.codegen.model.tree.expression.Expression;
import io.github.potjerodekool.codegen.model.tree.expression.FieldAccessExpression;
import io.github.potjerodekool.codegen.model.tree.expression.LiteralExpression;
import io.github.potjerodekool.codegen.model.tree.java.JMethodDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.BlockStatement;
import io.github.potjerodekool.codegen.model.tree.statement.ReturnStatement;
import io.github.potjerodekool.codegen.model.tree.statement.java.JClassDeclaration;
import io.github.potjerodekool.codegen.model.tree.type.ArrayTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.ClassOrInterfaceTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.TypeExpression;
import io.github.potjerodekool.openapi.ApiConfiguration;
import io.github.potjerodekool.openapi.GeneratorConfig;
import io.github.potjerodekool.openapi.HttpMethod;
import io.github.potjerodekool.openapi.internal.generate.annotation.openapi.OperationAnnotationBuilder;
import io.github.potjerodekool.openapi.internal.generate.annotation.openapi.header.HeaderAnnotationBuilder;
import io.github.potjerodekool.openapi.internal.generate.annotation.openapi.media.*;
import io.github.potjerodekool.openapi.internal.generate.annotation.openapi.parameter.RequestBodyAnnotationBuilder;
import io.github.potjerodekool.openapi.internal.generate.annotation.openapi.response.ApiResponseAnnotationBuilder;
import io.github.potjerodekool.openapi.internal.generate.annotation.openapi.response.ApiResponsesAnnotationBuilder;
import io.github.potjerodekool.openapi.internal.generate.annotation.openapi.security.SecurityRequirementAnnotationBuilder;
import io.github.potjerodekool.openapi.internal.generate.annotation.openapi.security.SecurityRequirementsBuilder;
import io.github.potjerodekool.openapi.internal.generate.annotation.spring.web.RequestMappingAnnotationBuilder;
import io.github.potjerodekool.openapi.internal.generate.api.AbstractApiGenerator;
import io.github.potjerodekool.openapi.internal.type.OpenApiTypeUtils;
import io.github.potjerodekool.openapi.internal.util.CollectionBuilder;
import io.github.potjerodekool.openapi.tree.*;
import io.github.potjerodekool.openapi.tree.media.OpenApiArraySchema;
import io.github.potjerodekool.openapi.tree.media.OpenApiMapSchema;
import io.github.potjerodekool.openapi.tree.media.OpenApiSchema;
import org.jdom.output.EscapeStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.github.potjerodekool.codegen.model.tree.expression.ExpressionBuilder.expression;
import static io.github.potjerodekool.codegen.model.tree.expression.MethodCallExpressionBuilder.invoke;

public class SpringApiGenerator extends AbstractApiGenerator {

    private final OpenApiTypeUtils openApiTypeUtils;

    public SpringApiGenerator(final GeneratorConfig generatorConfig,
                              final ApiConfiguration apiConfiguration,
                              final OpenApiTypeUtils openApiTypeUtils,
                              final Environment environment) {
        super(generatorConfig, environment, openApiTypeUtils, apiConfiguration);
        this.openApiTypeUtils = openApiTypeUtils;
    }

    @Override
    protected String generateClasName(final OpenApiPath openApiPath, final OpenApiOperation operation) {
        return super.generateClasName(openApiPath, operation) + "Api";
    }

    @Override
    protected JClassDeclaration createClass(final String packageName, final Name simpleName) {
        return new JClassDeclaration(simpleName, ElementKind.INTERFACE)
                .modifier(Modifier.PUBLIC)
                .annotation(new AnnotationExpression("javax.annotation.processing.Generated", LiteralExpression.createStringLiteralExpression(getClass().getName())));
    }

    private AnnotationExpression createApiResponsesAnnotation(final OpenApiOperation operation) {
        final var responses = operation.responses().entrySet().stream()
                .map(this::createApiResponse)
                .toList();

        return new ApiResponsesAnnotationBuilder()
                .value(responses)
                .build();
    }

    private AnnotationExpression createApiResponse(final Map.Entry<String, OpenApiResponse> entry) {
        final var response = entry.getValue();
        final var description = response.description();

        final List<Expression> contentList = new ArrayList<>(response.contentMediaType().entrySet().stream()
                .map(contentMediaType -> (Expression) createContentAnnotation(contentMediaType.getKey(), contentMediaType.getValue()))
                .toList());

        if (contentList.isEmpty()) {
            contentList.add(createContentAnnotation("", null));
        }

        return new ApiResponseAnnotationBuilder()
                .responseCode(entry.getKey())
                .description(description)
                .headers(headers(response.headers()))
                .content(contentList)
                .build();
    }

    private AnnotationExpression createContentAnnotation(final String mediaType,
                                                         final OpenApiContent content) {
        final var contentAnnotationBuilder = new ContentAnnotationBuilder();

        if (content != null) {
            final var schema = content.schema();

            final var schemaType = openApiTypeUtils.createTypeExpression(mediaType, schema);

            final var examples = content.examples().entrySet().stream()
                    .map(exampleEntry -> createExampleObject(exampleEntry.getKey(), exampleEntry.getValue()))
                    .toList();

            contentAnnotationBuilder.mediaType(mediaType);
            contentAnnotationBuilder.examples(examples);

            if (schema instanceof OpenApiArraySchema) {
                TypeExpression elementType;

                if (schemaType instanceof ArrayTypeExpression arrayTypeExpression) {
                    elementType = (TypeExpression) arrayTypeExpression.getComponentTypeExpression();
                } else if (schemaType instanceof ClassOrInterfaceTypeExpression classOrInterfaceTypeExpression) {
                    elementType = classOrInterfaceTypeExpression.getTypeArguments().get(0);
                } else {
                    throw new UnsupportedOperationException();
                }

                contentAnnotationBuilder.array(new ArraySchemaAnnotationBuilder()
                        .schema(
                                createSchemaAnnotation(schema)
                                        .implementation(openApiTypeUtils.resolveImplementationType(elementType))
                                        .requiredMode(false)
                                        .build()
                        )
                        .build());
            } else if (schema instanceof OpenApiMapSchema openApiMapSchema) {
                final var schemaPropertyAnnotation = new SchemaPropertyAnnotationBuilder()
                        .name("additionalProp1")
                        .build();

                final var additionalProperties = openApiMapSchema.additionalProperties();

                if (additionalProperties == null) {
                    throw new IllegalStateException("Missing additionalProperties");
                }

                final var typeName = additionalProperties.name();
                final var format = additionalProperties.format();

                contentAnnotationBuilder.schemaProperties(schemaPropertyAnnotation);
                contentAnnotationBuilder.additionalPropertiesSchema(new SchemaAnnotationBuilder()
                        .type(typeName)
                        .format(format)
                        .build());
            } else {
                contentAnnotationBuilder.schema(createSchemaAnnotation(schema)
                        .implementation(openApiTypeUtils.resolveImplementationType(schemaType))
                        .requiredMode(false)
                        .build());
            }
        } else {
            contentAnnotationBuilder.schema(
                    new SchemaAnnotationBuilder()
                            .implementation(new ClassOrInterfaceTypeExpression("java.lang.Void"))
                            .build()
            );
        }

        return contentAnnotationBuilder.build();
    }

    private SchemaAnnotationBuilder createSchemaAnnotation(final OpenApiSchema<?> schema) {
        final var builder = new SchemaAnnotationBuilder();

        if (schema != null) {
            final var requiredProperties = schema.required().stream()
                    .map(LiteralExpression::createStringLiteralExpression)
                    .toList();

            builder.description(schema.description())
                    .format(schema.format())
                    .nullable(schema.nullable())
                    .accessMode(schema.readOnly(), schema.writeOnly())
                    .requiredProperties(requiredProperties);
        }

        return builder;
    }

    private Expression createExampleObject(final String name,
                                           final OpenApiExample openApiExample) {
        return new ExampleObjectAnnotationBuilder()
                .name(name)
                .summary(openApiExample.summary())
                .value(escapeJson(openApiExample.value().toString()))
                .build();
    }

    private String escapeJson(final String s) {
        final var sb = new StringBuilder();
        char pc = (char) -1;

        for (char c : s.toCharArray()) {
            if (c == '"' && pc != '\\') {
                sb.append('\\');
            }
            sb.append(c);
            pc = c;
        }

        return sb.toString();
    }

    private ArrayInitializerExpression headers(final Map<String, OpenApiHeader> headersMap) {
        final List<Expression> headers =  headersMap.entrySet().stream()
                        .map(entry -> {
                            final var header = entry.getValue();
                            final String description = header.description();
                            final var required = header.required();
                            final var deprecated = header.deprecated();
                            final var headerSchema = header.schema();

                            final var headerType = headerSchema != null
                                    ? openApiTypeUtils.createTypeExpression(headerSchema)
                                    : null;

                            return (Expression) new HeaderAnnotationBuilder()
                                    .name(entry.getKey())
                                    .description(description)
                                    .required(required)
                                    .deprecated(deprecated)
                                    .schema(
                                            new SchemaAnnotationBuilder()
                                                    .implementation(headerType)
                                                    .build()
                                    )
                                    .build();
                        }).toList();

        return new ArrayInitializerExpression(headers);
    }


    private AnnotationExpression createMappingAnnotation(final HttpMethod httpMethod,
                                                         final String path,
                                                         final OpenApiOperation operation) {
        final var requestMappingAnnotationBuilder = switch (httpMethod) {
            case POST -> RequestMappingAnnotationBuilder.post();
            case GET -> RequestMappingAnnotationBuilder.get();
            case PUT -> RequestMappingAnnotationBuilder.put();
            case PATCH -> RequestMappingAnnotationBuilder.patch();
            case DELETE -> RequestMappingAnnotationBuilder.delete();
        };

        final var produces = operation.responses().values().stream()
                .flatMap(it -> it.contentMediaType().keySet().stream())
                .toList();

        final var requestBody = operation.requestBody();

        requestMappingAnnotationBuilder.value(path);

        if (requestBody != null && !requestBody.contentMediaType().isEmpty()) {
            final var consumes = new CollectionBuilder<String>()
                    .addAll(requestBody.contentMediaType().keySet())
                    .addAll(requestBody.contentMediaType().keySet().stream()
                            .filter(it -> it.startsWith("image/"))
                            .map(contentMediaType -> contentMediaType + ";charset=UTF-8")
                            .toList())
                    .buildList();
            requestMappingAnnotationBuilder.consumes(consumes);
        }

        return requestMappingAnnotationBuilder.produces(produces)
                .build();
    }

    @Override
    protected void postProcessOperation(final HttpMethod httpMethod,
                                        final String path,
                                        final OpenApiOperation operation,
                                        final JMethodDeclaration method) {
        method.addModifier(Modifier.DEFAULT);

        method.annotation(
                new OperationAnnotationBuilder()
                        .summary(operation.summary())
                        .operationId(operation.operationId())
                        .tags(operation.tags())
                        .requestBody(createRequestBody(operation.requestBody()))
                        .build()
        );

        final var securityRequirements = operation.securityRequirements();

        if (securityRequirements != null) {
            final var securityRequirementsAnnotation = createSecurityRequirementsAnnotation(securityRequirements);
            if (securityRequirementsAnnotation != null) {
                method.annotation(securityRequirementsAnnotation);
            }
        }

        final var apiResponsesAnnotation = createApiResponsesAnnotation(operation);

        method.annotation(apiResponsesAnnotation);
        method.annotation(createMappingAnnotation(httpMethod, path, operation));

        final var body = new BlockStatement();

        body.add(new ReturnStatement(
                expression(
                        invoke("org.springframework.http.ResponseEntity", "status")
                                .withArgs(new FieldAccessExpression("org.springframework.http.HttpStatus", "NOT_IMPLEMENTED"))
                                .invoke("build")
                )
        ));

        method.setBody(body);
    }

    private AnnotationExpression createRequestBody(final OpenApiRequestBody openApiRequestBody) {
        if (openApiRequestBody == null) {
            return null;
        }

        final var requestBodyBuilder = new RequestBodyAnnotationBuilder();

        final var contentList = openApiRequestBody.contentMediaType().entrySet().stream()
                .map(entry -> createContentAnnotation(entry.getKey(), entry.getValue()))
                .toList();

        requestBodyBuilder.content(contentList);
        return requestBodyBuilder.build();
    }

    private AnnotationExpression createSecurityRequirementsAnnotation(final List<OpenApiSecurityRequirement> securityRequirements) {
        final var annotations = securityRequirements.stream()
                .map(OpenApiSecurityRequirement::requirements)
                .map(securityParameterMap -> {
                    final String name;
                    final List<String> parameters;

                    if (securityParameterMap.isEmpty()) {
                        name = "";
                        parameters = List.of();
                    } else {
                        name = securityParameterMap.keySet().iterator().next();
                        parameters = Objects.requireNonNullElseGet(
                                securityParameterMap.get(name),
                                List::of
                        );
                    }

                    return (Expression) new SecurityRequirementAnnotationBuilder()
                            .name(name)
                            .scopes(parameters)
                            .build();
                })
                .toList();

        return !annotations.isEmpty()
                ? new SecurityRequirementsBuilder().value(annotations).build()
                : null;
    }
}
