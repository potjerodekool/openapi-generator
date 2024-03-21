package io.github.potjerodekool.openapi.spring.web.generate.api;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.model.element.ElementKind;
import io.github.potjerodekool.codegen.model.element.Modifier;
import io.github.potjerodekool.codegen.model.util.QualifiedName;
import io.github.potjerodekool.codegen.template.model.annotation.Annot;
import io.github.potjerodekool.codegen.template.model.element.MethodElem;
import io.github.potjerodekool.codegen.template.model.element.TypeElem;
import io.github.potjerodekool.codegen.template.model.expression.*;
import io.github.potjerodekool.codegen.template.model.statement.BlockStm;
import io.github.potjerodekool.codegen.template.model.statement.ReturnStm;
import io.github.potjerodekool.codegen.template.model.type.ClassOrInterfaceTypeExpr;
import io.github.potjerodekool.codegen.template.model.type.TypeExpr;
import io.github.potjerodekool.openapi.common.ApiConfiguration;
import io.github.potjerodekool.openapi.common.GeneratorConfig;
import io.github.potjerodekool.openapi.common.generate.annotation.openapi.media.*;
import io.github.potjerodekool.openapi.common.generate.OpenApiTypeUtils;
import io.github.potjerodekool.openapi.common.generate.annotation.OperationAnnotationBuilder;
import io.github.potjerodekool.openapi.common.generate.annotation.openapi.header.HeaderAnnotationBuilder;
import io.github.potjerodekool.openapi.common.generate.annotation.openapi.parameter.RequestBodyAnnotationBuilder;
import io.github.potjerodekool.openapi.common.generate.annotation.openapi.response.ApiResponseAnnotationBuilder;
import io.github.potjerodekool.openapi.common.generate.annotation.openapi.response.ApiResponsesAnnotationBuilder;
import io.github.potjerodekool.openapi.common.generate.annotation.openapi.security.SecurityRequirementAnnotationBuilder;
import io.github.potjerodekool.openapi.common.generate.annotation.openapi.security.SecurityRequirementsBuilder;
import io.github.potjerodekool.openapi.spring.web.generate.annotation.spring.web.RequestMappingAnnotationBuilder;
import io.github.potjerodekool.openapi.common.util.CollectionBuilder;
import io.github.potjerodekool.openapi.common.util.CollectionUtils;
import io.swagger.models.HttpMethod;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.github.potjerodekool.openapi.common.util.CollectionUtils.nonNull;

public class SpringApiGenerator extends AbstractApiGenerator {
    public SpringApiGenerator(final GeneratorConfig generatorConfig,
                              final ApiConfiguration apiConfiguration,
                              final OpenApiTypeUtils typeUtils,
                              final Environment environment) {
        super(generatorConfig, apiConfiguration, typeUtils, environment);
    }

    @Override
    protected TypeElem createClass(final String simpleName) {
        return super.createClass(simpleName)
                .kind(ElementKind.INTERFACE);
    }

    @Override
    protected String classNameSuffix() {
        return "Api";
    }

    private Annot createApiResponsesAnnotation(final OpenAPI openAPI,
                                               final Operation operation) {
        final List<Annot> responses;

        if (operation.getResponses() != null) {
            responses = operation.getResponses().entrySet().stream()
                    .map(response -> createApiResponse(openAPI, response))
                    .toList();
        } else {
            responses = new ArrayList<>();
        }

        return new ApiResponsesAnnotationBuilder()
                .value(responses)
                .build();
    }

    private Annot createApiResponse(final OpenAPI openAPI,
                                    final Map.Entry<String, ApiResponse> entry) {
        final var response = entry.getValue();
        final var description = response.getDescription();

        final List<Expr> contentList = response.getContent() != null
                ? new ArrayList<>(response.getContent().entrySet().stream()
                .map(contentMediaType -> (Expr) createContentAnnotation(openAPI, null, contentMediaType.getKey(), contentMediaType.getValue()))
                .toList())
                : new ArrayList<>();

        if (contentList.isEmpty()) {
            contentList.add(createContentAnnotation(openAPI, null,"", null));
        }

        return new ApiResponseAnnotationBuilder()
                .responseCode(entry.getKey())
                .description(description)
                .headers(headers(openAPI, response.getHeaders()))
                .content(contentList)
                .build();
    }

    private Annot createContentAnnotation(final OpenAPI openAPI,
                                          final HttpMethod httpMethod,
                                          final String mediaType,
                                          final MediaType content) {
        final var contentAnnotationBuilder = new ContentAnnotationBuilder();

        if (content != null) {
            final var schema = content.getSchema();

            final var schemaType = getTypeUtils().createType(
                    openAPI,
                    schema,
                    null,
                    getModelPackageName(),
                    mediaType,
                    null);

            if (httpMethod == HttpMethod.PATCH) {
                var classType = (ClassOrInterfaceTypeExpr) schemaType;

                if (classType.getTypeArguments() != null && classType.getTypeArguments().size() == 1) {
                    classType = (ClassOrInterfaceTypeExpr) classType.getTypeArguments().getFirst();
                }

                final var qualifiedName = QualifiedName.from(classType.getName());
                var simpleName = qualifiedName.simpleName().toString();
                final var packageName = qualifiedName.packageName().toString();

                if (!simpleName.contains("Patch")) {
                    simpleName = "Patch" + simpleName;
                }
                classType.name(packageName + "." + simpleName);
            }

            final List<Expr> examples = new ArrayList<>();

            if (content.getExamples() != null) {
                examples.addAll(content.getExamples().entrySet().stream()
                        .map(exampleEntry -> createExampleObject(exampleEntry.getKey(), exampleEntry.getValue()))
                        .toList());
            }

            contentAnnotationBuilder.mediaType(mediaType);
            contentAnnotationBuilder.examples(examples);

            if (schema instanceof ArraySchema) {
                TypeExpr elementType;

                if (schemaType instanceof ArrayExpr arrayTypeExpression) {
                    elementType = arrayTypeExpression.getComponentType();
                } else if (schemaType instanceof ClassOrInterfaceTypeExpr classOrInterfaceTypeExpression) {
                    elementType = classOrInterfaceTypeExpression.getTypeArguments().getFirst();
                } else {
                    throw new UnsupportedOperationException();
                }

                contentAnnotationBuilder.array(new ArraySchemaAnnotationBuilder()
                        .schema(
                                createSchemaAnnotation(schema)
                                        .implementation(getTypeUtils().resolveImplementationType(openAPI, elementType))
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
                    var typeName = additionalPropertiesSchema.getName();

                    if (typeName == null && additionalPropertiesSchema instanceof ObjectSchema) {
                        typeName = "object";
                    }

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
                        .implementation(getTypeUtils().resolveImplementationType(openAPI, schemaType))
                        .requiredMode(false)
                        .build());
            }
        } else {
            contentAnnotationBuilder.schema(
                    new SchemaAnnotationBuilder()
                            .implementation(new ClassOrInterfaceTypeExpr("java.lang.Void"))
                            .build()
            );
        }

        return contentAnnotationBuilder.build();
    }

    private SchemaAnnotationBuilder createSchemaAnnotation(final Schema<?> schema) {
        final var builder = new SchemaAnnotationBuilder();

        if (schema != null) {
            final var requiredProperties = nonNull(schema.getRequired()).stream()
                    .map(SimpleLiteralExpr::new)
                    .toList();

            builder.description(schema.getDescription())
                    .format(schema.getFormat())
                    .nullable(schema.getNullable())
                    .accessMode(schema.getReadOnly(), schema.getWriteOnly())
                    .requiredProperties(requiredProperties);
        }

        return builder;
    }

    private Expr createExampleObject(final String name,
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

    private ArrayExpr headers(final OpenAPI openAPI,
                              final Map<String, Header> headersMap) {
        final List<Expr> headers = nonNull(headersMap).entrySet().stream()
                .map(entry -> {
                    final var header = entry.getValue();
                    final String description = header.getDescription();
                    final var required = header.getRequired();
                    final var deprecated = header.getDeprecated();
                    final var headerSchema = header.getSchema();

                    final var headerType = headerSchema != null
                            ? getTypeUtils().createType(
                            openAPI,
                            headerSchema,
                            null,
                            getModelPackageName(),
                            null,
                            required
                    )
                            : null;

                    return (Expr) new HeaderAnnotationBuilder()
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

        return new ArrayExpr().values(headers);
    }


    private Annot createMappingAnnotation(final HttpMethod httpMethod,
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

        final var produces = CollectionUtils.nonNull(operation.getResponses()).values().stream()
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
                                        final MethodElem method) {
        method.modifier(Modifier.DEFAULT);

        method.annotation(
                new OperationAnnotationBuilder()
                        .summary(operation.getSummary())
                        .operationId(operation.getOperationId())
                        .tags(operation.getTags())
                        .requestBody(createRequestBody(openAPI, httpMethod, operation.getRequestBody()))
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

        final var body = new BlockStm();

        body.statement(new ReturnStm(
                new MethodInvocationExpr()
                        .target(new ClassOrInterfaceTypeExpr("org.springframework.http.ResponseEntity"))
                        .name("status")
                        .argument(new FieldAccessExpr()
                                .target(new IdentifierExpr("org.springframework.http.HttpStatus"))
                                .field(new IdentifierExpr("NOT_IMPLEMENTED"))
                        )
                        .invoke("build")
        ));

        method.body(body);
    }

    private Annot createRequestBody(final OpenAPI openAPI,
                                    final HttpMethod httpMethod,
                                    final RequestBody openApiRequestBody) {
        if (openApiRequestBody == null) {
            return null;
        }

        final var requestBodyBuilder = new RequestBodyAnnotationBuilder();

        final var contentList = openApiRequestBody.getContent().entrySet().stream()
                .map(entry -> createContentAnnotation(openAPI, httpMethod, entry.getKey(), entry.getValue()))
                .toList();

        requestBodyBuilder.content(contentList);
        return requestBodyBuilder.build();
    }

    private Annot createSecurityRequirementsAnnotation(final List<SecurityRequirement> securityRequirements) {
        final var annotations = new ArrayList<Expr>();

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
