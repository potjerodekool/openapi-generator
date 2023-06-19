package io.github.potjerodekool.openapi.internal.generate.config;

import io.github.potjerodekool.codegen.io.FileObject;
import io.github.potjerodekool.codegen.io.Filer;
import io.github.potjerodekool.codegen.io.Location;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class SpringApplicationConfigGenerator {

    private final Filer filer;

    public SpringApplicationConfigGenerator(final Filer filer) {
        this.filer = filer;
    }

    public void generate(final Map<String, Object> additionalApplicationProperties) {
        final var file = resolveFile();

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
    }

    private FileObject resolveFile() {
        var fileObject = filer.getResource(Location.RESOURCE_PATH, null, "application.properties");

        if (fileObject != null) {
            return fileObject;
        }

        fileObject = filer.getResource(Location.RESOURCE_PATH, null, "application.yml");

        if (fileObject != null) {
            return fileObject;
        }

        fileObject = filer.getResource(Location.RESOURCE_PATH, null, "application.yaml");

        if (fileObject != null) {
            return fileObject;
        }

        return filer.createResource(Location.RESOURCE_PATH, null, "application.properties");
    }

    private void createOfUpdateApplicationProperties(final Map<String, Object> additionalApplicationProperties,
                                                     final FileObject fileObject) {
        if (additionalApplicationProperties.size() > 0) {
            final var properties = new Properties();

            try (final var reader = fileObject.openReader(false)) {
                properties.load(reader);
            } catch (final IOException e) {
                //Ignore exceptions
            }

            additionalApplicationProperties.forEach((key, value) -> properties.putIfAbsent(key, value.toString()));

            try (final var writer = fileObject.openWriter()) {
                properties.store(writer, null);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void createOfUpdateApplicationPropertiesYaml(final Map<String, Object> additionalApplicationProperties,
                                                         final FileObject fileObject) {
        final var yaml = new Yaml();
        final Map<String, Object> yamlMap = new HashMap<>();

        try (final var reader = fileObject.openReader(false)) {
            final Map<String, Object> map = yaml.load(reader);
            yamlMap.putAll(map);
        } catch (final IOException e) {
            //Ignore
        }

        var modified = false;

        for (final Map.Entry<String, Object> entry : additionalApplicationProperties.entrySet()) {
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
                throw new RuntimeException(e);
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

