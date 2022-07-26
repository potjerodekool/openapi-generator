package io.github.potjerodekool.openapi.maven;

import io.github.potjerodekool.openapi.*;
import io.github.potjerodekool.openapi.internal.OpenApiGeneratorConfigImpl;
import io.github.potjerodekool.openapi.log.Logger;
import io.github.potjerodekool.openapi.log.LoggerFactory;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

import java.io.File;

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

    @Parameter(property = "dynamicModels")
    private String dynamicModels;

    @Override
    public void execute() {
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

        LoggerFactory.setLoggerProvider(this::getLogger);

        final var configBuilder = OpenApiGeneratorConfig.createBuilder(
                new File(openApiFile),
                generatedSourceDirectory,
                configPackageName
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

        final var config = configBuilder.build();

        new Generator().generate(config, dependencyChecker);
    }

    private Logger getLogger(final String name) {
        return new MavenLogger(this, name);
    }
}
