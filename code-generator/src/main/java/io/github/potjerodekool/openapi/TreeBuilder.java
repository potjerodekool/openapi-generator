package io.github.potjerodekool.openapi;

import io.github.potjerodekool.openapi.type.OpenApiType;
import io.github.potjerodekool.openapi.util.GenerateException;

import io.github.potjerodekool.openapi.util.Utils;
import com.reprezen.kaizen.oasparser.model3.*;
import com.reprezen.kaizen.oasparser.ovl3.PathImpl;
import com.reprezen.kaizen.oasparser.ovl3.SchemaImpl;
import io.github.potjerodekool.openapi.tree.*;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static io.github.potjerodekool.openapi.util.Utils.requireNonNull;

/**
 A builder that creates a tree from an OpenApi object.
 */
public class TreeBuilder {
    private final File schemaDir;

    private final SchemaToTypeConverter schemaToTypeConverter;

    public TreeBuilder(final OpenApiGeneratorConfig config) {
        this.schemaDir = requireNonNull(config.getSchemasDir());
        this.schemaToTypeConverter = new SchemaToTypeConverter(config);
    }

    public OpenApi build(final OpenApi3 openApi,
                         final File rootDir) {
        final var paths = openApi.getPaths().entrySet().stream()
                .map(entry -> processPath(entry.getKey(), entry.getValue(), rootDir))
                .toList();

        return new OpenApi(
                createInfo(openApi.getInfo()),
                paths
        );
    }

    private OpenApiInfo createInfo(final Info info) {
        return new OpenApiInfo(
                info.getTitle(),
                info.getDescription(),
                info.getTermsOfService(),
                createContact(info.getContact()),
                createLicense(info.getLicense()),
                info.getVersion(),
                info.getExtensions()
        );
    }

    private OpenApiLicense createLicense(final License license) {
        return new OpenApiLicense(
                license.getName(),
                license.getUrl(),
                license.getExtensions()
        );
    }

    private OpenApiContact createContact(final Contact contact) {
        return new OpenApiContact(
                contact.getName(),
                contact.getUrl(),
                contact.getEmail(),
                contact.getExtensions()
        );
    }

    private OpenApiPath processPath(final String pathStr,
                                    final Path path,
                                    final File rootDir) {
        final var post = processOperation(HttpMethod.POST, pathStr, path.getPost(), rootDir);
        final var get = processOperation(HttpMethod.GET, pathStr, path.getGet(), rootDir);
        final var put = processOperation(HttpMethod.PUT, pathStr, path.getPut(), rootDir);
        final var patch = processOperation(HttpMethod.PATCH, pathStr, path.getPatch(), rootDir);
        final var delete = processOperation(HttpMethod.DELETE, pathStr, path.getDelete(), rootDir);

        final var creatingRef = ((PathImpl)path)._getCreatingRef();

        return new OpenApiPath(
                pathStr,
                post,
                get,
                put,
                patch,
                delete,
                creatingRef.getNormalizedRef()
        );
    }

    private @Nullable OpenApiOperation processOperation(final HttpMethod httpMethod,
                                                        final String pathStr,
                                                        final Operation operation,
                                                        final File rootDir) {
        if (operation == null) {
            return null;
        }

        final var parameters = operation.getParameters().stream()
                .map(parameter -> processParameter(httpMethod, parameter, rootDir))
                .toList();

        final var requestBody = processRequestBody(operation.getRequestBody(), rootDir, httpMethod);

        final var responses = operation.getResponses().entrySet().stream()
                .map(entry -> Map.entry(
                        entry.getKey(),
                        processResponse(entry.getValue(), rootDir, httpMethod)
                ))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));



        return new OpenApiOperation(
                operation.getSummary(),
                operation.getDescription(),
                generateOperationId(httpMethod, pathStr, operation.getOperationId()),
                operation.getTags(),
                parameters,
                requestBody,
                responses
        );
    }

    private OpenApiParameter processParameter(final HttpMethod httpMethod,
                                              final Parameter parameter,
                                              final File rootDir) {

        final var parameterType = processSchema(
                parameter.getSchema(),
                rootDir,
                new SchemaContext(
                        httpMethod,
                        ParameterLocation.parseIn(parameter.getIn())
                )
        );

        return new OpenApiParameter(
                ParameterLocation.parseIn(parameter.getIn()),
                parameter.getName(),
                parameter.getRequired(),
                parameter.getAllowEmptyValue(),
                parameterType,
                null,
                parameter.getDescription()
        );
    }

    private String generateOperationId(final HttpMethod httpMethod,
                                       final String pathString,
                                       final @Nullable String operationId) {
        if (operationId == null || operationId.isEmpty()) {
            final var prefix = switch (httpMethod) {
                case POST -> "post";
                case GET -> "get";
                case PUT -> "put";
                case PATCH -> "path";
                case DELETE -> "delete";
            };

            final String value = Arrays.stream(pathString.split("/"))
                    .filter(it -> !it.isEmpty())
                    .map(Utils::firstUpper)
                    .collect(Collectors.joining());
            return prefix + value;
        } else {
            return operationId;
        }
    }

    private @Nullable OpenApiRequestBody processRequestBody(final @Nullable RequestBody requestBody,
                                                            final File rootDir,
                                                            final HttpMethod httpMethod) {
        if (requestBody == null) {
            return null;
        }

        final var contentMediaType = processContentMediaTypes(
                requestBody.getContentMediaTypes(),
                rootDir,
                RequestCycleLocation.REQUEST,
                httpMethod);

        if (contentMediaType.isEmpty()) {
            return null;
        }

        return new OpenApiRequestBody(
                requestBody.getDescription(),
                contentMediaType,
                requestBody.getRequired()
        );
    }

    private OpenApiResponse processResponse(final Response response,
                                            final File rootDir,
                                            final HttpMethod httpMethod) {
        final var contentMediaType = processContentMediaTypes(
                response.getContentMediaTypes(),
                rootDir,
                RequestCycleLocation.RESPONSE,
                httpMethod);


        final var headers = response.getHeaders().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        it -> createHeader(it.getValue())
                ));


        return new OpenApiResponse(
                response.getDescription(),
                contentMediaType,
                headers
        );
    }

    private OpenApiHeader createHeader(final Header header) {
        final var schema = header.getSchema();
        final var headerType = schemaToTypeConverter.build(
                schema.getType(),
                schema.getFormat(),
                schema.getNullable()
        );

        return new OpenApiHeader(
                header.getDescription(),
                header.getRequired(),
                header.getDeprecated(),
                header.getAllowEmptyValue(),
                header.getStyle(),
                header.getExplode(),
                header.getAllowReserved(),
                headerType
        );
    }

    private Map<String, OpenApiType> processContentMediaTypes(final Map<String, MediaType> mediaTypeMap,
                                                              final File rootDir,
                                                              final RequestCycleLocation requestCycleLocation,
                                                              final HttpMethod httpMethod) {
        return mediaTypeMap.entrySet()
                .stream().map(entry -> Map.entry(entry.getKey(),
                        processContentMediaType(
                            entry.getValue(),
                                rootDir,
                                requestCycleLocation,
                                httpMethod)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private OpenApiType processContentMediaType(final MediaType mediaType,
                                                final File rootDir,
                                                final RequestCycleLocation requestCycleLocation,
                                                final HttpMethod httpMethod) {
        return processSchema(mediaType.getSchema(), rootDir, new SchemaContext(
                httpMethod,
                requestCycleLocation
        ));
    }

    private OpenApiType processSchema(final Schema schema,
                                      final File rootDir,
                                      final SchemaContext schemaContext) {
        if (schema == null) {
            throw new NullPointerException();
        }

        final var schemaImpl = (SchemaImpl) schema;
        final var createRef = schemaImpl._getCreatingRef();

        if (createRef == null) {
            return schemaToTypeConverter.build(
                    schema.getType(),
                    schema.getFormat(),
                    schema.getNullable()
            );
        }

        final var refString = createRef.getNormalizedRef();

        final var absoluteSchemaUri = schemaDir.toURI().toString();

        if (!refString.startsWith(absoluteSchemaUri)) {
            throw new GenerateException(refString + " doesn't start with " + absoluteSchemaUri);
        }

        return schemaToTypeConverter.build(schema, rootDir, schemaContext);
    }

}
