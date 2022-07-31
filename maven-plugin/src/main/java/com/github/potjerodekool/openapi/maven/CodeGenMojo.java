package com.github.potjerodekool.openapi.maven;

import com.github.potjerodekool.openapi.Generator;
import com.github.potjerodekool.openapi.OpenApiGeneratorConfig;
import com.github.potjerodekool.openapi.Logger;
import com.github.potjerodekool.openapi.LoggerFactory;
//import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
//import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
//import org.apache.maven.project.DefaultProjectBuildingRequest;
//import org.apache.maven.project.MavenProject;
//import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
//import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
//import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.project.MavenProject;
//import org.sonatype.aether.RepositorySystem;
//import org.sonatype.aether.RepositorySystemSession;
//import org.sonatype.aether.artifact.Artifact;
//import org.sonatype.aether.resolution.ArtifactRequest;
//import org.sonatype.aether.resolution.ArtifactResolutionException;
//import org.sonatype.aether.util.artifact.DefaultArtifact;
//import org.apache.maven.project.MavenProject;
//import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
//import org.eclipse.aether.DefaultRepositorySystemSession;
//import org.eclipse.aether.RepositorySystem;
//import org.eclipse.aether.RepositorySystemSession;

import java.io.File;

@Mojo(
        name = "generate",
        defaultPhase = LifecyclePhase.GENERATE_SOURCES
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

    @Parameter(property = "generateApiImplementations", defaultValue = "false")
    private boolean generateApiImplementations;

    @Parameter(property = "jakarta.servlet")
    private Boolean jakartaServlet;

    @Parameter(property = "jakarta.validation")
    private Boolean jakartaValidation;

    @Parameter(property = "dynamicModels")
    private String dynamicModels;

    @Override
    public void execute() {
        getLog().info("CodeGenMojo");

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



        final var config = new OpenApiGeneratorConfig(
                new File(openApiFile),
                generatedSourceDirectory
        );

        final var hasCheckerDependency = hasDependency("org.checkerframework", "checker-qual");

        config.setConfigPackageName(configPackageName);
        config.setGenerateApiDefinitions(generateApiDefinitions);
        config.setGenerateApiImplementations(generateApiImplementations);
        config.setGenerateModels(generateModels);
        config.setAddCheckerAnnotations(hasCheckerDependency);
        config.setUseJakartaServlet(
                enableFeature(jakartaServlet, "jakarta.servlet", "jakarta.servlet-api"));
        config.setUseJakartaValidation(
                enableFeature(jakartaValidation, "jakarta.validation", "jakarta.validation-api")
        );

        new Generator().generate(config);
    }

    private boolean enableFeature(final Boolean value,
                                  final String groupId,
                                  final String artifact) {
        return value != null
                ? value
                : hasDependency(groupId, artifact);
    }

    private boolean hasDependency(final String groupId,
                                  final String artifactId) {
        return project.getDependencies().stream()
                .anyMatch(dependency -> groupId.equals(dependency.getGroupId())
                        && artifactId.equals(dependency.getArtifactId()));
    }

    private Logger getLogger(final String name) {
        return new MavenLogger(this, name);
    }
}
