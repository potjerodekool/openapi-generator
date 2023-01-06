package io.github.potjerodekool.openapi.generate.config;

import io.github.potjerodekool.openapi.tree.OpenApi;

public interface ApiConfigGenerator {

    void generate(final OpenApi api);
}
