package io.github.potjerodekool.openapi.internal.generate.config;

import io.github.potjerodekool.openapi.InMemoryFileManager;
import io.github.potjerodekool.openapi.internal.Filer;
import io.github.potjerodekool.openapi.internal.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpringApplicationConfigGeneratorTest {

    private final InMemoryFileManager fileManager = new InMemoryFileManager();

    @Mock
    private Filer filerMock;

    private SpringApplicationConfigGenerator generator = new SpringApplicationConfigGenerator(
            filerMock
    );

    @BeforeEach
    void setup() {
        generator = new SpringApplicationConfigGenerator(
                filerMock
        );
    }

    @AfterEach
    void tearDown() {
        fileManager.reset();
    }

    @Test
    void generateProperties() {

    }

    @Test
    void generatePropertiesYaml() throws IOException {
        final var data =
                getClass().getClassLoader().getResourceAsStream("spring-boot-app/src/main/resources/application.yml").readAllBytes();
        fileManager.add(Location.RESOURCE_PATH, null, "application.yml", data);

        when(filerMock.getResource(
                any(),
                any(),
                any()
        )).thenAnswer(answer -> {
           final Location location = answer.getArgument(0);
            final String moduleAndPkg = answer.getArgument(1);
            final String relativeName = answer.getArgument(2);

           return fileManager.getResource(
                   location,
                   moduleAndPkg,
                   relativeName
           );
        });

        final var additionalApplicationProperties = new HashMap<String, Object>();
        additionalApplicationProperties.put("spring.jackson.date-format", "org.some.example.RFC3339DateFormat");
        additionalApplicationProperties.put("spring.jackson.serialization.WRITE_DATES_AS_TIMESTAMPS", false);

        generator.generate(additionalApplicationProperties);

        final var fileObject = fileManager.getResource(Location.RESOURCE_PATH, null, "application.yml");

        try (final var reader =  fileObject.openReader(false)) {
            Yaml yaml = new Yaml();
            final Map<String, Object> map = yaml.load(reader);
            final String dateFormat = getYamlValue("spring.jackson.date-format", map);
            final Boolean dateAsTimeStamps = getYamlValue("spring.jackson.serialization.WRITE_DATES_AS_TIMESTAMPS", map);

            assertEquals("org.some.example.RFC3339DateFormat", dateFormat);
            assertEquals(false, dateAsTimeStamps);
        }
    }

    private <T> T getYamlValue(final String key,
                               final Map<String, Object> map) {
        return getYamlValue(
                key.split("\\."),
                0,
                map
        );
    }

    private <T> T getYamlValue(final String[] keys,
                               final int keyIndex,
                               final Map<String, Object> map) {
        final var lastIndex = keys.length - 1;
        final var key = keys[keyIndex];
        final var value = map.get(key);

        if (value == null) {
            return null;
        } else if (keyIndex == lastIndex) {
            return (T) value;
        } else {
            return getYamlValue(
                    keys,
                    keyIndex + 1,
                    (Map<String, Object>)value
            );
        }
    }
}