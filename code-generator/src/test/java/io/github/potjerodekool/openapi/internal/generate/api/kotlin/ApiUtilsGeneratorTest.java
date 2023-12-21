package io.github.potjerodekool.openapi.internal.generate.api.kotlin;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.Language;
import io.github.potjerodekool.codegen.io.FilerImpl;
import io.github.potjerodekool.codegen.io.Location;
import io.github.potjerodekool.openapi.Features;
import io.github.potjerodekool.openapi.GeneratorConfig;
import io.github.potjerodekool.openapi.InMemoryFileManager;
import io.github.potjerodekool.openapi.ResourceLoader;
import io.github.potjerodekool.openapi.internal.generate.api.ApiUtilsGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiUtilsGeneratorTest {

    @Test
    void generate() {
        final var generatorConfigMock = mock(GeneratorConfig.class);
        when(generatorConfigMock.basePackageName())
                .thenReturn("org.some.util");
        when(generatorConfigMock.isFeatureEnabled(ArgumentMatchers.eq(Features.FEATURE_JAKARTA)))
                .thenReturn(true);
        when(generatorConfigMock.language())
                .thenReturn(Language.KOTLIN);

        final var fileManager = new InMemoryFileManager();
        final var environmentMock = mock(Environment.class);

        final var filer = new FilerImpl(
                null,
                null,
                fileManager
        );

        when(environmentMock.getFiler())
                .thenReturn(filer);

        new ApiUtilsGenerator(generatorConfigMock, environmentMock)
                .generate();

        final var resource = fileManager.getResource(Location.SOURCE_OUTPUT, "org.some.util", "ApiUtils.kt");
        final var actual = new String(fileManager.getData(resource));
        final var expected = ResourceLoader.readContent("io/github/potjerodekool/openapi/internal/generate/api/kotlin/ApiUtils.kt");
        assertEquals(expected, actual);
    }
}