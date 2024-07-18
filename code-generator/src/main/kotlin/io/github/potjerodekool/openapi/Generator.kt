package io.github.potjerodekool.openapi

import io.github.potjerodekool.codegen.Environment
import io.github.potjerodekool.codegen.Language
import io.github.potjerodekool.codegen.io.Location
import io.github.potjerodekool.codegen.model.CompilationUnit
import io.github.potjerodekool.codegen.model.util.type.Types
import io.github.potjerodekool.codegen.resolve.Enter
import io.github.potjerodekool.codegen.resolve.ImportOrganiser
import io.github.potjerodekool.codegen.resolve.Resolver
import io.github.potjerodekool.openapi.common.ApiConfiguration
import io.github.potjerodekool.openapi.common.GeneratorConfig
import io.github.potjerodekool.openapi.common.OpenApiEnvironment
import io.github.potjerodekool.openapi.common.Project
import io.github.potjerodekool.openapi.common.dependency.ApplicationContext
import io.github.potjerodekool.openapi.common.dependency.DependencyChecker
import io.github.potjerodekool.openapi.common.generate.api.CodeGenerator
import io.github.potjerodekool.openapi.common.log.LogLevel
import io.github.potjerodekool.openapi.common.log.Logger
import io.github.potjerodekool.openapi.internal.ClassPath
import io.github.potjerodekool.openapi.internal.GeneratorRegistry
import io.github.potjerodekool.openapi.internal.di.ClassPathScanner
import io.github.potjerodekool.openapi.internal.di.bean.DefaultApplicationContext
import io.swagger.parser.OpenAPIParser
import io.swagger.v3.parser.core.models.SwaggerParseResult
import java.io.File
import java.io.IOException
import java.util.function.Consumer

class Generator {

    fun generate(
        project: Project,
        apiConfigurations: List<ApiConfiguration>,
        features: Map<String, Boolean>,
        basePackageName: String,
        language: Language
    ) {
        val environment = Environment(ClassPath.getFullClassPath(project))
        configureFileManager(project, environment)

        val generatorConfig = createGeneratorConfig(
            language,
            basePackageName,
            resolveFeatures(project, features)
        )

        val applicationContext = createApplicationContext(
            project.dependencyChecker,
            generatorConfig,
            environment
        )

        val openApiEnvironment = OpenApiEnvironment(
            project,
            environment,
            generatorConfig,
            applicationContext
        )

        val codeGenerator = GeneratorRegistry()
            .loadCodeGenerator("spring-mvc", "Java")

        codeGenerator.generateCommon(openApiEnvironment)

        val standardApiConfiguration = apiConfigurations
            .firstOrNull { it.isGenerateApiDefinitions }

        apiConfigurations.stream()
            .forEach { apiConfiguration: ApiConfiguration ->
                val generateConfig = apiConfiguration === standardApiConfiguration
                generateApi(
                    openApiEnvironment,
                    codeGenerator,
                    apiConfiguration,
                    generateConfig
                )
            }

        generateCompilationUnits(environment, language)
    }

    private fun generateCompilationUnits(
        environment: Environment,
        language: Language
    ) {
        val filer = environment.filer

        val enter = Enter(environment.symbolTable)
        val resolver = Resolver(
            environment.javaElements,
            environment.javaTypes,
            environment.symbolTable
        )

        val importOrganiser = ImportOrganiser()

        environment.compilationUnits.forEach(Consumer { compilationUnit: CompilationUnit ->
            compilationUnit.accept(enter, null)
            resolver.resolve(compilationUnit)
            importOrganiser.organiseImports(compilationUnit)
            try {
                filer.writeSource(compilationUnit, language)
            } catch (e: IOException) {
                LOGGER.log(LogLevel.SEVERE, "Fail to generate code", e)
            }
        })
    }


    private fun generateApi(
        openApiEnvironment: OpenApiEnvironment,
        codeGenerator: CodeGenerator,
        apiConfiguration: ApiConfiguration,
        generateConfigs: Boolean
    ) {
        doGenerateApi(
            openApiEnvironment,
            codeGenerator,
            apiConfiguration,
            generateConfigs
        )
    }

    private fun resolveFeatures(
        project: Project,
        features: Map<String, Boolean>
    ): Map<String, Boolean> {
        return checkFeatures(features, project.dependencyChecker)
    }

    private fun createGeneratorConfig(
        language: Language,
        basePackageName: String,
        resolvedFeatures: Map<String, Boolean>
    ): GeneratorConfig {
        return GeneratorConfig(
            language,
            basePackageName,
            resolvedFeatures
        )
    }

    private fun doGenerateApi(
        openApiEnvironment: OpenApiEnvironment,
        codeGenerator: CodeGenerator,
        apiConfiguration: ApiConfiguration,
        generateConfigs: Boolean
    ) {
        val apiFile = apiConfiguration.apiFile.absoluteFile

        val parseResult = parse(apiFile)
        val openApi = parseResult.openAPI

        codeGenerator.generateApi(
            openApiEnvironment,
            openApi,
            apiConfiguration,
            generateConfigs
        )
    }

    private fun parse(file: File): SwaggerParseResult {
        return OpenAPIParser().readLocation(file.absolutePath, null, null)
    }

    private fun configureFileManager(
        project: Project,
        environment: Environment
    ) {
        val fileManager = environment.fileManager
        fileManager.setPathsForLocation(Location.RESOURCE_PATH, project.resourcePaths)
        fileManager.setPathsForLocation(
            Location.RESOURCE_OUTPUT,
            listOf(project.generatedSourcesDirectory.resolve("resources"))
        )
        fileManager.setPathsForLocation(Location.SOURCE_OUTPUT, listOf(project.generatedSourcesDirectory))
    }

    private fun checkFeatures(
        configuredFeatures: Map<String, Boolean>,
        dependencyChecker: DependencyChecker
    ): Map<String, Boolean> {
        val features = HashMap(configuredFeatures)

        if (features[Features.FEATURE_CHECKER] == null) {
            if (dependencyChecker.isDependencyPresent("org.checkerframework", "checker-qual")) {
                features[Features.FEATURE_CHECKER] = true
            }
        }

        return features
    }

    private fun createApplicationContext(
        dependencyChecker: DependencyChecker,
        generatorConfig: GeneratorConfig,
        environment: Environment
    ): ApplicationContext {
        val context = DefaultApplicationContext(dependencyChecker)
        context.registerBean(GeneratorConfig::class.java, generatorConfig)
        context.registerBean(DependencyChecker::class.java, dependencyChecker)
        context.registerBean(Types::class.java, environment.javaTypes)
        context.registerBean(Environment::class.java, environment)
        val beanDefinitions = ClassPathScanner.scan()
        context.registerBeans<Any>(beanDefinitions)
        return context
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(
            Generator::class.java.name
        )
    }
}
