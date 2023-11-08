package io.github.potjerodekool.openapi.internal;

import io.github.potjerodekool.openapi.ApiConfiguration;
import io.github.potjerodekool.openapi.ParameterLocation;
import io.github.potjerodekool.openapi.tree.*;
import io.github.potjerodekool.openapi.tree.Package;
import io.github.potjerodekool.openapi.tree.enums.OpenApiSecuritySchemeIn;
import io.github.potjerodekool.openapi.tree.enums.OpenApiSecuritySchemeType;
import io.github.potjerodekool.openapi.tree.media.*;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariables;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.util.*;
import java.util.stream.Collectors;

public class OpenApiTreeBuilder {

    private final String modelPackageName;

    public OpenApiTreeBuilder(final ApiConfiguration apiConfiguration) {
        this.modelPackageName = apiConfiguration.modelPackageName();
    }

    public OpenApi build(final SwaggerParseResult result) {
        final var openApi = result.getOpenAPI();
        final var specVersion = openApi.getSpecVersion();
        final var version = specVersion == SpecVersion.V30
                ? "3.0.0"
                : specVersion == SpecVersion.V31
                    ? "3.1.0"
                    : "unknown";

        final var info = buildInfo(openApi.getInfo());

        final var servers = openApi.getServers().stream()
                .map(this::buildServer)
                .toList();


        final var schemas = new LinkedHashMap<String, OpenApiSchema<?>>();
        final Map<String, OpenApiSecurityScheme> securitySchemas = new HashMap<>();
        final var components = openApi.getComponents();

        if (components != null) {
            components.getSchemas().forEach((name, schema) -> {
                final var type = buildSchema(name, schema, openApi, schemas);
                schemas.put(name, type);
            });

            securitySchemas.putAll(buildSecuritySchemas(components.getSecuritySchemes()));
        }

        final var paths = buildPaths(openApi.getPaths(), openApi, schemas);

        final var securityRequirements = buildSecurityRequirements(openApi.getSecurity());

        return new OpenApi(
                version,
                info,
                servers,
                paths,
                securitySchemas,
                securityRequirements,
                new OpenApiComponents(schemas)
        );
    }

    private List<OpenApiSecurityRequirement> buildSecurityRequirements(final List<SecurityRequirement> security) {
        if (security == null) {
            return List.of();
        }

        return security.stream()
                .map(this::createSecurityRequirement)
                .toList();
    }

    private Map<String, OpenApiSecurityScheme> buildSecuritySchemas(final Map<String, SecurityScheme> securitySchemes) {
        final Map<String, OpenApiSecurityScheme> map = new HashMap<>();

        if (securitySchemes != null) {
            securitySchemes.forEach((name, scheme) -> {
                final var type = switch (scheme.getType()) {
                    case APIKEY -> OpenApiSecuritySchemeType.APIKEY;
                    case HTTP -> OpenApiSecuritySchemeType.HTTP;
                    case OAUTH2 -> OpenApiSecuritySchemeType.OAUTH2;
                    case OPENIDCONNECT -> OpenApiSecuritySchemeType.OPENIDCONNECT;
                    case MUTUALTLS -> OpenApiSecuritySchemeType.MUTUALTLS;
                };

                final var schemeName = scheme.getName() != null
                        ? scheme.getName()
                        : name;

                final var description = scheme.getDescription();

                final OpenApiSecuritySchemeIn in = scheme.getIn() != null
                        ? switch (scheme.getIn()) {
                    case COOKIE -> OpenApiSecuritySchemeIn.COOKIE;
                    case HEADER -> OpenApiSecuritySchemeIn.HEADER;
                    case QUERY -> OpenApiSecuritySchemeIn.QUERY;
                }
                        : null;

                final var schema = scheme.getScheme();
                final var bearerFormat = scheme.getBearerFormat();

                final var securityScheme = new OpenApiSecurityScheme(
                        type,
                        schemeName,
                        description,
                        "",
                        in,
                        schema,
                        bearerFormat
                );
                map.put(name, securityScheme);
            });
        }

        return map;
    }

    private Map<String, OpenApiSecurityScheme> buildSecuritySchemas(final List<SecurityRequirement> security) {
        return new HashMap<>();
    }

    private OpenApiServer buildServer(final Server server) {
        return new OpenApiServer(
                server.getUrl(),
                server.getDescription(),
                buildServerVariables(server.getVariables()),
                server.getExtensions()
        );
    }

    private Map<String, Object> buildServerVariables(final ServerVariables variables) {
        return variables != null
                ? new HashMap<>(variables)
                : Map.of();
    }

    private OpenApiInfo buildInfo(final Info info) {
        return new OpenApiInfo(
                info.getTitle(),
                info.getSummary(),
                info.getDescription(),
                info.getTermsOfService(),
                buildContact(info.getContact()),
                buildLicense(info.getLicense()),
                info.getVersion(),
                info.getExtensions()
        );
    }

    private OpenApiLicense buildLicense(final License license) {
        return license != null
                ? new OpenApiLicense(
                    license.getName(),
                    license.getIdentifier(),
                    license.getUrl(),
                    license.getExtensions()
        )
        : null;
    }

    private OpenApiContact buildContact(final Contact contact) {
        return contact != null ?
                new OpenApiContact(
                    contact.getName(),
                    contact.getUrl(),
                    contact.getEmail(),
                    contact.getExtensions()
                )
                : null;
    }

    private List<OpenApiPath> buildPaths(final Paths paths,
                                         final OpenAPI openAPI,
                                         final Map<String, OpenApiSchema<?>> schemaMap) {
        return paths.entrySet().stream()
                .map(pathEntry -> buildPath(pathEntry.getKey(), pathEntry.getValue(), openAPI, schemaMap))
                .toList();
    }

    private OpenApiPath buildPath(final String url,
                                  final PathItem path,
                                  final OpenAPI openAPI,
                                  final Map<String, OpenApiSchema<?>> schemaMap) {
        return new OpenApiPath(
                url,
                buildOperation(path.getPost(), openAPI, schemaMap),
                buildOperation(path.getGet(), openAPI, schemaMap),
                buildOperation(path.getPut(), openAPI, schemaMap),
                buildOperation(path.getPatch(), openAPI, schemaMap),
                buildOperation(path.getDelete(), openAPI, schemaMap),
                path.get$ref()
        );
    }

    private OpenApiOperation buildOperation(final Operation operation,
                                            final OpenAPI openAPI,
                                            final Map<String, OpenApiSchema<?>> schemaMap) {
        if (operation == null) {
            return null;
        }

        final List<OpenApiParameter> parameters = operation.getParameters() != null
                ? operation.getParameters().stream()
                    .map(parameter -> buildParameter(parameter, openAPI, schemaMap))
                    .toList()
                : List.of();

        final var requestBody = buildRequestBody(operation.getRequestBody(), openAPI, schemaMap);
        final var responses = buildResponses(operation.getResponses(), openAPI, schemaMap);

        final List<OpenApiSecurityRequirement> security = buildSecurityRequirements(operation.getSecurity());

        return new OpenApiOperation(
                operation.getSummary(),
                operation.getDescription(),
                operation.getOperationId(),
                nonNull(operation.getTags()),
                parameters,
                requestBody,
                responses,
                security
        );
    }

    private OpenApiSecurityRequirement createSecurityRequirement(final SecurityRequirement securityRequirement) {
        final var map = securityRequirement.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        it -> List.copyOf(it.getValue())
                ));
        return new OpenApiSecurityRequirement(map);
    }

    private Map<String, OpenApiResponse> buildResponses(final ApiResponses responses,
                                                        final OpenAPI openAPI,
                                                        final Map<String, OpenApiSchema<?>> schemaMap) {
        final var map = new LinkedHashMap<String, OpenApiResponse>();

        responses.forEach((code, response) -> map.put(code, buildResponse(response, openAPI, schemaMap)));

        return map;
    }

    private OpenApiResponse buildResponse(final ApiResponse response,
                                          final OpenAPI openAPI,
                                          final Map<String, OpenApiSchema<?>> schemaMap) {
        final var content = buildContent(response.getContent(), openAPI, schemaMap);
        final var headers = buildHeaders(response.getHeaders(), openAPI, schemaMap);
        return new OpenApiResponse(
                response.getDescription(),
                content,
                headers
        );
    }

    private Map<String, OpenApiHeader> buildHeaders(final Map<String, Header> headers,
                                                    final OpenAPI openAPI,
                                                    final Map<String, OpenApiSchema<?>> schemaMap) {
        final var map = new HashMap<String, OpenApiHeader>();

        if (headers != null) {
            headers.forEach((name, header) -> {
                final var headerSchema = header.getSchema();
                final OpenApiSchema<?> schema;

                if (headerSchema != null) {
                    final var schemaName = resolveSchemaName(headerSchema);
                    schema = buildSchema(schemaName, headerSchema, openAPI, schemaMap);
                } else {
                    schema = null;
                }

                final var openApiHeader = new OpenApiHeader(
                        header.getDescription(),
                        header.getRequired(),
                        header.getDeprecated(),
                        false,
                        header.getStyle().name(),
                        header.getExplode(),
                        false,
                        schema
                );
                map.put(name, openApiHeader);
            });
        }

        return map;
    }

    private OpenApiRequestBody buildRequestBody(final RequestBody requestBody,
                                                final OpenAPI openAPI,
                                                final Map<String, OpenApiSchema<?>> schemaMap) {
        if (requestBody == null) {
            return null;
        }

        return new OpenApiRequestBody(
                requestBody.getDescription(),
                buildContent(requestBody.getContent(), openAPI, schemaMap),
                requestBody.getRequired()
        );
    }

    private Map<String, OpenApiContent> buildContent(final Content content,
                                                     final OpenAPI openAPI,
                                                     final Map<String, OpenApiSchema<?>> schemaMap) {
        final var contentMap = new HashMap<String, OpenApiContent>();

        if (content != null) {
            content.forEach((key, value) -> contentMap.put(key, buildType(value, openAPI, schemaMap)));
        }

        return contentMap;
    }

    private OpenApiContent buildType(final MediaType value,
                                     final OpenAPI openAPI,
                                     final Map<String, OpenApiSchema<?>> schemaMap) {
        final var valueSchema = value.getSchema();

        if (valueSchema != null) {
            final var ref = valueSchema.get$ref();
            final var valueSchemaName = resolveSchemaName(valueSchema);
            final var schema = buildSchema(valueSchemaName, valueSchema, openAPI, schemaMap);

            final var examples = buildExamples(value.getExamples());

            return new OpenApiContent(
                    ref,
                    schema,
                    examples
            );
        } else {
            return new OpenApiContent(null, null, Map.of());
        }
    }

    private Map<String, OpenApiExample> buildExamples(final Map<String, Example> examples) {
        if (examples == null) {
            return Map.of();
        }

        return examples.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), processExample(entry.getValue())))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }

    private OpenApiExample processExample(final Example example) {
        return new OpenApiExample(
                example.getSummary(),
                example.getDescription(),
                example.getValue()
        );
    }

    private OpenApiParameter buildParameter(final Parameter parameter,
                                            final OpenAPI openAPI,
                                            final Map<String, OpenApiSchema<?>> schemaMap) {
        final var parameterSchema = parameter.getSchema();
        final var parameterSchemaName = resolveSchemaName(parameterSchema);
        final var type = buildSchema(parameterSchemaName, parameterSchema, openAPI, schemaMap);

        return new OpenApiParameter(
                ParameterLocation.parseIn(parameter.getIn()),
                type,
                parameter.getName(),
                parameter.getDescription(),
                parameter.getRequired(),
                parameter.getAllowEmptyValue(),
                parameter.getExplode(),
                parameterSchema.getNullable(),
                null
        );
    }

    private String getSchemaNameFromRef(final String ref) {
        return ref.substring("#/components/schemas/".length());
    }

    private OpenApiSchema<?> buildSchema(final String schemaName,
                                         final Schema<?> schema,
                                         final OpenAPI openAPI,
                                         final Map<String, OpenApiSchema<?>> schemaMap) {
        final var ref = schema.get$ref();

        if (ref != null) {
            final var componentSchemas = openAPI.getComponents().getSchemas();
            final var name = getSchemaNameFromRef(ref);
            final var componentSchema = componentSchemas.get(name);

            return buildSchema(name, componentSchema, openAPI, schemaMap);
        }

        final var openApiSchema = createOpenApiSchema(schema, schemaMap, openAPI);

        if (openApiSchema instanceof OpenApiObjectSchema openApiObjectSchema
                && !"object".equals(schemaName)) {
            return openApiObjectSchema
                    .withPackage(Package.of(modelPackageName))
                    .withName(schemaName);
        }

        return openApiSchema;
    }

    private OpenApiSchema<?> buildAdditionalProperties(final Object additionalProperties,
                                                       final OpenAPI openAPI,
                                                       final Map<String, OpenApiSchema<?>> schemaMap) {
        if (additionalProperties == null) {
            return null;
        }

        final var schema = (Schema<?>) additionalProperties;
        final var schemaName = resolveSchemaName(schema);
        return buildSchema(schemaName, schema, openAPI, schemaMap);
    }

    private OpenApiSchema<?> createOpenApiSchema(final Schema<?> schema,
                                                 final Map<String, OpenApiSchema<?>> schemaMap,
                                                 final OpenAPI openApi) {
        final var schemaBuilder = new OpenApiSchemaBuilder();
        fillConstraints(schema, schemaBuilder);

        final var properties = buildProperties(schema, schemaMap, openApi);
        final var additionalProperties = buildAdditionalProperties(schema.getAdditionalProperties(), openApi, schemaMap);

        schemaBuilder.format(schema.getFormat())
                .name(schema.getName())
                .nullable(schema.getNullable())
                .requiredProperties(schema.getRequired())
                .readOnly(schema.getReadOnly())
                .writeOnly(schema.getWriteOnly())
                .description(schema.getDescription())
                .extensions(schema.getExtensions())
                .properties(properties)
                .additionalProperties(additionalProperties);

        if (schema instanceof ArraySchema) {
            final var itemsSchema = createOpenApiSchema(schema.getItems(), schemaMap, openApi);
            schemaBuilder.itemsSchema(itemsSchema);
            return new OpenApiArraySchema(schemaBuilder);
        } else if (schema instanceof BinarySchema) {
            return new OpenApiBinarySchema(schemaBuilder);
        } else if (schema instanceof BooleanSchema) {
            return new OpenApiBooleanSchema(schemaBuilder);
        } else if (schema instanceof ByteArraySchema) {
            return new OpenApiByteArraySchema(schemaBuilder);
        } else if (schema instanceof ComposedSchema) {
            return new OpenApiCompositeSchema(schemaBuilder);
        } else if (schema instanceof DateSchema) {
            return new OpenApiDateSchema(schemaBuilder);
        } else if (schema instanceof DateTimeSchema) {
            return new OpenApiDateTimeSchema(schemaBuilder);
        } else if (schema instanceof EmailSchema) {
            return new OpenApiEmailSchema(schemaBuilder);
        } else if (schema instanceof FileSchema) {
            return new OpenApiFileSchema(schemaBuilder);
        } else if (schema instanceof IntegerSchema) {
            return new OpenApiIntegerSchema(schemaBuilder);
        } else if (schema instanceof JsonSchema) {
            return new OpenApiJsonSchema(schemaBuilder);
        } else if (schema instanceof MapSchema) {
            return new OpenApiMapSchema(schemaBuilder);
        } else if (schema instanceof NumberSchema) {
            return new OpenApiNumberSchema(schemaBuilder);
        } else if (schema instanceof ObjectSchema) {
            return new OpenApiObjectSchema(
                    schemaBuilder,
                    Package.UNNAMED,
                    "object"
            );
        } else if (schema instanceof PasswordSchema) {
            return new OpenApiPasswordSchema(schemaBuilder);
        } else if (schema instanceof StringSchema) {
            return new OpenApiStringSchema(schemaBuilder);
        } else if (schema instanceof UUIDSchema) {
            return new OpenApiUUIDSchema(schemaBuilder);
        } else {
            final var ref = schema.get$ref();

            if (ref != null) {
                final var schemaName = getSchemaNameFromRef(ref);
                return schemaMap.get(schemaName);
            }

            return new OpenApiSchema<>(schemaBuilder);
        }
    }

    private void fillConstraints(final Schema<?> schema,
                                 final OpenApiSchemaBuilder schemaBuilder) {
        schemaBuilder.minimum(schema.getMinimum());
        schemaBuilder.exclusiveMinimum(schema.getExclusiveMinimum());
        schemaBuilder.maximum(schema.getMaximum());
        schemaBuilder.exclusiveMaximum(schema.getExclusiveMaximum());
        schemaBuilder.minLength(schema.getMinLength());
        schemaBuilder.maxLength(schema.getMaxLength());
        schemaBuilder.pattern(schema.getPattern());
        schemaBuilder.minItems(schema.getMinItems());
        schemaBuilder.maxItems(schema.getMaxItems());
        schemaBuilder.uniqueItems(schema.getUniqueItems());
        final List<Object> enums = (List<Object>) schema.getEnum();
        schemaBuilder.enums(enums);
    }

    private <T> List<T> nonNull(final List<T> list) {
        return list != null
                ? list
                : List.of();
    }

    private Map<String, OpenApiSchema<?>> buildProperties(final Schema<?> schema,
                                                          final Map<String, OpenApiSchema<?>> schemaMap,
                                                          final OpenAPI openAPI) {
        if (schema.getProperties() == null) {
            return Map.of();
        }

        final var properties = new LinkedHashMap<String, OpenApiSchema<?>>();

        schema.getProperties().forEach((propertyName, propertySchema) ->
                properties.put(propertyName, createOpenApiSchema(propertySchema, schemaMap, openAPI)));

        if (schema.getAllOf() != null) {
            schema.getAllOf()
                    .forEach(otherSchema -> {
                        final var otherProperties = buildProperties(
                                otherSchema,
                                schemaMap,
                                openAPI
                        );

                        //Call put instead of putAll to maintain order
                        otherProperties.forEach(properties::put);
                    });
        }

        return properties;
    }

    private String resolveSchemaName(final Schema<?> schema) {
        final var ref = schema.get$ref();

        if (ref == null) {
            return "object";
        }

        final var start = ref.lastIndexOf("/") + 1;
        return ref.substring(start);
    }
}


