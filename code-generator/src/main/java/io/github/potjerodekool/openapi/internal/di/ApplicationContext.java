package io.github.potjerodekool.openapi.internal.di;

import io.github.potjerodekool.openapi.dependency.DependencyChecker;
import io.github.potjerodekool.openapi.internal.di.scope.LazySingletonScopeManager;
import io.github.potjerodekool.openapi.internal.di.scope.ScopeManager;
import io.github.potjerodekool.openapi.internal.di.scope.SingletonScopeManager;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

public class ApplicationContext {

    private final Map<Class<?>, List<ScopeManager<?>>> beansMaps = new HashMap<>();

    private final DependencyChecker dependencyChecker;

    public ApplicationContext(final DependencyChecker dependencyChecker) {
        this.dependencyChecker = dependencyChecker;
    }

    private boolean isBeanOfTypePresent(final Class<?> beanType) {
        return beansMaps.values().stream().flatMap(Collection::stream)
                .anyMatch(sm -> beanType.isAssignableFrom(sm.getBeanType()));
    }

    public <T> void registerBean(final @NonNull T bean) {
        registerBean(bean.getClass(), bean);
    }

    public <T> void registerBeans(final List<BeanDefinition> beanDefinitions) {
        beanDefinitions.stream()
                .filter(beanDefinition -> !this.hasConditions(beanDefinition))
                .forEach(this::doRegister);

        beanDefinitions.stream()
                .filter(this::hasConditions)
                .filter(this::testConditions)
                .forEach(this::doRegister);
    }

    private void doRegister(final BeanDefinition beanDefinition) {
        final var sm = createLazySingletonScopeManager(beanDefinition.className());
        if (sm != null) {
            final var list = this.beansMaps.computeIfAbsent(sm.getBeanType(), (key) -> new ArrayList<>());
            list.add(sm);
        }
    }

    private @Nullable LazySingletonScopeManager<?> createLazySingletonScopeManager(final String className) {
        try {
            final Class<?> clazz = classLoader().loadClass(className);
            return new LazySingletonScopeManager<>(clazz);
        } catch (final ClassNotFoundException e) {
            return null;
        }
    }

    private boolean testConditions(final BeanDefinition beanDefinition) {
        var result = true;
        final var annotationIterator = beanDefinition.annotations().values().iterator();

        while (result && annotationIterator.hasNext()) {
            final var annotation = annotationIterator.next();
            final var annotationType = annotation.annotationType();

            if (isCondition(annotationType)) {
                if (annotationType == ConditionalOnDependency.class) {
                    if (!testConditionalOnDependency((ConditionalOnDependency) annotation)) {
                        result = false;
                    }
                } else if (annotationType == ConditionalOnMissingBean.class) {
                    if (!testConditionOnMissingBean((ConditionalOnMissingBean) annotation, beanDefinition)) {
                        result = false;
                    }
                }
            }
        }

        return result;
    }

    private boolean testConditionalOnDependency(final ConditionalOnDependency conditionalOnDependency) {
        final var groupId = conditionalOnDependency.groupId();
        final var artifactId = conditionalOnDependency.artifactId();
        return this.dependencyChecker.isDependencyPresent(groupId, artifactId);
    }

    private boolean testConditionOnMissingBean(final ConditionalOnMissingBean conditionalOnMissingBean,
                                               final BeanDefinition beanDefinition) {
        try {
            final var clazz = classLoader().loadClass(beanDefinition.className());
            return !isBeanOfTypePresent(clazz);
        } catch (final ClassNotFoundException e) {
            return false;
        }
    }

    private boolean hasConditions(final BeanDefinition beanDefinition) {
        return beanDefinition.annotations().values().stream()
                .anyMatch(annotation -> isCondition(annotation.annotationType()));
    }

    private boolean isCondition(final Class<? extends Annotation> annotation) {
        return annotation.isAnnotationPresent(Conditional.class);
    }

    @SuppressWarnings("argument")
    public <T> void registerBean(final Class<?> beanClass,
                                 final T bean) {
        var list =  beansMaps.computeIfAbsent(beanClass, (key) -> new ArrayList<>());
        list.add(new SingletonScopeManager<>(bean));
    }

    public <T> @NonNull T getBeanOfType(final Class<T> beanType) {
        final var bean = resolveBean(beanType);

        if (bean != null) {
            return bean;
        }

        return createBean(beanType);
    }

    public <T> Set<T> getBeansOfType(final Class<T> beanType) {
        return resolveBeans(beanType);
    }

    private <T> T resolveBean(final Class<T> beanType) {
        final var resolvedBeans = resolveBeans(beanType);

        if (resolvedBeans.isEmpty()) {
            if (!Modifier.isAbstract(beanType.getModifiers())) {
                return createBean(beanType);
            } else {
                throw new DIException(String.format("Failed to resolve bean of %s", beanType.getName()));
            }
        } else if (resolvedBeans.size() > 1) {
            throw new DIException(String.format("Failed to resolve unique bean of %s. Found %s beans", beanType.getName(), resolvedBeans.size()));
        } else {
            return resolvedBeans.iterator().next();
        }
    }

    private <T> Set<T> resolveBeans(final Class<T> beanType) {
        List<@NonNull ScopeManager<?>> list = beansMaps.get(beanType);

        final Set<@NonNull ScopeManager<?>> resolvedBeans;

        if (list != null) {
            resolvedBeans = new HashSet<>(list);
        } else {
            resolvedBeans = new HashSet<>();
        }

        for (final var aClass : beansMaps.keySet()) {
            if (beanType.isAssignableFrom(aClass)) {
                resolvedBeans.addAll(beansMaps.get(aClass));
            }
        }

        return resolvedBeans.stream()
                .map(sm -> (T) sm.get(this))
                .collect(Collectors.toSet());
    }

    private ClassLoader classLoader() {
        final var classLoader = getClass().getClassLoader();
        if (classLoader == null) {
            throw new DIException("No classloader found");
        }
        return classLoader;
    }

    public <T> @NonNull T createBean(final Class<T> beanClass) {
        final var constructorOptional = resolveConstructor(beanClass);
        if (constructorOptional.isEmpty()) {
            throw new DIException(String.format("Failed to resolve constructor for %s", beanClass.getName()));
        }

        return createBean(constructorOptional.get(), beanClass);
    }

    private <T> @NonNull T createBean(final Constructor<T> constructor,
                                      final Class<T> beanClass) {
        final Object[] arguments = resolveArguments(constructor);
        try {
            T bean = constructor.newInstance(arguments);
            invokeInjectOnMethods(bean);
            invokePostConstructor(bean);
            registerBean(beanClass, bean);
            return bean;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object[] resolveArguments(final Executable executable) {
        final Object[] arguments;

        if (executable.getParameterCount() == 0) {
            arguments = new Object[0];
        } else {
            final var parameterTypes = executable.getParameterTypes();
            arguments = new Object[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                final var parameterType = parameterTypes[i];
                arguments[i] = getBeanOfType(parameterType);
            }
        }

        return arguments;
    }

    private void invokeInjectOnMethods(final @NonNull Object bean) {
        Arrays.stream(bean.getClass().getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Inject.class))
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .forEach(injectMethod -> {
                    final var arguments = resolveArguments(injectMethod);
                    try {
                        injectMethod.invoke(bean, arguments);
                    } catch (final Exception e) {
                        throw new DIException("Failed to invoke method", e);
                    }
                });
    }

    private void invokePostConstructor(final @NonNull Object bean) {
        final var postConstructorOptional = Arrays.stream(bean.getClass().getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PostConstruct.class))
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .findFirst();

        postConstructorOptional.ifPresent(pc -> {
            try {
                pc.invoke(bean);
            } catch (final Exception e) {
                throw new DIException(String.format("Failed to invoke postconstructor of %s", bean.getClass().getName()), e);
            }
        });
    }

    private <T> Optional<Constructor<T>> resolveConstructor(final Class<T> clazz) {
        for (final var constructor : clazz.getDeclaredConstructors()) {
            if (constructor.isAnnotationPresent(Inject.class)) {
                return Optional.of((Constructor<T>) constructor);
            }
        }

        try {
            return Optional.of(clazz.getConstructor());
        } catch (final NoSuchMethodException e) {
            //Ignore
        }

        return Optional.empty();
    }

}

