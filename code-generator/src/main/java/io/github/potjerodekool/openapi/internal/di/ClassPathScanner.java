package io.github.potjerodekool.openapi.internal.di;

import io.github.potjerodekool.openapi.common.util.StreamUtils;
import io.github.potjerodekool.openapi.internal.di.bean.BeanDefinition;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.ClassReader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import static io.github.potjerodekool.openapi.common.util.StreamUtils.tryAction;

public class ClassPathScanner {

    private static final PathMatcher CLASS_FILE_PATH_MATCHER = FileSystems.getDefault().getPathMatcher("glob:**.class");

    private ClassPathScanner() {
    }

    public static List<BeanDefinition> scan() {
        final var codeSource = ClassPathScanner.class.getProtectionDomain().getCodeSource();

        if (codeSource == null) {
            return List.of();
        }

        final var location = codeSource.getLocation();
        final var beanDefinitions = new ArrayList<BeanDefinition>();

        if ("file".equals(location.getProtocol())) {
            final var file = location.getFile();
            beanDefinitions.addAll(file.endsWith(".jar")
                    ? scanArchive(location)
                    : scanDirectory(location)
            );
        }
        return beanDefinitions;
    }

    private static List<BeanDefinition> scanArchive(final URL location) {
        final var beanDefinitions = new ArrayList<BeanDefinition>();

        try (final ZipFile zipFile = new ZipFile(new File(location.toURI()))) {
            StreamUtils.of(zipFile.entries().asIterator())
                    .filter(it -> it.getName().endsWith(".class"))
                    .forEach(entry -> tryAction(() -> {
                        final var inputStream = zipFile.getInputStream(entry);
                        final var beanDefinition = read(inputStream.readAllBytes());

                        if (beanDefinition != null) {
                            beanDefinitions.add(beanDefinition);
                        }
                    }));
        } catch (final Exception e) {
            //Ignore exceptions
        }

        return beanDefinitions;
    }

    private static List<BeanDefinition> scanDirectory(final URL location) {
        final var beanDefinitions = new ArrayList<BeanDefinition>();

        try (final var stream = Files.walk(Paths.get(location.toURI()))) {
            stream
                    .filter(CLASS_FILE_PATH_MATCHER::matches)
                    .forEach(classFile -> {
                        try {
                            final var bytes = Files.readAllBytes(classFile);
                            final var beanDefinition = read(bytes);

                            if (beanDefinition != null) {
                                beanDefinitions.add(beanDefinition);
                            }
                        } catch (final IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        return beanDefinitions;
    }

    private static @Nullable BeanDefinition read(final byte[] data) {
        final var classReader = new ClassReader(data);
        final var classVisitor = new SimpleClassVisitor();
        classReader.accept(classVisitor, 0);
        return classVisitor.getBeanDefinition();
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
