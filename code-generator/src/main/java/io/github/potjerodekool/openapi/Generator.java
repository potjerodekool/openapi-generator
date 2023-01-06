package io.github.potjerodekool.openapi;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.potjerodekool.openapi.dependency.Artifact;
import io.github.potjerodekool.openapi.dependency.DependencyChecker;
import io.github.potjerodekool.openapi.generate.config.ApiConfigGenerator;
import io.github.potjerodekool.openapi.generate.config.ConfigGenerator;
import io.github.potjerodekool.openapi.internal.*;
import io.github.potjerodekool.openapi.internal.ast.util.TypeUtils;
import io.github.potjerodekool.openapi.internal.di.ApplicationContext;
import io.github.potjerodekool.openapi.internal.di.ClassPathScanner;
import io.github.potjerodekool.openapi.internal.generate.*;
import io.github.potjerodekool.openapi.internal.generate.api.SpringApiDefinitionGenerator;
import io.github.potjerodekool.openapi.internal.generate.api.UtilsGenerator;
import io.github.potjerodekool.openapi.internal.generate.config.SpringApplicationConfigGenerator;
import io.github.potjerodekool.openapi.internal.generate.config.SpringJacksonConfigGenerator;
import io.github.potjerodekool.openapi.internal.generate.config.SpringOpenApiConfigGenerator;
import io.github.potjerodekool.openapi.internal.generate.model.ModelCodeGenerator;
import io.github.potjerodekool.openapi.internal.util.GenerateException;
import io.github.potjerodekool.openapi.tree.OpenApi;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class Generator {

    public void generate(final Project project,
                         final List<ApiConfiguration> apiConfigurations,
                         final Map<String, Boolean> features,
                         final String basePackageName,
                         final Language language) {
        generateCommon(project, basePackageName, language, features);

        final var standardApiConfiguration = apiConfigurations.stream()
                .filter(ApiConfiguration::generateApiDefinitions)
                .findFirst()
                .orElse(null);

        apiConfigurations.forEach(apiConfiguration -> {
            final var generateConfig = apiConfiguration == standardApiConfiguration;
            generateApi(project, apiConfiguration, features, basePackageName, language, generateConfig);
        });
    }

    private void generateCommon(final Project project,
                                final String basePackageName,
                                final Language language,
                                final Map<String, Boolean> features) {
        final var generatorConfig = createGeneratorConfig(
                language,
                basePackageName,
                resolveFeatures(project, features)
        );

        final var typeUtils = createTypeUtils(project.dependencyChecker());
        final var filer = createFiler(typeUtils, project);

        generateUtils(
                generatorConfig,
                typeUtils,
                filer
        );

        final var applicationContext = createApplicationContext(
                project.dependencyChecker(),
                generatorConfig,
                typeUtils
        );

        generateConfigs(
                generatorConfig,
                applicationContext,
                typeUtils,
                project.dependencyChecker(),
                filer
        );

        final var additionalApplicationProperties = new HashMap<String, Object>();

        generateSpringConfig(additionalApplicationProperties, filer);
    }

    private void generateApi(final Project project,
                             final ApiConfiguration apiConfiguration,
                             final Map<String, Boolean> features,
                             final String basePackageName,
                             final Language language,
                             final boolean generateConfigs) {
        final var apiFile = apiConfiguration.apiFile().getAbsoluteFile();
        final var rootDir = apiFile.getParentFile();

        if (rootDir == null) {
            throw new GenerateException("Api file has no parent directory");
        }

        final var configurationFile = loadConfigFor(apiFile);

        final var controllers = configurationFile.getControllers();
        final var resolvedFeatures = resolveFeatures(project, features);

        final var generatorConfig = createGeneratorConfig(
                language,
                basePackageName,
                resolvedFeatures
        );

        generateApi(
                project,
                apiFile,
                rootDir,
                generatorConfig,
                apiConfiguration.withControllers(controllers),
                project.dependencyChecker(),
                generateConfigs
        );
    }

    private Map<String, Boolean> resolveFeatures(final Project project,
                                                 final Map<String, Boolean> features) {
        return checkFeatures(features, project.dependencyChecker());
    }

    private GeneratorConfig createGeneratorConfig(final Language language,
                                                  final String basePackageName,
                                                  final Map<String, Boolean> resolvedFeatures) {
        return new GeneratorConfig(
                language,
                basePackageName,
                resolvedFeatures
        );
    }

    private ConfigFile loadConfigFor(final File apiFile) {
        //spec.yaml
        final var rootDir = apiFile.getParentFile();
        final var apiFileName = apiFile.getName();
        final var separatorIndex = apiFileName.lastIndexOf('.');
        final var file = new File(rootDir,apiFileName.substring(0, separatorIndex) + "-config.json");

        if (file.exists()) {
            final var objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            try {
                return objectMapper.readValue(file, ConfigFile.class);
            } catch (final IOException e) {
                return new ConfigFile();
            }
        } else {
            return new ConfigFile();
        }
    }

    private void generateApi(final Project project,
                             final File apiFile,
                             final File rootDir,
                             final GeneratorConfig generatorConfig,
                             final ApiConfiguration apiConfiguration,
                             final DependencyChecker dependencyChecker,
                             final boolean generateConfigs) {
        final var openApi = OpenApiParserHelper.merge(apiFile);
        final var builder = new TreeBuilder(apiConfiguration);
        final var api = builder.build(openApi, rootDir);

        final var typeUtils = createTypeUtils(dependencyChecker);
        final var filer = createFiler(typeUtils, project);
        final var generateUtils = new GenerateUtils(typeUtils);

        final var applicationContext = createApplicationContext(
                dependencyChecker,
                generatorConfig,
                typeUtils
        );

        generateModels(
                api,
                apiConfiguration,
                generatorConfig,
                applicationContext,
                typeUtils,
                filer
        );

        generateApiDefinitions(
                api,
                apiConfiguration,
                generatorConfig,
                typeUtils,
                generateUtils,
                filer
        );

        final var additionalApplicationProperties = new HashMap<String, Object>();

        generateApiConfigs(
                api,
                generatorConfig,
                applicationContext,
                typeUtils,
                filer,
                generateConfigs
        );

        generateSpringConfig(additionalApplicationProperties, filer);
    }

    private TypeUtils createTypeUtils(final DependencyChecker dependencyChecker) {
        final var classLoader = createClassLoader(dependencyChecker.getProjectArtifacts().toList());
        return new TypeUtils(classLoader);
    }

    private Filer createFiler(final TypeUtils typeUtils,
                              final Project project) {
        return new Filer(typeUtils, project);
    }

    private void generateModels(final OpenApi api,
                                final ApiConfiguration apiConfiguration,
                                final GeneratorConfig generatorConfig,
                                final ApplicationContext applicationContext,
                                final TypeUtils typeUtils,
                                final Filer filer) {
        final var generateModels = apiConfiguration.generateModels();

        if (generateModels) {
            new ModelCodeGenerator(
                    filer,
                    typeUtils,
                    applicationContext,
                    generatorConfig.language()
            ).generate(api);
        }
    }

    private void generateApiDefinitions(final OpenApi api,
                                        final ApiConfiguration apiConfiguration,
                                        final GeneratorConfig generatorConfig,
                                        final TypeUtils typeUtils,
                                        final GenerateUtils generateUtils,
                                        final Filer filer) {
        if (apiConfiguration.generateApiDefinitions()) {
            new SpringApiDefinitionGenerator(
                    generatorConfig,
                    apiConfiguration,
                    typeUtils,
                    generateUtils,
                    filer).generate(api);
        }
    }

    private void generateUtils(final GeneratorConfig generatorConfig,
                               final TypeUtils typeUtils,
                               final Filer filer) {
        new UtilsGenerator(
                generatorConfig,
                filer,
                typeUtils
        ).generate();
    }

    private void generateConfigs(final GeneratorConfig generatorConfig,
                                 final ApplicationContext applicationContext,
                                 final TypeUtils typeUtils,
                                 final DependencyChecker dependencyChecker,
                                 final Filer filer) {
        new SpringJacksonConfigGenerator(
                generatorConfig,
                typeUtils,
                filer,
                dependencyChecker
        ).generate();

        final var configGenerators = applicationContext.getBeansOfType(ConfigGenerator.class);
        configGenerators.forEach(ConfigGenerator::generate);
    }

    private void generateApiConfigs(final OpenApi api,
                                    final GeneratorConfig generatorConfig,
                                    final ApplicationContext applicationContext,
                                    final TypeUtils typeUtils,
                                    final Filer filer,
                                    final boolean generateConfigs) {
        if (!generateConfigs) {
            return;
        }

        new SpringOpenApiConfigGenerator(generatorConfig, typeUtils, filer).generate(api);

        final var configGenerators = applicationContext.getBeansOfType(ApiConfigGenerator.class);
        configGenerators.forEach(configGenerator -> configGenerator.generate(api));
    }

    private void generateSpringConfig(final Map<String, Object> additionalApplicationProperties,
                                      final Filer filer) {
        new SpringApplicationConfigGenerator(filer)
                .generate(additionalApplicationProperties);
    }

    private Map<String, Boolean> checkFeatures(final Map<String, Boolean> configuredFeatures,
                                               final DependencyChecker dependencyChecker) {
        final var features = new HashMap<>(configuredFeatures);

        if (configuredFeatures.get(Features.FEATURE_JAKARTA) == null) {
            if (dependencyChecker.isClassPresent(ClassNames.JAKARTA_HTTP_SERVLET_REQUEST)) {
                features.put(Features.FEATURE_JAKARTA, true);
            }
        }

        if (features.get(Features.FEATURE_CHECKER) == null) {
            if (dependencyChecker.isDependencyPresent("org.checkerframework", "checker-qual")) {
                features.put(Features.FEATURE_CHECKER, true);
            }
        }

        return features;
    }

    private ApplicationContext createApplicationContext(final DependencyChecker dependencyChecker,
                                                        final GeneratorConfig generatorConfig,
                                                        final TypeUtils typeUtils) {
        final ApplicationContext context = new ApplicationContext(dependencyChecker);
        context.registerBean(GeneratorConfig.class, generatorConfig);
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
