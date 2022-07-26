package com.github.potjerodekool.openapi;

import com.github.potjerodekool.openapi.generate.ConfigGenerator;
import com.github.potjerodekool.openapi.generate.api.DelegateGenerator;
import com.github.potjerodekool.openapi.generate.api.SpringApiDefinitionGenerator;
import com.github.potjerodekool.openapi.generate.api.SpringApiImplementationGenerator;
import com.github.potjerodekool.openapi.generate.model.ModelCodeGenerator;
import com.github.potjerodekool.openapi.util.GenerateException;

import static com.github.potjerodekool.openapi.util.Utils.requireNonNull;

public class Generator {

    public void generate(final OpenApiGeneratorConfig config) {
        final var apiFile = requireNonNull(config.getApiFile(), () -> new GenerateException("No api file specified"));
        final var rootDir = requireNonNull(apiFile.getParentFile(), () -> new GenerateException("Api file has no parent directory"));
        requireNonNull(config.getOutputDir(), () -> new GenerateException("No output directory specified"));

        final var openApi = OpenApiMerger.merge(apiFile);
        final var builder = new TreeBuilder(config);
        final var api = builder.build(openApi, rootDir);

        final Filer filer = new Filer(config);

        if (config.isGenerateModels()) {
            new ModelCodeGenerator(config, filer).generate(api);
        }

        if (config.isGenerateApiDefinitions()) {
            new SpringApiDefinitionGenerator(config, filer).generate(api);
        }

        if (config.isGenerateApiImplementations()) {
            new DelegateGenerator(config, filer).generate(api);
            new SpringApiImplementationGenerator(config, filer).generate(api);
        }

        new ConfigGenerator(config, filer).generate(api);
    }
}
