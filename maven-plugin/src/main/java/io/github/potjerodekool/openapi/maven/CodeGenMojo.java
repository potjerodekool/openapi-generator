package io.github.potjerodekool.openapi.maven;

import io.github.potjerodekool.codegen.Language;
import io.github.potjerodekool.openapi.*;
import io.github.potjerodekool.openapi.log.Logger;
import io.github.potjerodekool.openapi.log.LoggerFactory;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mojo(
        name = "generate",
        defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class CodeGenMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(property = "basePackageName")
    private String basePackageName;

    @Parameter(property = "jakarta")
    private Boolean jakarta;

    @Parameter(property = "checker")
    private Boolean checker;

    @Parameter(property = "features")
    private Map<String, Boolean> features;

    @Parameter(property = "language")
    private String language;

    @Parameter(property = "apis")
    private List<ApiConfiguration> apis;

    @Override
    public void execute() {
        LoggerFactory.setLoggerProvider(this::getLogger);
        generateApis();
    }

    private void generateApis() {
        final var language = Language.fromString(this.language);

        final var sourceRoots = project.getCompileSourceRoots().stream()
                .map(Paths::get)
                .toList();

        final var resourceRoots = project.getResources().stream()
                .map(resource -> Paths.get(resource.getDirectory()))
                .toList();

        final var dependencyChecker = new MavenDependencyChecker(this.project);

        final var rootDir = this.project.getBasedir().toPath();

        final var project = new Project(
                rootDir,
                sourceRoots,
                resourceRoots,
                rootDir.resolve("target/generated-sources"),
                dependencyChecker);

        final var apiConfigurations = apis.stream()
                .filter(apiConfiguration -> apiConfiguration.getOpenApiFile() != null)
                .map(this::toApiConfiguration)
                .toList();

        final var features = new HashMap<String, Boolean>();

        if (jakarta != null) {
            features.put(Features.FEATURE_JAKARTA, jakarta);
        }

        if (checker != null) {
            features.put(Features.FEATURE_CHECKER, checker);
        }

        features.putAll(this.features);

        new Generator().generate(
                project,
                apiConfigurations,
                features,
                this.basePackageName,
                language
        );
    }

    private io.github.potjerodekool.openapi.ApiConfiguration toApiConfiguration(final ApiConfiguration apiConfiguration) {
        return new io.github.potjerodekool.openapi.ApiConfiguration(
                new File(apiConfiguration.getOpenApiFile()),
                apiConfiguration.getBasePackageName(),
                apiConfiguration.isGenerateApiDefinitions(),
                apiConfiguration.isGenerateModels(),
                new HashMap<>()
        );
    }

    private Logger getLogger(final String name) {
        return new MavenLogger(this, name);
    }
}
