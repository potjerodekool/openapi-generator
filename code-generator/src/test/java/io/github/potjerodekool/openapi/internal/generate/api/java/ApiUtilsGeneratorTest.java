package io.github.potjerodekool.openapi.internal.generate.api.java;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.Language;
import io.github.potjerodekool.codegen.io.FileManager;
import io.github.potjerodekool.codegen.io.FilerImpl;
import io.github.potjerodekool.codegen.io.Location;
import io.github.potjerodekool.codegen.loader.java.JavaElements;
import io.github.potjerodekool.codegen.loader.kotlin.KotlinElements;
import io.github.potjerodekool.codegen.model.util.SymbolTable;
import io.github.potjerodekool.codegen.model.util.type.JavaTypes;
import io.github.potjerodekool.codegen.model.util.type.KotlinTypes;
import io.github.potjerodekool.openapi.*;
import io.github.potjerodekool.openapi.dependency.Artifact;
import io.github.potjerodekool.openapi.dependency.DependencyChecker;
import io.github.potjerodekool.openapi.internal.generate.api.ApiUtilsGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.io.File;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiUtilsGeneratorTest {

    private Project createProject() {
        final var dependencyChecker = mock(DependencyChecker.class);

        String classpath = System.getProperty("java.class.path");
        String[] classpathEntries = classpath.split(File.pathSeparator);

        final var artifacts = Arrays.stream(classpathEntries)
                .filter(it -> it.endsWith(".jar"))
                .map(entry -> new Artifact("", "", new File(entry), "", ""))
                .toList();

        when(dependencyChecker.getProjectArtifacts())
                .thenAnswer(answer -> artifacts.stream());

        final var project = mock(Project.class);
        when(project.dependencyChecker())
                .thenReturn(dependencyChecker);

        return project;
    }

    private Environment createEnvironment(final Project project,
                                          final FileManager fileManager) {
        final var classPath = ClassPath.getFullClassPath(project);
        final var symbolTable = new SymbolTable();
        final var javaTypes = new JavaTypes(symbolTable);
        final var types = new KotlinTypes(javaTypes);
        final var javaElements = new JavaElements(symbolTable, classPath, javaTypes);
        final var elements = new KotlinElements(symbolTable, classPath, javaElements);
        final var filer = new FilerImpl(elements, types, fileManager);

        final var environmentMock = mock(Environment.class);
        when(environmentMock.getSymbolTable()).thenReturn(symbolTable);
        when(environmentMock.getElementUtils()).thenReturn(elements);
        when(environmentMock.getTypes()).thenReturn(types);
        when(environmentMock.getFiler()).thenReturn(filer);

        return environmentMock;
    }

    @Test
    void generateJava() {
        final var generatorConfigMock = mock(GeneratorConfig.class);
        when(generatorConfigMock.basePackageName())
                .thenReturn("org.some.util");
        when(generatorConfigMock.isFeatureEnabled(ArgumentMatchers.eq(Features.FEATURE_JAKARTA)))
                .thenReturn(true);
        when(generatorConfigMock.language())
                .thenReturn(Language.JAVA);

        final var fileManager = new InMemoryFileManager();
        final var projectMock = createProject();
        final var environmentMock = createEnvironment(projectMock, fileManager);

        new ApiUtilsGenerator(generatorConfigMock, environmentMock)
                .generate();

        final var resource = fileManager.getResource(Location.SOURCE_OUTPUT, "org.some.util", "ApiUtils.java");
        final var actual = new String(fileManager.getData(resource));
        final var expected = ResourceLoader.readContent("io/github/potjerodekool/openapi/internal/generate/api/java/ApiUtils.java");
        assertEquals(expected, actual);
    }

    @Test
    void generateKotlin() {
        final var generatorConfigMock = mock(GeneratorConfig.class);
        when(generatorConfigMock.basePackageName())
                .thenReturn("org.some.util");
        when(generatorConfigMock.isFeatureEnabled(ArgumentMatchers.eq(Features.FEATURE_JAKARTA)))
                .thenReturn(true);
        when(generatorConfigMock.language())
                .thenReturn(Language.KOTLIN);

        final var fileManager = new InMemoryFileManager();
        final var projectMock = createProject();
        final var environmentMock = createEnvironment(projectMock, fileManager);

        new ApiUtilsGenerator(generatorConfigMock, environmentMock)
                .generate();

        final var resource = fileManager.getResource(Location.SOURCE_OUTPUT, "org.some.util", "ApiUtils.kt");
        final var actual = new String(fileManager.getData(resource));
        final var expected = ResourceLoader.readContent("io/github/potjerodekool/openapi/internal/generate/api/kotlin/ApiUtils.kt");
        assertEquals(expected, actual);
    }
}