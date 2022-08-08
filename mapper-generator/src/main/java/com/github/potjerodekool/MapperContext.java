package com.github.potjerodekool;

public class MapperContext {

    private final ObjectMapper objectMapper;

    public MapperContext(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
