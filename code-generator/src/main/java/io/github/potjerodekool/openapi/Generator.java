package io.github.potjerodekool.openapi;

import io.github.potjerodekool.openapi.dependency.Artifact;
import io.github.potjerodekool.openapi.dependency.DependencyChecker;
import io.github.potjerodekool.openapi.generate.config.ConfigGenerator;
import io.github.potjerodekool.openapi.internal.Filer;
import io.github.potjerodekool.openapi.internal.OpenApiGeneratorConfigImpl;
import io.github.potjerodekool.openapi.internal.OpenApiParserHelper;
import io.github.potjerodekool.openapi.internal.TreeBuilder;
import io.github.potjerodekool.openapi.internal.ast.TypeUtils;
import io.github.potjerodekool.openapi.internal.di.ApplicationContext;
import io.github.potjerodekool.openapi.internal.di.ClassPathScanner;
import io.github.potjerodekool.openapi.internal.generate.*;
import io.github.potjerodekool.openapi.internal.generate.api.SpringApiDefinitionGenerator;
import io.github.potjerodekool.openapi.internal.generate.api.UtilsGenerator;
import io.github.potjerodekool.openapi.internal.generate.config.SpringJacksonConfigGenerator;
import io.github.potjerodekool.openapi.internal.generate.config.SpringOpenApiConfigGenerator;
import io.github.potjerodekool.openapi.internal.generate.model.ModelCodeGenerator;
import io.github.potjerodekool.openapi.internal.util.GenerateException;
import io.github.potjerodekool.openapi.internal.util.Utils;
import io.github.potjerodekool.openapi.tree.OpenApi;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import static io.github.potjerodekool.openapi.internal.util.Utils.requireNonNull;

public class Generator {

    public void generate(final OpenApiGeneratorConfig config,
                         final DependencyChecker dependencyChecker) {
        final var apiFile = toAbsoluteFile(requireNonNull(config.getApiFile(), () -> new GenerateException("No api file specified")));
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
        final var openApi = OpenApiParserHelper.merge(apiFile);
        final var builder = new TreeBuilder(config);
        final var api = builder.build(openApi, rootDir);

        final var typeUtils = createTypeUtils(dependencyChecker);
        final var filer = createFiler(config, typeUtils);
        final var generateUtils = new GenerateUtils(typeUtils);

        final var applicationContext = createApplicationContext(
                dependencyChecker,
                config,
                typeUtils
        );

        generateModels(
                api,
                config,
                applicationContext,
                typeUtils,
                filer
        );

        generateApiDefinitions(
                api,
                config,
                typeUtils,
                generateUtils,
                filer
        );

        generateUtils(
                api,
                config,
                typeUtils,
                filer
        );

        generateConfigs(
                api,
                config,
                applicationContext,
                typeUtils,
                dependencyChecker,
                filer
        );
    }

    private TypeUtils createTypeUtils(final DependencyChecker dependencyChecker) {
        final var classLoader = createClassLoader(dependencyChecker.getProjectArtifacts().toList());
        return new TypeUtils(classLoader);
    }

    private Filer createFiler(final OpenApiGeneratorConfig config,
                              final TypeUtils typeUtils) {
        return new Filer(config, typeUtils);
    }

    public void generateExternalApi(final OpenApiGeneratorConfig config,
                                    final DependencyChecker dependencyChecker) {
        final var apiFile = toAbsoluteFile(requireNonNull(config.getApiFile(), () -> new GenerateException("No api file specified")));
        final var rootDir = requireNonNull(apiFile.getParentFile(), () -> new GenerateException("Api file has no parent directory"));
        requireNonNull(config.getOutputDir(), () -> new GenerateException("No output directory specified"));
        if (Utils.isNullOrEmpty(config.getModelPackageName())) {
            throw new GenerateException("No model package name specified");
        }
        doGenerateExternalApi(apiFile, rootDir, config, dependencyChecker);
    }

    private File toAbsoluteFile(final File file) {
        return file.getAbsoluteFile();
    }

    private void doGenerateExternalApi(final File apiFile,
                                       final File rootDir,
                                       final OpenApiGeneratorConfig config,
                                       final DependencyChecker dependencyChecker) {
        final var openApi = OpenApiParserHelper.parse(apiFile);
        final var builder = new TreeBuilder(config);
        final var api = builder.build(openApi, rootDir);

        final var typeUtils = createTypeUtils(dependencyChecker);
        final var filer = createFiler(config, typeUtils);

        final var applicationContext = createApplicationContext(
                dependencyChecker,
                config,
                typeUtils
        );

        generateModels(
                api,
                config,
                applicationContext,
                typeUtils,
                filer
        );
    }

    private void generateModels(final OpenApi api,
                                final OpenApiGeneratorConfig config,
                                final ApplicationContext applicationContext,
                                final TypeUtils typeUtils,
                                final Filer filer) {
        final var generateModels = config.isGenerateModels();

        if (generateModels) {
            new ModelCodeGenerator(
                    filer,
                    typeUtils,
                    applicationContext,
                    config.getLanguage()
            ).generate(api);
        }
    }

    private void generateApiDefinitions(final OpenApi api,
                                        final OpenApiGeneratorConfig config,
                                        final TypeUtils typeUtils,
                                        final GenerateUtils generateUtils2,
                                        final Filer filer) {
        if (config.isGenerateApiDefinitions()) {
            new SpringApiDefinitionGenerator(config, typeUtils, generateUtils2, filer).generate(api);
        }
    }

    private void generateUtils(final OpenApi api,
                               final OpenApiGeneratorConfig config,
                               final TypeUtils typeUtils,
                               final Filer filer) {
        new UtilsGenerator(
                config,
                filer,
                typeUtils
        ).generate(api);
    }

    private void generateConfigs(final OpenApi api,
                                 final OpenApiGeneratorConfig config,
                                 final ApplicationContext applicationContext,
                                 final TypeUtils typeUtils,
                                 final DependencyChecker dependencyChecker,
                                 final Filer filer) {
        new SpringOpenApiConfigGenerator(config, typeUtils, filer).generate(api);

        new SpringJacksonConfigGenerator(
                config,
                typeUtils,
                filer,
                dependencyChecker
        ).generate(api);

        final var configGenerators = applicationContext.getBeansOfType(ConfigGenerator.class);
        configGenerators.forEach(configGenerator -> configGenerator.generate(api));
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

        return configBuilder.build(config.getConfigType());
    }

    private ApplicationContext createApplicationContext(final DependencyChecker dependencyChecker,
                                                        final OpenApiGeneratorConfig config,
                                                        final TypeUtils typeUtils) {
        final ApplicationContext context = new ApplicationContext(dependencyChecker);
        context.registerBean(OpenApiGeneratorConfig.class, config);
        context.registerBean(DependencyChecker.class, dependencyChecker);
        context.registerBean(TypeUtils.class, typeUtils);

        final var beanDefinitions = ClassPathScanner.scan();
        context.registerBeans(beanDefinitions);
        return context;
    }

    private ClassLoader createClassLoader(final List<Artifact> projectArtifacts) {
        final var urls = new ArrayList<URL>();

        projectArtifacts.forEach(artifact -> {
            final var file = artifact.file();
            if (file != null) {
                urls.add(toURL(file));
            }
        });

        return new URLClassLoader(
                urls.toArray(URL[]::new),
                null
        );
    }

    private URL toURL(final File file) {
        try {
            return file.toURI().toURL();
        } catch (final MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
