package io.github.potjerodekool.openapi.generate.config;

import io.github.potjerodekool.openapi.tree.OpenApi;

public interface ConfigGenerator {

    void generate(final OpenApi api);
}
