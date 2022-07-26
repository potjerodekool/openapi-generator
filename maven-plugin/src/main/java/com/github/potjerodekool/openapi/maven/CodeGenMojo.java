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

    @Parameter(property = "model-package")
    private String modelPackage;

    @Parameter(property = "api-package")
    private String apiPackageName;

    @Parameter(property = "openApiFile", required = true)
    private String openApiFile;

    @Parameter(property = "configPackageName")
    private String configPackageName;

    @Parameter(property = "generateModels", defaultValue = "true")
    private boolean generateModels;

    @Parameter(property = "generateApiDefintions", defaultValue = "true")
    private boolean generateApiDefintions;

    @Parameter(property = "generateApiImplementations", defaultValue = "true")
    private boolean generateApiImplementations;

    @Parameter(property = "jakarta.servlet")
    private Boolean jakartaServlet;

    @Parameter(property = "jakarta.validation")
    private Boolean jakartaValidation;

    @Parameter(property = "dynamicModels")
    private String dynamicModels;

    @Override
    public void execute() {
        getLog().info("compile artifacts " + project.getCompileArtifacts().size());

        project.getCompileArtifacts().forEach(artifact -> {
            getLog().info("artifact" + artifact);
        });

        getLog().info("dependencies " + project.getDependencies().size());

        project.getDependencies().forEach(dependency -> {
            getLog().info("dependency" + dependency);
        });

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

        final var hasCheckerDependency = hasDependency("org.checkerframework", "checker-qual");

        final var config = new OpenApiGeneratorConfig(
                new File(openApiFile),
                generatedSourceDirectory
        );

        config.setAddCheckerAnnotations(hasCheckerDependency);
        config.setConfigPackageName(configPackageName);
        config.setGenerateApiDefinitions(generateApiDefintions);
        config.setGenerateApiImplementations(generateApiImplementations);
        config.setGenerateModels(generateModels);

        if (jakartaServlet != null) {
            config.setUseJakartaServlet(Boolean.TRUE.equals(jakartaServlet));
        } else {
            config.setUseJakartaServlet(hasDependency("jakarta.servlet", "jakarta.servlet-api"));
        }

        if (jakartaValidation != null) {
            config.setUseJakartaValidation(Boolean.TRUE.equals(jakartaValidation));
        } else {
            config.setUseJakartaValidation(hasDependency("jakarta.validation", "jakarta.validation-api"));
        }

        new Generator().generate(config);
    }

    private boolean hasDependency(final String groupId,
                                  final String artifactId) {
        return project.getDependencies().stream()
                .anyMatch(dependency -> groupId.equals(dependency.getGroupId())
                        && artifactId.equals(dependency.getArtifactId()));
    }

    /*
    private List<String> resolveDependencies() throws DependencyGraphBuilderException {
        final var dependencies = new ArrayList<String>();

        final var buildingRequest = new DefaultProjectBuildingRequest(new DefaultProjectBuildingRequest());
        buildingRequest.setRepositorySession(repoSession);
        buildingRequest.setProject(project);

        final var artifactFilter = new ScopeArtifactFilter("compile");

        final var rootNode = dependencyGraphBuilder.buildDependencyGraph( buildingRequest, artifactFilter);

        rootNode.getChildren().forEach( child ->
                visitChild(child, dependencies)
        );

        return dependencies;
    }

    private void visitChild(final DependencyNode node, final List<String> dependencies) {
        final var artifactRequest = createArtifactRequest(
                node.getArtifact().getGroupId(),
                node.getArtifact().getArtifactId(),
                node.getArtifact().getType(),
                node.getArtifact().getVersion()
        );

        final var artifact = resolveArtifact(artifactRequest);

        if (artifact.getFile() != null) {
            dependencies.add(artifact.getFile().getAbsolutePath());
        }

        node.getChildren().forEach( child ->
                visitChild(child, dependencies)
        );
    }

    private ArtifactRequest createArtifactRequest(final String groupId,
                                                  final String artifactId,
                                                  final String type,
                                                  final String version) {
        return new ArtifactRequest(
                new DefaultArtifact(
                        groupId,
                        artifactId,
                        type,
                        version
                ),
                List.of(),
                null
        );
    }

    private @Nullable Artifact resolveArtifact(final ArtifactRequest artifactRequest) {
        try {
            final var artifactResults = this.repoSystem.resolveArtifacts(this.repoSession, List.of(artifactRequest));
            return artifactResults.get(0).getArtifact();
        } catch (final ArtifactResolutionException e) {
            //TODO throw exception
            return null;
        }
    }

     */

/*
    private fun parseDynamicModels(): List<String> {
        val dModels = dynamicModels

        if (dModels == null) {
            return emptyList()
        } else {
            return dModels.replace("\r", "")
                    .split("\n")
                    .toList()
        }
    }
*/
    private Logger getLogger(final String name) {
        return new MavenLogger(this, name);
    }
}
