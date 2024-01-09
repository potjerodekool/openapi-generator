package io.github.potjerodekool.openapi;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.Language;
import io.github.potjerodekool.codegen.io.Location;
import io.github.potjerodekool.codegen.model.util.type.Types;
import io.github.potjerodekool.codegen.resolve.Enter;
import io.github.potjerodekool.codegen.resolve.Resolver;
import io.github.potjerodekool.openapi.dependency.DependencyChecker;
import io.github.potjerodekool.openapi.internal.*;
import io.github.potjerodekool.openapi.internal.di.ApplicationContext;
import io.github.potjerodekool.openapi.internal.di.DefaultApplicationContext;
import io.github.potjerodekool.openapi.internal.di.ClassPathScanner;
import io.github.potjerodekool.openapi.internal.generate.api.AbstractCodeGenerator;
import io.github.potjerodekool.openapi.internal.generate.springmvc.SpringMvcGenerator;
import io.github.potjerodekool.openapi.log.LogLevel;
import io.github.potjerodekool.openapi.log.Logger;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Generator {

    private static final Logger LOGGER = Logger.getLogger(Generator.class.getName());

    public void generate(final Project project,
                         final List<ApiConfiguration> apiConfigurations,
                         final Map<String, Boolean> features,
                         final String basePackageName,
                         final Language language) {
        final var environment = new Environment(ClassPath.getFullClassPath(project));
        configureFileManager(project, environment);

        final var generatorConfig = createGeneratorConfig(
                language,
                basePackageName,
                resolveFeatures(project, features)
        );

        final var applicationContext = createApplicationContext(
                project.dependencyChecker(),
                generatorConfig,
                environment
        );

        final var openApiEnvironment = new OpenApiEnvironment(
                project,
                environment,
                generatorConfig,
                applicationContext
        );

        final var springGenerator = new SpringMvcGenerator();
        springGenerator.generateCommon(openApiEnvironment);

        final var standardApiConfiguration = apiConfigurations.stream()
                .filter(ApiConfiguration::generateApiDefinitions)
                .findFirst()
                .orElse(null);

        apiConfigurations.forEach(apiConfiguration -> {
            final var generateConfig = apiConfiguration == standardApiConfiguration;
            generateApi(
                    openApiEnvironment,
                    springGenerator,
                    apiConfiguration,
                    generateConfig
            );
        });

        generateCompilationUnits(environment, language);
        springGenerator.generate(environment);
    }

    private void generateCompilationUnits(final Environment environment,
                                          final Language language) {
        final var filer = environment.getFiler();

        final var enter = new Enter(environment.getSymbolTable());
        final var resolver = new Resolver(
                environment.getJavaElements(),
                environment.getJavaTypes(),
                environment.getSymbolTable());

        environment.getCompilationUnits().forEach(compilationUnit -> {
            compilationUnit.accept(enter, null);
            resolver.resolve(compilationUnit);

            try {
                filer.writeSource(compilationUnit, language);
            } catch (final IOException e) {
                LOGGER.log(LogLevel.SEVERE, "Fail to generate code", e);
            }
        });
    }


    private void generateApi(final OpenApiEnvironment openApiEnvironment,
                             final AbstractCodeGenerator codeGenerator,
                             final ApiConfiguration apiConfiguration,
                             final boolean generateConfigs) {
        final var apiFile = apiConfiguration.apiFile().getAbsoluteFile();
        final var rootDir = apiFile.getParentFile();

        if (rootDir == null) {
            throw new GenerateException("Api file has no parent directory");
        }

        final var configurationFile = loadConfigFor(apiFile);

        final var controllers = configurationFile.getControllers();

        doGenerateApi(
                openApiEnvironment,
                codeGenerator,
                apiFile,
                apiConfiguration.withControllers(controllers),
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
        final var rootDir = apiFile.getParentFile();
        final var apiFileName = apiFile.getName();
        final var separatorIndex = apiFileName.lastIndexOf('.');
        final var file = new File(rootDir, apiFileName.substring(0, separatorIndex) + "-config.json");

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

    private void doGenerateApi(final OpenApiEnvironment openApiEnvironment,
                               final AbstractCodeGenerator codeGenerator,
                               final File apiFile,
                               final ApiConfiguration apiConfiguration,
                               final boolean generateConfigs) {
        final var parseResult = parse(apiFile);
        final var openApi = parseResult.getOpenAPI();

        codeGenerator.generateApi(
                openApiEnvironment,
                openApi,
                apiConfiguration,
                generateConfigs);
    }

    private SwaggerParseResult parse(final File file) {
        return new OpenAPIParser().readLocation(file.getAbsolutePath(), null, null);
    }

    private void configureFileManager(final Project project,
                                      final Environment environment) {
        final var fileManager = environment.getFileManager();
        fileManager.setPathsForLocation(Location.RESOURCE_PATH, project.resourcePaths());
        fileManager.setPathsForLocation(Location.RESOURCE_OUTPUT, List.of(project.generatedSourcesDirectory().resolve("resources")));
        fileManager.setPathsForLocation(Location.SOURCE_OUTPUT, List.of(project.generatedSourcesDirectory()));
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
                                                        final Environment environment) {
        final var context = new DefaultApplicationContext(dependencyChecker);
        context.registerBean(GeneratorConfig.class, generatorConfig);
        context.registerBean(DependencyChecker.class, dependencyChecker);
        context.registerBean(Types.class, environment.getJavaTypes());
        context.registerBean(Environment.class, environment);
        final var beanDefinitions = ClassPathScanner.scan();
        context.registerBeans(beanDefinitions);
        return context;
    }
}
