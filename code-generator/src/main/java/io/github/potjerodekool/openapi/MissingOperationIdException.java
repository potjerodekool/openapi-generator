package io.github.potjerodekool.openapi;

import io.swagger.models.HttpMethod;

public class MissingOperationIdException extends RuntimeException {
    public MissingOperationIdException(final String path, final HttpMethod httpMethod) {
        super(String.format("Missing operationId for %s %s", httpMethod.name(), path));
    }
}
