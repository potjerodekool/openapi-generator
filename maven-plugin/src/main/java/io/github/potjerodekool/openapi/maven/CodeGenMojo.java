package io.github.potjerodekool.openapi.maven;

import io.github.potjerodekool.openapi.*;
import io.github.potjerodekool.openapi.internal.OpenApiGeneratorConfigImpl;
import io.github.potjerodekool.openapi.log.Logger;
import io.github.potjerodekool.openapi.log.LoggerFactory;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.List;

@Mojo(
        name = "generate",
        defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class CodeGenMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.build.directory}/generated-sources", required = true)
    private File generatedSourceDirectory;

    @Parameter(property = "openApiFile", required = true)
    private String openApiFile;

    @Parameter(property = "configPackageName")
    private String configPackageName;

    @Parameter(property = "generateModels", defaultValue = "true")
    private boolean generateModels;

    @Parameter(property = "generateApiDefinitions", defaultValue = "true")
    private boolean generateApiDefinitions;

    @Parameter(property = "jakarta.servlet")
    private Boolean jakartaServlet;

    @Parameter(property = "jakarta.validation")
    private Boolean jakartaValidation;

    @Parameter(property = "checker")
    private Boolean checker;

    @Parameter(property = "language")
    private String language;

    @Parameter(property = "externalApis")
    private List<ExternalApiConfig> externalApis;

    @Override
    public void execute() {
        LoggerFactory.setLoggerProvider(this::getLogger);
        generate();
        generateExternalApiModels();
    }

    private void generate() {
        if (openApiFile.isEmpty()) {
            getLog().warn(""" 
                No files specified to process.
                Specify a file via configuration of the plugin.
                Files must have a yml or yaml file extension.
                
                <configuration>
                  <openApiFile>openapi.yml</openApiFiles>
                </configuration>
            """);
        }

        final var configBuilder = OpenApiGeneratorConfig.createBuilder(
                new File(openApiFile),
                generatedSourceDirectory,
                configPackageName,
                null
        );

        final var dependencyChecker = new MavenDependencyChecker(this.project);
        configBuilder.generateApiDefinitions(generateApiDefinitions);
        configBuilder.generateModels(generateModels);

        if (jakartaServlet != null) {
            configBuilder.featureValue(OpenApiGeneratorConfigImpl.FEATURE_JAKARTA_SERVLET, jakartaServlet);
        }

        if (jakartaValidation != null) {
            configBuilder.featureValue(OpenApiGeneratorConfigImpl.FEATURE_JAKARTA_VALIDATION, jakartaValidation);
        }

        if (checker != null) {
            configBuilder.featureValue(OpenApiGeneratorConfigImpl.FEATURE_CHECKER, checker);
        }

        if (language != null) {
            configBuilder.language(Language.valueOf(language.toUpperCase()));
        }

        final var config = configBuilder.build(ConfigType.DEFAULT);

        new Generator().generate(config, dependencyChecker);
    }

    private void generateExternalApiModels() {
        externalApis.forEach(externalApiConfig -> generateExternalApiModels(externalApiConfig));
    }

    private void generateExternalApiModels(final ExternalApiConfig externalApiConfig) {
        final var configBuilder = OpenApiGeneratorConfig.createBuilder(
                new File(externalApiConfig.getOpenApiFile()),
                generatedSourceDirectory,
                null,
                externalApiConfig.getModelPackageName()
        );

        final var dependencyChecker = new MavenDependencyChecker(this.project);

        if (jakartaServlet != null) {
            configBuilder.featureValue(OpenApiGeneratorConfigImpl.FEATURE_JAKARTA_SERVLET, jakartaServlet);
        }

        if (jakartaValidation != null) {
            configBuilder.featureValue(OpenApiGeneratorConfigImpl.FEATURE_JAKARTA_VALIDATION, jakartaValidation);
        }

        if (checker != null) {
            configBuilder.featureValue(OpenApiGeneratorConfigImpl.FEATURE_CHECKER, checker);
        }

        if (language != null) {
            configBuilder.language(Language.valueOf(language.toUpperCase()));
        }

        final var config = configBuilder.build(ConfigType.EXTERNAL);

        new Generator().generateExternalApi(config, dependencyChecker);
    }

    private Logger getLogger(final String name) {
        return new MavenLogger(this, name);
    }
}
