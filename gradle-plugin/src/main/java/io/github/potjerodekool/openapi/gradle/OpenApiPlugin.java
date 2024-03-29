package io.github.potjerodekool.openapi.gradle;

import io.github.potjerodekool.codegen.Language;
import io.github.potjerodekool.openapi.Features;
import io.github.potjerodekool.openapi.Generator;
import io.github.potjerodekool.openapi.common.log.LogLevel;
import io.github.potjerodekool.openapi.common.log.LoggerFactory;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.UnknownTaskException;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class OpenApiPlugin implements Plugin<Project> {

    @Override
    public void apply(final Project project) {
        final var extension = project.getExtensions().create(
                OpenApiPluginExtension.class,
                "openapi",
                OpenApiPluginExtension.class
        );

        project.task("openapi").doLast(action -> performAction(project, extension));

        addDependsOn("compileJava", project);
        addDependsOn("compileKotlin", project);
    }

    private void addDependsOn(final String taskName,
                              final Project project) {
        try {
            final var task = project.getTasks().getByName(taskName);
            task.dependsOn("openapi");
        } catch (final UnknownTaskException e) {
            //Ignore exception
        }
    }

    private void performAction(final Project project,
                               final OpenApiPluginExtension extension) {
        final var logger = new GradleLogger(project);
        LoggerFactory.setLoggerProvider(name -> logger);

        final var language = extension.getLanguage().map(Language::fromString)
                .getOrElse(Language.JAVA);
        final var checker = extension.getChecker().getOrNull();
        final var basePackageName = extension.getBasePackageName().getOrNull();

        if (basePackageName == null || basePackageName.isEmpty()) {
            logger.log(LogLevel.SEVERE, "No base basePackageName specified");
            return;
        }

        final var openApiProject = createOpenApiProject(project);

        final var apiConfigurations = extension.getApis().get().stream()
                .filter(apiConfiguration -> apiConfiguration.getOpenApiFile() != null)
                .map(this::toApiConfiguration)
                .toList();

        final var features = new HashMap<String, Boolean>();

        if (checker != null) {
            features.put(Features.FEATURE_CHECKER, checker);
        }

        new Generator().generate(
                openApiProject,
                apiConfigurations,
                features,
                basePackageName,
                language
        );
    }

    private io.github.potjerodekool.openapi.common.Project createOpenApiProject(final Project project) {
        final var dependencyChecker = new GradleDependencyChecker(project);

        final var javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);

        final var javaSourceSets = javaPluginExtension.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);

        final var sourceDirectories = new ArrayList<>(javaSourceSets.getJava().getSrcDirs().stream()
                .map(File::toPath)
                .toList());

        final var resourceDirectories = new ArrayList<>(javaSourceSets.getResources().getSrcDirs().stream()
                .map(File::toPath)
                .toList());

        final var rootDir = project.getProjectDir().toPath();
        final var generatedSourcesDir = rootDir.resolve("build/generated/sources");

        return new io.github.potjerodekool.openapi.common.Project(
                rootDir,
                sourceDirectories,
                resourceDirectories,
                generatedSourcesDir,
                dependencyChecker
        );
    }

    private io.github.potjerodekool.openapi.common.ApiConfiguration toApiConfiguration(final ApiConfiguration apiConfiguration) {
        return new io.github.potjerodekool.openapi.common.ApiConfiguration(
                new File(apiConfiguration.getOpenApiFile()),
                apiConfiguration.getBasePackageName(),
                apiConfiguration.isGenerateApiDefinitions(),
                apiConfiguration.isGenerateApiImplementations(),
                apiConfiguration.isGenerateModels(),
                new HashMap<>()
        );
    }

    private boolean getOrDefault(final Boolean value,
                                 final boolean defaultValue) {
        return value != null ? value : defaultValue;
    }
}
