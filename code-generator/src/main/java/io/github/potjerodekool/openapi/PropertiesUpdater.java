package io.github.potjerodekool.openapi;

import io.github.potjerodekool.codegen.io.FileObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class PropertiesUpdater {

    private PropertiesUpdater() {
    }

    public static void update(final FileObject fileObject,
                              final Map<String, Object> updates) throws IOException {
        try (final var inputStream = fileObject.openInputStream()) {
            final var reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.ISO_8859_1));

            final Path tempFile =  Files.createTempFile(null, null);

            try {
                writeTempFile(reader, tempFile, updates);
                updateProperties(tempFile, fileObject);
            } finally {
                Files.delete(tempFile);
            }
        }
    }

    private static void writeTempFile(final BufferedReader reader,
                                      final Path tempFile,
                                      final Map<String, Object> updates) throws IOException {
        final var remainingUpdates = new HashMap<>(updates);

        try (var tmpWriter = Files.newBufferedWriter(tempFile, StandardCharsets.ISO_8859_1)) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) {
                    tmpWriter.write(line);
                    tmpWriter.newLine();
                } else {
                    final var keyValue = line.split("=", 2);
                    final String key = keyValue[0];
                    final Object value;

                    if (remainingUpdates.containsKey(key)) {
                        value = remainingUpdates.get(key);
                        remainingUpdates.remove(key);
                    } else {
                        value = keyValue[0];
                    }

                    if (!key.trim().isEmpty()) {
                        tmpWriter.write(key + "=" + value);
                        tmpWriter.newLine();
                    } else {
                        tmpWriter.write(line);
                        tmpWriter.newLine();
                    }
                }
            }

            for (final Map.Entry<String, Object> entry : remainingUpdates.entrySet()) {
                tmpWriter.write(entry.getKey() + "= " + entry.getValue());
                tmpWriter.newLine();
            }
        }
    }

    private static void updateProperties(final Path tempFile,
                                         final FileObject fileObject) throws IOException {
        try (final var inputStream = Files.newInputStream(tempFile);
             final var outputStream = fileObject.openOutputStream()) {
            final var bytes = inputStream.readAllBytes();
            outputStream.write(bytes);
        }
    }

}
