package io.github.potjerodekool.openapi.internal.di;

import io.github.potjerodekool.openapi.internal.di.bean.BeanDefinition;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClassPathScanner {

    private static final PathMatcher CLASS_FILE_PATH_MATCHER = FileSystems.getDefault().getPathMatcher("glob:**.class");

    private ClassPathScanner() {
    }

    public static List<BeanDefinition> scan() {
        final var beanDefinitions = new ArrayList<BeanDefinition>();
        try {
            final var resources = ClassPathScanner.getClassLoader()
                    .getResources("META-INF/io.github.potjerodekool.openapi.common.autoconfig.AutoConfiguration");

            final var iterator = resources.asIterator();

            while (iterator.hasNext()) {
                final var resource = iterator.next();

                try (final var inputStream = resource.openStream()) {
                    final var lines = new String(inputStream.readAllBytes()).split("\n");

                    Arrays.stream(lines)
                            .filter(line -> !line.trim().startsWith("#"))
                            .forEach(className -> {
                                beanDefinitions.addAll(loadConfiguration(className));
                            });
                }
            }

        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        return beanDefinitions;
    }

    private static List<BeanDefinition> loadConfiguration(final String className) {
        try (final var inputStream = getClassLoader().getResourceAsStream(
                className.replace('.', '/') + ".class"
        )) {
            final var instance = getClassLoader().loadClass(className).getDeclaredConstructor().newInstance();
            final var data = inputStream.readAllBytes();
            final var classReader = new ClassReader(data);
            final var reader = new AutoConfigReader(instance);
            classReader.accept(reader, 0);
            return reader.getBeanDefinitions();
        } catch (final Exception e) {
            //Ignore
            e.printStackTrace();
        }
        return List.of();
    }

    protected static ClassLoader getClassLoader() {
        final var classLoader = ClassPathScanner.class.getClassLoader();

        if (classLoader != null) {
            return classLoader;
        } else {
            return ClassLoader.getSystemClassLoader();
        }
    }
}
