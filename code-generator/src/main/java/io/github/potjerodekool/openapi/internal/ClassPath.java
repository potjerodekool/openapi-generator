package io.github.potjerodekool.openapi.internal;

import io.github.potjerodekool.openapi.common.Project;
import io.github.potjerodekool.openapi.common.dependency.Artifact;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class ClassPath {

    private ClassPath() {
    }

    public static URL[] getFullClassPath(final Project project) {
        final var fullClassPath = new ArrayList<URL>();
        fullClassPath.addAll(getProjectClassPath(project));
        fullClassPath.addAll(Arrays.stream(io.github.potjerodekool.codegen.loader.java.ClassPath.getJavaClassPath()).toList());
        return fullClassPath.toArray(URL[]::new);
    }


    public static List<URL> getProjectClassPath(final Project project) {
        return project.dependencyChecker().getProjectArtifacts()
                .map(Artifact::file)
                .filter(Objects::nonNull)
                .map(file -> action(() -> file.toURI().toURL()))
                .toList();
    }

    private static <R> R action(final Action<R> action) {
        try {
            return action.execute();
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}

@FunctionalInterface
interface Action<R> {

    R execute() throws Exception;
}