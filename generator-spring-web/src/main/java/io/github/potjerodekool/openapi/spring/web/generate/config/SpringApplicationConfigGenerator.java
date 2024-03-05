package io.github.potjerodekool.openapi.spring.web.generate.config;

import io.github.potjerodekool.codegen.io.FileObject;
import io.github.potjerodekool.codegen.io.Filer;
import io.github.potjerodekool.codegen.io.Location;
import io.github.potjerodekool.openapi.common.PropertiesUpdater;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.util.*;

public class SpringApplicationConfigGenerator {

    private final Filer filer;

    public SpringApplicationConfigGenerator(final Filer filer) {
        this.filer = filer;
    }

    public void generate(final Map<String, Object> additionalApplicationProperties) {
        final var fileOptional = resolveFile();
        fileOptional.ifPresent(file -> {
            if (file.getKind() == FileObject.Kind.PROPERTIES) {
                createOfUpdateApplicationProperties(
                        additionalApplicationProperties,
                        file
                );
            } else if (file.getKind() == FileObject.Kind.YAML) {
                createOfUpdateApplicationPropertiesYaml(
                        additionalApplicationProperties,
                        file
                );
            }
        });
    }

    private Optional<FileObject> resolveFile() {
        final var fileNames = List.of(
                "application.properties",
                "application.yml",
                "application.yaml",
                "application.properties"
        );

        return fileNames.stream()
                .map(fileName -> filer.getResource(Location.RESOURCE_PATH, null, fileName))
                .filter(Objects::nonNull)
                .findFirst();
    }

    private void createOfUpdateApplicationProperties(final Map<String, Object> additionalApplicationProperties,
                                                     final FileObject fileObject) {
        if (!additionalApplicationProperties.isEmpty()) {
            PropertiesUpdater.update(fileObject, additionalApplicationProperties);
        }
    }

    private void createOfUpdateApplicationPropertiesYaml(final Map<String, Object> additionalApplicationProperties,
                                                         final FileObject fileObject) {
        final var yaml = new Yaml();
        final var yamlMap = new HashMap<String, Object>();

        try (final var reader = fileObject.openReader(false)) {
            final Map<String, Object> map = yaml.load(reader);
            yamlMap.putAll(map);
        } catch (final IOException e) {
            //Ignore
        }

        var modified = false;

        for (final var entry : additionalApplicationProperties.entrySet()) {
            final var key = entry.getKey();
            final var value = entry.getValue();
            final var keyElements = key.split("\\.");
            if (addValueToYaml(keyElements, 0, value, yamlMap)) {
                modified = true;
            }
        }

        if (modified) {
            try (final var fileWriter = fileObject.openWriter()) {
                final var code = yaml.dumpAsMap(yamlMap);
                fileWriter.write(code);
            } catch (final IOException e) {
                //Ignore
            }
        }
    }

    private boolean addValueToYaml(final String[] keyElements,
                                   final int keyIndex,
                                   final Object value,
                                   final Map<String, Object> yamlMap) {
        final boolean added;

        final var lastKeyIndex = keyElements.length - 1;
        final var key = keyElements[keyIndex];

        if (yamlMap.containsKey(key)) {
            final var subValue = yamlMap.get(key);

            if (keyIndex < lastKeyIndex) {
                final var subMap = (Map<String, Object>) subValue;
                added = addValueToYaml(keyElements, keyIndex + 1, value, subMap);
            } else {
                added = false;
            }
        } else if (keyIndex < lastKeyIndex) {
            final var subMap = new HashMap<String, Object>();
            yamlMap.put(key, subMap);
            added = addValueToYaml(keyElements, keyIndex + 1, value, subMap);
        } else {
            yamlMap.put(key, value);
            added = true;
        }

        return added;
    }
}

