package io.github.potjerodekool.openapi.internal.generate.springmvc.api;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.model.element.ElementKind;
import io.github.potjerodekool.codegen.model.element.Modifier;
import io.github.potjerodekool.codegen.model.element.Name;
import io.github.potjerodekool.codegen.model.tree.AnnotationExpression;
import io.github.potjerodekool.codegen.model.tree.MethodDeclaration;
import io.github.potjerodekool.codegen.model.tree.expression.*;
import io.github.potjerodekool.codegen.model.tree.statement.BlockStatement;
import io.github.potjerodekool.codegen.model.tree.statement.ClassDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.ReturnStatement;
import io.github.potjerodekool.codegen.model.tree.type.ArrayTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.ClassOrInterfaceTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.TypeExpression;
import io.github.potjerodekool.openapi.ApiConfiguration;
import io.github.potjerodekool.openapi.GeneratorConfig;
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
import io.github.potjerodekool.openapi.internal.util.CollectionUtils;
import io.swagger.models.HttpMethod;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.github.potjerodekool.openapi.internal.util.CollectionUtils.nonNull;

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
    protected String generateClasName(final String path,
                                      final PathItem openApiPath,
                                      final Operation operation) {
        return super.generateClasName(path, openApiPath, operation) + "Api";
    }

    @Override
    protected ClassDeclaration createClass(final String packageName, final Name simpleName) {
        return new ClassDeclaration()
                .simpleName(simpleName)
                .kind(ElementKind.INTERFACE)
                .modifier(Modifier.PUBLIC)
                .annotation(new AnnotationExpression("javax.annotation.processing.Generated", LiteralExpression.createStringLiteralExpression(getClass().getName())));
    }

    private AnnotationExpression createApiResponsesAnnotation(final OpenAPI openAPI,
                                                              final Operation operation) {
        final var responses = operation.getResponses().entrySet().stream()
                .map(response -> createApiResponse(openAPI, response))
                .toList();

        return new ApiResponsesAnnotationBuilder()
                .value(responses)
                .build();
    }

    private AnnotationExpression createApiResponse(final OpenAPI openAPI,
                                                   final Map.Entry<String, ApiResponse> entry) {
        final var response = entry.getValue();
        final var description = response.getDescription();

        final List<Expression> contentList = response.getContent() != null
                ? new ArrayList<>(response.getContent().entrySet().stream()
                .map(contentMediaType -> (Expression) createContentAnnotation(openAPI, contentMediaType.getKey(), contentMediaType.getValue()))
                .toList())
                : new ArrayList<>();

        if (contentList.isEmpty()) {
            contentList.add(createContentAnnotation(openAPI, "", null));
        }

        return new ApiResponseAnnotationBuilder()
                .responseCode(entry.getKey())
                .description(description)
                .headers(headers(openAPI, response.getHeaders()))
                        .content(contentList)
                        .build();
    }

    private AnnotationExpression createContentAnnotation(final OpenAPI openAPI,
                                                         final String mediaType,
                                                         final MediaType content) {
        final var contentAnnotationBuilder = new ContentAnnotationBuilder();

        if (content != null) {
            final var schema = content.getSchema();

            final var schemaType = openApiTypeUtils.createTypeExpression(mediaType, schema, openAPI);

            final List<Expression> examples = new ArrayList<>();

            if (content.getExamples() != null) {
                examples.addAll(content.getExamples().entrySet().stream()
                        .map(exampleEntry -> createExampleObject(exampleEntry.getKey(), exampleEntry.getValue()))
                        .toList());
            }

            contentAnnotationBuilder.mediaType(mediaType);
            contentAnnotationBuilder.examples(examples);

            if (schema instanceof ArraySchema) {
                TypeExpression elementType;

                if (schemaType instanceof ArrayTypeExpression arrayTypeExpression) {
                    elementType = (TypeExpression) arrayTypeExpression.getComponentTypeExpression();
                } else if (schemaType instanceof ClassOrInterfaceTypeExpression classOrInterfaceTypeExpression) {
                    elementType = classOrInterfaceTypeExpression.getTypeArguments().getFirst();
                } else {
                    throw new UnsupportedOperationException();
                }

                contentAnnotationBuilder.array(new ArraySchemaAnnotationBuilder()
                        .schema(
                                createSchemaAnnotation(schema)
                                        .implementation(openApiTypeUtils.resolveImplementationType(elementType, openAPI))
                                        .requiredMode(false)
                                        .build()
                        )
                        .build());
            } else if (schema instanceof MapSchema openApiMapSchema) {
                final var schemaPropertyAnnotation = new SchemaPropertyAnnotationBuilder()
                        .name("additionalProp1")
                        .build();

                final var additionalProperties = openApiMapSchema.getAdditionalProperties();

                if (additionalProperties == null) {
                    throw new IllegalStateException("Missing additionalProperties");
                }

                if (additionalProperties instanceof Schema<?> additionalPropertiesSchema) {
                    final var typeName = additionalPropertiesSchema.getName();
                    final var format = additionalPropertiesSchema.getFormat();
                    contentAnnotationBuilder.schemaProperties(schemaPropertyAnnotation);
                    contentAnnotationBuilder.additionalPropertiesSchema(new SchemaAnnotationBuilder()
                            .type(typeName)
                            .format(format)
                            .build());
                } else {
                    //TODO handle Boolean
                    throw new UnsupportedOperationException();
                }
            } else {
                contentAnnotationBuilder.schema(createSchemaAnnotation(schema)
                        .implementation(openApiTypeUtils.resolveImplementationType(schemaType, openAPI))
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

    private SchemaAnnotationBuilder createSchemaAnnotation(final Schema<?> schema) {
        final var builder = new SchemaAnnotationBuilder();

        if (schema != null) {
            final var requiredProperties = nonNull(schema.getRequired()).stream()
                    .map(LiteralExpression::createStringLiteralExpression)
                    .toList();

            builder.description(schema.getDescription())
                    .format(schema.getFormat())
                    .nullable(schema.getNullable())
                    .accessMode(schema.getReadOnly(), schema.getWriteOnly())
                    .requiredProperties(requiredProperties);
        }

        return builder;
    }

    private Expression createExampleObject(final String name,
                                           final Example openApiExample) {
        return new ExampleObjectAnnotationBuilder()
                .name(name)
                .summary(openApiExample.getSummary())
                .value(escapeJson(openApiExample.getValue().toString()))
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

    private ArrayInitializerExpression headers(final OpenAPI openAPI,
                                               final Map<String, Header> headersMap) {
        final List<Expression> headers = nonNull(headersMap).entrySet().stream()
                .map(entry -> {
                    final var header = entry.getValue();
                    final String description = header.getDescription();
                    final var required = header.getRequired();
                    final var deprecated = header.getDeprecated();
                    final var headerSchema = header.getSchema();

                    final var headerType = headerSchema != null
                            ? openApiTypeUtils.createTypeExpression(headerSchema, openAPI)
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
                                                         final Operation operation) {
        final var requestMappingAnnotationBuilder = switch (httpMethod) {
            case POST -> RequestMappingAnnotationBuilder.post();
            case GET -> RequestMappingAnnotationBuilder.get();
            case PUT -> RequestMappingAnnotationBuilder.put();
            case PATCH -> RequestMappingAnnotationBuilder.patch();
            case DELETE -> RequestMappingAnnotationBuilder.delete();
            default -> throw new UnsupportedOperationException();
        };

        final var produces = operation.getResponses().values().stream()
                .flatMap(it -> CollectionUtils.nonNull(it.getContent()).keySet().stream())
                .toList();

        final var requestBody = operation.getRequestBody();

        requestMappingAnnotationBuilder.value(path);

        if (requestBody != null
                && requestBody.getContent() != null
                && !requestBody.getContent().isEmpty()) {
            final var consumes = new CollectionBuilder<String>()
                    .addAll(requestBody.getContent().keySet())
                    .addAll(requestBody.getContent().keySet().stream()
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
    protected void postProcessOperation(final OpenAPI openAPI,
                                        final HttpMethod httpMethod,
                                        final String path,
                                        final Operation operation,
                                        final MethodDeclaration method) {
        method.modifier(Modifier.DEFAULT);

        method.annotation(
                new OperationAnnotationBuilder()
                        .summary(operation.getSummary())
                        .operationId(operation.getOperationId())
                        .tags(operation.getTags())
                        .requestBody(createRequestBody(openAPI, operation.getRequestBody()))
                        .build()
        );

        final var securityRequirements = operation.getSecurity();

        if (securityRequirements != null) {
            final var securityRequirementsAnnotation = createSecurityRequirementsAnnotation(securityRequirements);
            if (securityRequirementsAnnotation != null) {
                method.annotation(securityRequirementsAnnotation);
            }
        }

        final var apiResponsesAnnotation = createApiResponsesAnnotation(openAPI, operation);

        method.annotation(apiResponsesAnnotation);
        method.annotation(createMappingAnnotation(httpMethod, path, operation));

        final var body = new BlockStatement();

        body.add(new ReturnStatement(
                new MethodCallExpression()
                        .target(new ClassOrInterfaceTypeExpression("org.springframework.http.ResponseEntity"))
                        .methodName("status")
                        .argument(new FieldAccessExpression("org.springframework.http.HttpStatus", "NOT_IMPLEMENTED"))
                        .invoke("build")
        ));

        method.body(body);
    }

    private AnnotationExpression createRequestBody(final OpenAPI openAPI,
                                                   final RequestBody openApiRequestBody) {
        if (openApiRequestBody == null) {
            return null;
        }

        final var requestBodyBuilder = new RequestBodyAnnotationBuilder();

        final var contentList = openApiRequestBody.getContent().entrySet().stream()
                .map(entry -> createContentAnnotation(openAPI, entry.getKey(), entry.getValue()))
                .toList();

        requestBodyBuilder.content(contentList);
        return requestBodyBuilder.build();
    }

    private AnnotationExpression createSecurityRequirementsAnnotation(final List<SecurityRequirement> securityRequirements) {
        final var annotations = new ArrayList<Expression>();

        for (final SecurityRequirement securityRequirement : securityRequirements) {
            for (final Map.Entry<String, List<String>> entry : securityRequirement.entrySet()) {
                final String name = entry.getKey();
                final var values = entry.getValue();

                annotations.add(new SecurityRequirementAnnotationBuilder()
                        .name(name)
                        .scopes(values)
                        .build());
            }
        }

        return !annotations.isEmpty()
                ? new SecurityRequirementsBuilder().value(annotations).build()
                : null;
    }
}
