package io.github.potjerodekool.openapi.internal;

import io.github.potjerodekool.openapi.internal.util.Utils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ServiceLoader<S> {

    private static final Function<String, ?> DEFAULT_SERVICE_PROVIDER = new DefaultServiceProvider<>();

    private final Class<S> serviceClass;

    private final Function<String, S> serviceProvider;

    private final Set<String> classNames = new HashSet<>();

    private boolean resolveClassNames = true;

    private ServiceLoader(final Class<S> serviceClass,
                          final Function<String, S> serviceProvider) {
        this.serviceClass = serviceClass;
        this.serviceProvider = serviceProvider;
    }

    public static <S> ServiceLoader<S> load(final Class<S> serviceClass,
                                            final Function<String, S> serviceProvider) {
        return new ServiceLoader<>(serviceClass, serviceProvider);
    }

    public static <S> ServiceLoader<S> load(final Class<S> serviceClass) {
        return new ServiceLoader<>(serviceClass, (Function<String, S>) DEFAULT_SERVICE_PROVIDER);
    }

    public Iterator<S> iterator() {
        if (resolveClassNames) {
            doLoad(serviceClass);
            resolveClassNames = false;
        }

        return new ServiceIterator<>(classNames.iterator(), serviceProvider);
    }

    public Stream<S> stream() {
        return StreamSupport.stream(
                Utils.spliterator(iterator()),
                false
        );
    }

    public void forEach(final Consumer<S> consumer) {
        stream().forEach(consumer);
    }

    private void doLoad(final Class<?> serviceClass) {
        Iterator<URL> iterator;

        try {
            iterator = Utils.requireNonNull(getClass().getClassLoader()).getResources("META-INF/services/" + serviceClass.getName())
                    .asIterator();
        } catch (final IOException e) {
            return;
        }

        final Set<String> classNames = new HashSet<>();

        while (iterator.hasNext()) {
            final var url = iterator.next();
            classNames.addAll(parse(url));
        }

        this.classNames.addAll(classNames);
    }

    private Set<String> parse(final URL url) {
        final var classNames = new HashSet<String>();

        try (final var inputStream = url.openStream()) {
            final var reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;

            while ((line = readLine(reader)) != null) {
                if (!line.startsWith("#")) {
                    classNames.add(line);
                }
            }
        } catch (final IOException e) {
            //Ignore
        }

        return classNames;
    }

    private @Nullable String readLine(final BufferedReader reader) throws IOException {
        String line = reader.readLine();
        return line != null
                ? line.trim().replace("\t", "")
                : null;
    }
}

class ServiceIterator<S> implements Iterator<S> {

    private final Iterator<String> classNames;
    private final Function<String, S> serviceProvider;

    ServiceIterator(final Iterator<String> classNames,
                    final Function<String, S> serviceProvider) {
        this.classNames = classNames;
        this.serviceProvider = serviceProvider;
    }

    @Override
    public boolean hasNext() {
        return classNames.hasNext();
    }

    @Override
    public S next() {
        return serviceProvider.apply(classNames.next());
    }
}

class DefaultServiceProvider<S> implements Function<String, S> {

    @Override
    public S apply(final String className) {
        try {
            final var clazz = (Class<S>) Utils.requireNonNull(getClass().getClassLoader()).loadClass(className);
            return clazz.getConstructor().newInstance();
        } catch (final Exception e) {
            throw new ServiceLoaderException(String.format("Failed to create service %s", className));
        }
    }
}