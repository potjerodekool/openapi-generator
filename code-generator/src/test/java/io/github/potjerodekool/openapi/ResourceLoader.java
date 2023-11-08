package io.github.potjerodekool.openapi;

import java.io.IOException;

public final class ResourceLoader {

    private ResourceLoader() {
    }

    public static String loadAsString(final String name) {
        try (final var inputStream = ResourceLoader.class.getClassLoader().getResourceAsStream(name)) {
            return new String(inputStream.readAllBytes());
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String loadAsString(final String name,
                                      final boolean fixLines) {
        try (final var inputStream = ResourceLoader.class.getClassLoader().getResourceAsStream(name)) {
            var result = new String(inputStream.readAllBytes());

            if (fixLines) {
                result = result.replace("\r", "");
            }
            return result;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
