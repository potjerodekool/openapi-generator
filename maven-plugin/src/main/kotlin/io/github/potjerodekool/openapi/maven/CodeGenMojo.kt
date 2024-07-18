package io.github.potjerodekool.openapi.maven

import io.github.potjerodekool.codegen.Language
import io.github.potjerodekool.openapi.Features
import io.github.potjerodekool.openapi.Generator
import io.github.potjerodekool.openapi.common.Project
import io.github.potjerodekool.openapi.common.log.Logger
import io.github.potjerodekool.openapi.common.log.LoggerFactory.Companion.setLoggerProvider
import org.apache.maven.model.Resource
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import org.apache.maven.project.MavenProject
import java.io.File
import java.nio.file.Paths

@Mojo(
    name = "generate",
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME
)
class CodeGenMojo : AbstractMojo() {
    @Parameter(defaultValue = "\${project}", required = true, readonly = true)
    private val project: MavenProject? = null

    @Parameter(property = "basePackageName")
    private val basePackageName: String? = null

    @Parameter(property = "jakarta")
    private val jakarta: Boolean? = null

    @Parameter(property = "checker")
    private val checker: Boolean? = null

    @Parameter(property = "features")
    private val features: Map<String, Boolean> = HashMap()

    @Parameter(property = "language")
    private val language: String? = null

    @Parameter(property = "apis")
    private val apis: List<ApiConfiguration> = ArrayList()

    override fun execute() {
        setLoggerProvider { name: String -> this.getLogger(name) }
        generateApis()
    }

    private fun generateApis() {
        val language = Language.fromString(this.language)

        val sourceRoots = project!!.compileSourceRoots.stream()
            .map { first: String -> Paths.get(first) }
            .toList()

        val resourceRoots = project.resources.stream()
            .map { resource: Resource -> Paths.get(resource.directory) }
            .toList()

        val dependencyChecker = MavenDependencyChecker(this.project)

        val rootDir = project.basedir.toPath()

        val project = Project(
            rootDir,
            sourceRoots,
            resourceRoots,
            rootDir.resolve("target/generated-sources"),
            dependencyChecker
        )

        val apiConfigurations = apis.stream()
            .filter { apiConfiguration: ApiConfiguration -> apiConfiguration.openApiFile != null }
            .map { apiConfiguration: ApiConfiguration -> this.toApiConfiguration(apiConfiguration) }
            .toList()

        val features = HashMap<String, Boolean>()

        if (checker != null) {
            features[Features.FEATURE_CHECKER] = checker
        }

        features.putAll(this.features)

        Generator().generate(
            project,
            apiConfigurations,
            features,
            basePackageName!!,
            language
        )
    }

    private fun toApiConfiguration(apiConfiguration: ApiConfiguration): io.github.potjerodekool.openapi.common.ApiConfiguration {
        return io.github.potjerodekool.openapi.common.ApiConfiguration(
            File(apiConfiguration.openApiFile),
            apiConfiguration.basePackageName,
            apiConfiguration.isGenerateApiDefinitions,
            apiConfiguration.isGenerateApiImplementations,
            apiConfiguration.isGenerateModels
        )
    }

    private fun getLogger(name: String): Logger {
        return MavenLogger(this, name)
    }
}
