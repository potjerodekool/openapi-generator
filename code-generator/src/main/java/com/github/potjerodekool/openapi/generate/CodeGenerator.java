package com.github.potjerodekool.openapi.generate;

import com.github.potjerodekool.openapi.tree.OpenApi;

import java.io.IOException;

public interface CodeGenerator {

    void generate(OpenApi api) throws IOException;
}
