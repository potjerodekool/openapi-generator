package io.github.potjerodekool.openapi.internal;

import io.github.potjerodekool.openapi.Project;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileManagerImplTest {

    @Test
    void getResource() {
        final var path = Path.of("src/test/resources/spring-boot-app/src/main/resources");

        final var project = Mockito.mock(Project.class);
        when(project.resourcePaths())
                .thenReturn(List.of(path));

        final var fileManager = new FileManagerImpl(project);
        final var fileObject = fileManager.getResource(
                Location.RESOURCE_PATH,
                null,
                "application.yml"
        );
        assertNotNull(fileObject);
    }
}