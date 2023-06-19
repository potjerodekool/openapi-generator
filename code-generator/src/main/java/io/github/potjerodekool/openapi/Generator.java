package io.github.potjerodekool.openapi;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.Language;
import io.github.potjerodekool.codegen.io.Location;
import io.github.potjerodekool.codegen.model.util.type.Types;
import io.github.potjerodekool.openapi.dependency.DependencyChecker;
import io.github.potjerodekool.openapi.generate.config.ApiConfigGenerator;
import io.github.potjerodekool.openapi.generate.config.ConfigGenerator;
import io.github.potjerodekool.openapi.internal.ClassNames;
import io.github.potjerodekool.openapi.internal.ConfigFile;
import io.github.potjerodekool.openapi.internal.OpenApiParserHelper;
import io.github.potjerodekool.openapi.internal.TreeBuilder;
import io.github.potjerodekool.openapi.internal.di.ApplicationContext;
import io.github.potjerodekool.openapi.internal.di.ClassPathScanner;
import io.github.potjerodekool.openapi.internal.generate.api.SpringApiDefinitionGenerator;
import io.github.potjerodekool.openapi.internal.generate.api.UtilsGenerator;
import io.github.potjerodekool.openapi.internal.generate.config.SpringApplicationConfigGenerator;
import io.github.potjerodekool.openapi.internal.generate.config.SpringJacksonConfigGenerator;
import io.github.potjerodekool.openapi.internal.generate.config.SpringOpenApiConfigGenerator;
import io.github.potjerodekool.openapi.internal.generate.model.ModelsCodeGenerator;
import io.github.potjerodekool.openapi.internal.type.OpenApiTypeUtils;
import io.github.potjerodekool.openapi.internal.util.GenerateException;
import io.github.potjerodekool.openapi.internal.util.TypeUtils;
import io.github.potjerodekool.openapi.tree.OpenApi;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Generator {

    public void generate(final Project project,
                         final List<ApiConfiguration> apiConfigurations,
                         final Map<String, Boolean> features,
                         final String basePackageName,
                         final Language language) {
        final var environment = new Environment(ClassPath.getFullClassPath(project));
        configureFileManager(project, environment);

        final var typeUtils = new TypeUtils(
                environment.getTypes(),
                environment.getElementUtils()
        );

        final var openApiTypeUtils = createOpenApiTypeUtils(
                environment,
                typeUtils
        );

        generateCommon(
                project,
                basePackageName,
                language,
                features,
                typeUtils,
                openApiTypeUtils,
                environment
        );

        final var standardApiConfiguration = apiConfigurations.stream()
                .filter(ApiConfiguration::generateApiDefinitions)
                .findFirst()
                .orElse(null);

        apiConfigurations.forEach(apiConfiguration -> {
            final var generateConfig = apiConfiguration == standardApiConfiguration;
            generateApi(
                    project,
                    apiConfiguration,
                    features,
                    basePackageName,
                    language,
                    typeUtils,
                    openApiTypeUtils,
                    environment,
                    generateConfig
            );
        });
    }

    private void generateCommon(final Project project,
                                final String basePackageName,
                                final Language language,
                                final Map<String, Boolean> features,
                                final TypeUtils typeUtils,
                                final OpenApiTypeUtils openApiTypeUtils,
                                final Environment environment) {
        final var generatorConfig = createGeneratorConfig(
                language,
                basePackageName,
                resolveFeatures(project, features)
        );

        generateUtils(
                generatorConfig,
                environment
        );

        final var applicationContext = createApplicationContext(
                project.dependencyChecker(),
                generatorConfig,
                environment,
                typeUtils,
                openApiTypeUtils
        );

        generateConfigs(
                generatorConfig,
                applicationContext,
                project.dependencyChecker(),
                environment
        );

        final var additionalApplicationProperties = new HashMap<String, Object>();

        generateSpringConfig(additionalApplicationProperties, environment);
    }

    private void generateApi(final Project project,
                             final ApiConfiguration apiConfiguration,
                             final Map<String, Boolean> features,
                             final String basePackageName,
                             final Language language,
                             final TypeUtils typeUtils,
                             final OpenApiTypeUtils openApiTypeUtils,
                             final Environment environment,
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

        doGenerateApi(
                apiFile,
                rootDir,
                generatorConfig,
                apiConfiguration.withControllers(controllers),
                project.dependencyChecker(),
                openApiTypeUtils,
                typeUtils,
                environment,
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

    private void doGenerateApi(final File apiFile,
                               final File rootDir,
                               final GeneratorConfig generatorConfig,
                               final ApiConfiguration apiConfiguration,
                               final DependencyChecker dependencyChecker,
                               final OpenApiTypeUtils openApiTypeUtils,
                               final TypeUtils typeUtils,
                               final Environment environment,
                               final boolean generateConfigs) {
        final var openApi = OpenApiParserHelper.merge(apiFile);
        final var builder = new TreeBuilder(apiConfiguration);
        final var api = builder.build(openApi, rootDir);

        final var applicationContext = createApplicationContext(
                dependencyChecker,
                generatorConfig,
                environment,
                typeUtils,
                openApiTypeUtils
        );

        generateModels(
                api,
                apiConfiguration,
                generatorConfig,
                applicationContext,
                typeUtils,
                openApiTypeUtils,
                environment
        );

        generateApiDefinitions(
                api,
                apiConfiguration,
                generatorConfig,
                typeUtils,
                openApiTypeUtils,
                environment
        );

        final var additionalApplicationProperties = new HashMap<String, Object>();

        generateApiConfigs(
                api,
                generatorConfig,
                applicationContext,
                environment,
                generateConfigs
        );

        generateSpringConfig(additionalApplicationProperties, environment);
    }

    private OpenApiTypeUtils createOpenApiTypeUtils(final Environment environment,
                                                    final TypeUtils typeUtils) {
        return new OpenApiTypeUtils(environment.getTypes(), typeUtils, environment.getElementUtils());
    }

    private void configureFileManager(final Project project,
                                      final Environment environment) {
        final var fileManager = environment.getFileManager();
        fileManager.setPathsForLocation(Location.RESOURCE_PATH, project.resourcePaths());
        fileManager.setPathsForLocation(Location.RESOURCE_OUTPUT, List.of(project.generatedSourcesDirectory().resolve("resources")));
        fileManager.setPathsForLocation(Location.SOURCE_OUTPUT, List.of(project.generatedSourcesDirectory()));
    }

    private void generateModels(final OpenApi api,
                                final ApiConfiguration apiConfiguration,
                                final GeneratorConfig generatorConfig,
                                final ApplicationContext applicationContext,
                                final TypeUtils typeUtils,
                                final OpenApiTypeUtils openApiTypeUtils,
                                final Environment environment) {
        final var generateModels = apiConfiguration.generateModels();

        if (generateModels) {
            new ModelsCodeGenerator(
                    typeUtils,
                    openApiTypeUtils,
                    environment,
                    generatorConfig.language(),
                    applicationContext
            ).generate(api);
        }
    }

    private void generateApiDefinitions(final OpenApi api,
                                        final ApiConfiguration apiConfiguration,
                                        final GeneratorConfig generatorConfig,
                                        final TypeUtils typeUtils,
                                        final OpenApiTypeUtils openApiTypeUtils,
                                        final Environment environment) {
        if (apiConfiguration.generateApiDefinitions()) {
            new SpringApiDefinitionGenerator(
                    generatorConfig,
                    apiConfiguration,
                    typeUtils,
                    openApiTypeUtils,
                    environment
            ).generate(api);
        }
    }

    private void generateUtils(final GeneratorConfig generatorConfig,
                               final Environment environment) {
        new UtilsGenerator(
                generatorConfig,
                environment
        ).generate();
    }

    private void generateConfigs(final GeneratorConfig generatorConfig,
                                 final ApplicationContext applicationContext,
                                 final DependencyChecker dependencyChecker,
                                 final Environment environment) {
        new SpringJacksonConfigGenerator(
                generatorConfig,
                environment,
                dependencyChecker
        ).generate();

        final var configGenerators = applicationContext.getBeansOfType(ConfigGenerator.class);
        configGenerators.forEach(ConfigGenerator::generate);
    }

    private void generateApiConfigs(final OpenApi api,
                                    final GeneratorConfig generatorConfig,
                                    final ApplicationContext applicationContext,
                                    final Environment environment,
                                    final boolean generateConfigs) {
        if (!generateConfigs) {
            return;
        }

        new SpringOpenApiConfigGenerator(generatorConfig, environment).generate(api);

        final var configGenerators = applicationContext.getBeansOfType(ApiConfigGenerator.class);
        configGenerators.forEach(configGenerator -> configGenerator.generate(api));
    }

    private void generateSpringConfig(final Map<String, Object> additionalApplicationProperties,
                                      final Environment environment) {
        new SpringApplicationConfigGenerator(environment.getFiler())
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
                                                        final Environment environment,
                                                        final TypeUtils typeUtils,
                                                        final OpenApiTypeUtils openApiTypeUtils) {
        final ApplicationContext context = new ApplicationContext(dependencyChecker);
        context.registerBean(GeneratorConfig.class, generatorConfig);
        context.registerBean(DependencyChecker.class, dependencyChecker);
        context.registerBean(Types.class, environment.getTypes());
        context.registerBean(Environment.class, environment);
        context.registerBean(TypeUtils.class, typeUtils);
        context.registerBean(OpenApiTypeUtils.class, openApiTypeUtils);

        final var beanDefinitions = ClassPathScanner.scan();
        context.registerBeans(beanDefinitions);
        return context;
    }
}
