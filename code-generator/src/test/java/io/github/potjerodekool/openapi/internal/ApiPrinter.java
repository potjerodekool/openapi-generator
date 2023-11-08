package io.github.potjerodekool.openapi.internal;

import io.github.potjerodekool.openapi.tree.*;
import io.github.potjerodekool.openapi.tree.media.*;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ApiPrinter {

    private int tabs = 0;

    private final Writer writer;

    public ApiPrinter(final Writer writer) {
        this.writer = writer;
    }

    public void print(final OpenApi api) {
        printLn("openapi: \"" + api.version() + "\"");
        printInfo(api.info());
        printServers(api.servers());
        printPaths(api.paths());
    }

    private void printInfo(final OpenApiInfo info) {
        printLn("info:");
        indent();

        printIdent();
        printLn("version: " + info.version());

        printIdent();
        printLn("title: " + info.title());

        printIdent();
        printLn("license:");

        indent();
        printIdent();
        printLn("name: " + info.license().name());

        deIndent();
        deIndent();
    }

    private void printServers(final List<OpenApiServer> servers) {
        printLn("servers:");
        indent();

        servers.forEach(server -> {
            printIdent();
            printLn("- url: " + server.url());
        });
        deIndent();
    }


    private void printPaths(final List<OpenApiPath> paths) {
        printLn("paths:");
        indent();
        paths.forEach(this::printPath);
        deIndent();
    }

    private void printPath(final OpenApiPath path) {
        printIdent();
        printLn(path.path() + ":");
        printOperation("post", path.post());
        printOperation("get", path.get());
        printOperation("put", path.put());
        printOperation("patch", path.patch());
        printOperation("delete", path.delete());
    }

    private void printOperation(final String operationName,
                                final @Nullable OpenApiOperation operation) {
        if (operation != null) {
            indent();

            printIdent();
            printLn(operationName + ":");

            indent();

            printIdent();
            printLn("summary: " + operation.summary());

            printIdent();
            printLn("operationId: " + operation.operationId());

            if (notEmpty(operation.tags())) {
                printIdent();
                printLn("tags:");
                indent();

                operation.tags().forEach(tag -> {
                    printIdent();
                    printLn("- " + tag);
                });

                deIndent();
            }

            final var securityRequirements = operation.securityRequirements();

            if (notEmpty(securityRequirements)) {
                printIdent();
                printLn("security:");
                indent();
                securityRequirements.forEach(securityRequirement -> {
                    printIdent();
                    printLn("- " + securityRequirement.requirements());
                });
                deIndent();
            }

            if (notEmpty(operation.parameters())) {
                printIdent();
                printLn("parameters:");
                indent();

                operation.parameters().forEach(openApiParameter -> {
                    printIdent();
                    printLn("- name: " + openApiParameter.name());
                    indent();

                    printIdent();
                    printLn("in: " + openApiParameter.in().value());

                    printIdent();
                    printLn("description: " + openApiParameter.description());

                    printIdent();
                    printLn("required: " + openApiParameter.required());

                    printSchema(openApiParameter.type());

                    deIndent();
                });

                deIndent();
            }

            final var requestBody = operation.requestBody();

            if (requestBody != null) {
                printIdent();
                printLn("requestBody:");
                indent();
                printContent(requestBody.contentMediaType());
                deIndent();
            }

            final var responses = operation.responses();

            if (responses != null
                    && !responses.isEmpty()) {
                printIdent();
                printLn("responses:");
                indent();

                responses.forEach((code, response) -> {
                    printIdent();

                    if (isNummeric(code)) {
                        printLn("'" + code + "':");
                    } else {
                        printLn(code + ":");
                    }

                    indent();
                    printIdent();
                    printLn("description: '" + response.description() + "'");

                    if (notEmpty(response.headers())) {
                        printIdent();
                        printLn("headers:");
                        indent();

                        response.headers().forEach((name, header) -> {
                           printIdent();
                           printLn(name + ":");
                           indent();
                            printIdent();
                            printLn("description: '" + header.description() + "'");
                            printSchema(header.schema());
                           deIndent();

                        });
                        deIndent();
                    }

                    printContent(response.contentMediaType());
                    deIndent();
                });

                deIndent();
            }

            deIndent();
            deIndent();
        }
    }

    private boolean isNummeric(final String value) {
        return value.chars()
                .allMatch(Character::isDigit);
    }

    private void printSchema(final OpenApiSchema<?> schema) {
        if (schema == null) {
            return;
        }

        printIdent();
        printLn("schema:");
        indent();

        printIdent();
        printLn("schema: " + resolveSchemaType(schema));

        if (schema.format() != null) {
            printIdent();
            printLn("format: " + schema.format());
        }

        if (schema.nullable() != null) {
            printIdent();
            printLn("nullable: " + schema.nullable());
        }

        deIndent();
    }

    private String resolveSchemaType(final OpenApiSchema<?> schema) {
        if (schema instanceof OpenApiStringSchema) {
            return "string";
        } else if (schema instanceof OpenApiIntegerSchema) {
            return "integer";
        } else if (schema instanceof OpenApiBooleanSchema) {
            return "noolean";
        } else if (schema instanceof OpenApiNumberSchema) {
            return "number";
        } else {
            return null;
        }
    }

    private void printContent(final Map<String, OpenApiContent> contentMediaType) {
        if (contentMediaType.isEmpty()) {
            return;
        }

        printIdent();
        printLn("content:");
        indent();
        contentMediaType.forEach((contentType, content) -> {
            printIdent();

            final var type = contentType.startsWith("*")
                    ? "'" + contentType + "'"
                    : contentType;

            printLn(type + ":");
            indent();

            if (content.ref() != null) {
                printIdent();
                printLn("schema:");
                indent();

                printIdent();
                printLn("$ref: '" + content.ref() + "'");
            } else {
                printSchema(content.schema());
            }
            deIndent();
            deIndent();
        });
        deIndent();
    }

    private void print(final Object s) {
        try {
            writer.write(s.toString());
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void printLn(final Object s) {
        print(s);
        print("\n");
    }

    public void indent() {
        tabs++;
    }

    public void deIndent() {
        tabs--;
    }

    private void printIdent() {
        for (int i = 0; i < tabs; i++) {
            print("  ");
        }
    }

    public void flush() {
        try {
            writer.flush();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private <E> boolean notEmpty(final Collection<E> collection) {
        return collection != null
                && !collection.isEmpty();
    }

    private boolean notEmpty(final Map<?, ?> map) {
        return map != null
                && !map.isEmpty();
    }
}
