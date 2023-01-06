package io.github.potjerodekool.openapi.gradle;

import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiPluginTest {

    @Test
    void apply() {
        final var project = ProjectBuilder.builder().build();
        project.getPluginManager().apply("java");
        project.getPluginManager().apply("io.github.potjerodekool.openapi.gradle.openapi");

        assertTrue(project.getPluginManager()
                .hasPlugin("io.github.potjerodekool.openapi.gradle.openapi"));

        final var task = project.getTasks().getByName("openapi");
        assertNotNull(task);
        final var action = task.getActions().get(0);
        action.execute(task);
    }
}