package io.github.potjerodekool.openapi.test;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.io.FileManager;
import io.github.potjerodekool.codegen.io.FilerImpl;
import io.github.potjerodekool.codegen.loader.java.JavaElements;
import io.github.potjerodekool.codegen.loader.kotlin.KotlinElements;
import io.github.potjerodekool.codegen.model.util.SymbolTable;
import io.github.potjerodekool.codegen.model.util.type.JavaTypes;
import io.github.potjerodekool.codegen.model.util.type.KotlinTypes;
import io.github.potjerodekool.openapi.internal.ClassPath;
import io.github.potjerodekool.openapi.common.Project;
import io.github.potjerodekool.openapi.common.dependency.Artifact;
import io.github.potjerodekool.openapi.common.dependency.DependencyChecker;

import java.io.File;
import java.util.Arrays;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class TestUtils {

    private TestUtils() {
    }

    public static Project createProject() {
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
        when(project.getDependencyChecker())
                .thenReturn(dependencyChecker);

        return project;
    }

}
