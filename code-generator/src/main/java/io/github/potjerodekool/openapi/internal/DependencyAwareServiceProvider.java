package io.github.potjerodekool.openapi.internal;

import io.github.potjerodekool.openapi.internal.util.Utils;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DependencyAwareServiceProvider<S> implements Function<String, S> {

    private final Map<Class<?>, Object> dependencies;

    public DependencyAwareServiceProvider() {
        this(Map.of());
    }

    public DependencyAwareServiceProvider(final Map<Class<?>, Object> dependencies) {
        this.dependencies = dependencies;
    }

    public static <S> DependencyAwareServiceProvider<S> create(final List<Object> dependencies) {
        final Map<Class<?>, Object> map = dependencies.stream()
                .collect(Collectors.toMap(
                        Object::getClass,
                        Function.identity()
                ));
        return new DependencyAwareServiceProvider<>(map);
    }

    @Override
    public S apply(final String className) {
        return get(loadClass(className));
    }

    public S get(final Class<S> serviceClass) {
        final var constructor = resolveConstructor(serviceClass);

        if (constructor.getParameterCount() == 0) {
            try {
                return constructor.newInstance();
            } catch (final Exception e) {
                throw new ServiceLoaderException("Failed to create instance of service", e);
            }
        } else {
            final var args = new Object[constructor.getParameterCount()];
            final var parameterTypes = constructor.getParameterTypes();

            for (int i = 0; i < parameterTypes.length; i++) {
                final var parameterType = parameterTypes[i];
                args[i] = resolveArgument(parameterType);
            }

            try {
                return constructor.newInstance(args);
            } catch (final Exception e) {
                throw new ServiceLoaderException("Failed to create instance of service", e);
            }
        }
    }

    private Object resolveArgument(final Class<?> parameterType) {
        final var dependency = this.dependencies.get(parameterType);

        if (dependency != null) {
            return dependency;
        }

        return this.dependencies.values().stream()
                .filter(d -> parameterType.isAssignableFrom(d.getClass()))
                .findFirst()
                .orElseThrow(() -> new ServiceLoaderException("Failed to resolve parameter for service"));
    }

    private Constructor<S> resolveConstructor(final Class<S> serviceClass) {
        Constructor<S> selectedConstructor = null;

        for (final Constructor<?> constructor : serviceClass.getConstructors()) {
            if (selectedConstructor == null || constructor.getParameterCount() > selectedConstructor.getParameterCount()) {
                selectedConstructor = (Constructor<S>) constructor;
            }
        }

        if (selectedConstructor == null) {
            throw new ServiceLoaderException(String.format("failed to resolve constructor for serviceclass %s",
                    serviceClass.getName()));
        }

        return selectedConstructor;
    }

    private Class<S> loadClass(final String className) {
        try {
            return (Class<S>) Utils.requireNonNull(getClass().getClassLoader()).loadClass(className);
        } catch (final ClassNotFoundException e) {
            throw new ServiceLoaderException(String.format("Failed to load serviceclass %s", className), e);
        }
    }
}
