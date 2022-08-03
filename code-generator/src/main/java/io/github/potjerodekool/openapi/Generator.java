package io.github.potjerodekool.openapi;

import io.github.potjerodekool.openapi.generate.JavaTypes;
import io.github.potjerodekool.openapi.generate.api.UtilsGenerator;
import io.github.potjerodekool.openapi.generate.config.SpringJacksonConfigGenerator;
import io.github.potjerodekool.openapi.generate.config.SpringOpenApiConfigGenerator;
import io.github.potjerodekool.openapi.generate.api.SpringApiDefinitionGenerator;
import io.github.potjerodekool.openapi.generate.model.ModelCodeGenerator;
import io.github.potjerodekool.openapi.util.GenerateException;
import io.github.potjerodekool.openapi.util.Utils;

import static io.github.potjerodekool.openapi.util.Utils.requireNonNull;

public class Generator {

    public void generate(final OpenApiGeneratorConfig config,
                         final DependencyChecker dependencyChecker) {
        final var apiFile = requireNonNull(config.getApiFile(), () -> new GenerateException("No api file specified"));
        final var rootDir = requireNonNull(apiFile.getParentFile(), () -> new GenerateException("Api file has no parent directory"));
        requireNonNull(config.getOutputDir(), () -> new GenerateException("No output directory specified"));
        if (Utils.isNullOrEmpty(config.getConfigPackageName())) {
            throw new GenerateException("No config package name specified");
        }

        final var openApi = OpenApiMerger.merge(apiFile);
        final var builder = new TreeBuilder(config);
        final var api = builder.build(openApi, rootDir);

        final var filer = new Filer(config);
        final var types = new JavaTypes();

        if (config.isGenerateModels()) {
            new ModelCodeGenerator(config, types, filer).generate(api);
        }

        if (config.isGenerateApiDefinitions()) {
            new SpringApiDefinitionGenerator(config, types, filer).generate(api);
        }

        new UtilsGenerator(config, types, filer).generate(api);

        new SpringOpenApiConfigGenerator(config, types, filer).generate(api);

        new SpringJacksonConfigGenerator(
                config,
                types,
                filer,
                dependencyChecker
        ).generate(api);
    }
}
