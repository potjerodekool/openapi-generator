package io.github.potjerodekool.openapi.common

import io.swagger.models.HttpMethod

class MissingOperationIdException(path: String, httpMethod: HttpMethod) :
    RuntimeException(String.format("Missing operationId for %s %s", httpMethod.name, path))
