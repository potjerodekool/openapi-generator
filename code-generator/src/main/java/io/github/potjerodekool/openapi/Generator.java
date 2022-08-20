package io.github.potjerodekool.openapi;

import io.github.potjerodekool.openapi.dependency.DependencyChecker;
import io.github.potjerodekool.openapi.internal.Filer;
import io.github.potjerodekool.openapi.internal.OpenApiGeneratorConfigImpl;
import io.github.potjerodekool.openapi.internal.OpenApiMerger;
import io.github.potjerodekool.openapi.internal.TreeBuilder;
import io.github.potjerodekool.openapi.internal.generate.GenerateUtilsJava;
import io.github.potjerodekool.openapi.internal.generate.TypesJava;
import io.github.potjerodekool.openapi.internal.generate.api.UtilsGenerator;
import io.github.potjerodekool.openapi.internal.generate.config.SpringJacksonConfigGenerator;
import io.github.potjerodekool.openapi.internal.generate.config.SpringOpenApiConfigGenerator;
import io.github.potjerodekool.openapi.internal.generate.api.SpringApiDefinitionGenerator;
import io.github.potjerodekool.openapi.internal.generate.model.ModelCodeGenerator;
import io.github.potjerodekool.openapi.internal.util.GenerateException;
import io.github.potjerodekool.openapi.internal.util.Utils;

import java.io.File;

import static io.github.potjerodekool.openapi.internal.util.Utils.requireNonNull;

public class Generator {

    public void generate(final OpenApiGeneratorConfig config,
                         final DependencyChecker dependencyChecker) {
        final var apiFile = requireNonNull(config.getApiFile(), () -> new GenerateException("No api file specified"));
        final var rootDir = requireNonNull(apiFile.getParentFile(), () -> new GenerateException("Api file has no parent directory"));
        requireNonNull(config.getOutputDir(), () -> new GenerateException("No output directory specified"));
        if (Utils.isNullOrEmpty(config.getConfigPackageName())) {
            throw new GenerateException("No config package name specified");
        }

        doGenerate(
                apiFile,
                rootDir,
                checkFeatures(config, dependencyChecker),
                dependencyChecker
        );
    }

    private void doGenerate(final File apiFile,
                            final File rootDir,
                            final OpenApiGeneratorConfig config,
                            final DependencyChecker dependencyChecker) {
        final var openApi = OpenApiMerger.merge(apiFile);
        final var builder = new TreeBuilder(config);
        final var api = builder.build(openApi, rootDir);

        final var filer = new Filer(config);
        final var types = new TypesJava();
        final var generateUtils = new GenerateUtilsJava(types);

        if (config.isGenerateModels()) {
            new ModelCodeGenerator(config, types, dependencyChecker, generateUtils, filer).generate(api);
        }

        if (config.isGenerateApiDefinitions()) {
            new SpringApiDefinitionGenerator(config, types, generateUtils, filer).generate(api);
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

    private OpenApiGeneratorConfig checkFeatures(final OpenApiGeneratorConfig config,
                                                 final DependencyChecker dependencyChecker) {
        final var configBuilder = config.toBuilder();

        if (config.getFeatureValue(OpenApiGeneratorConfig.FEATURE_JAKARTA_SERVLET) == null) {
            if (dependencyChecker.isDependencyPresent("jakarta.servlet", "jakarta.servlet-api")) {
                configBuilder.featureValue(OpenApiGeneratorConfig.FEATURE_JAKARTA_SERVLET, true);
            }
        }

        if (config.getFeatureValue(OpenApiGeneratorConfigImpl.FEATURE_JAKARTA_VALIDATION) == null) {
            if (dependencyChecker.isDependencyPresent("jakarta.validation", "jakarta.validation-api")) {
                configBuilder.featureValue(OpenApiGeneratorConfig.FEATURE_JAKARTA_VALIDATION, true);
            }
        }

        if (config.getFeatureValue(OpenApiGeneratorConfigImpl.FEATURE_CHECKER) == null) {
            if (dependencyChecker.isDependencyPresent("org.checkerframework", "checker-qual")) {
                configBuilder.featureValue(OpenApiGeneratorConfig.FEATURE_CHECKER, true);
            }
        }

        return configBuilder.build();
    }
}
