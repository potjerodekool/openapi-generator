package io.github.potjerodekool.openapi.internal.di.bean;

import io.github.potjerodekool.openapi.common.dependency.*;
import io.github.potjerodekool.openapi.internal.di.DIException;
import io.github.potjerodekool.openapi.internal.di.conditiontest.ConditionalOnDependencyTest;
import io.github.potjerodekool.openapi.internal.di.conditiontest.ConditionalOnMissingBeanTest;
import io.github.potjerodekool.openapi.internal.di.conditiontest.ConditionalTest;
import jakarta.inject.Inject;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

public class DefaultApplicationContext implements ApplicationContext {

    private final Map<Class<?>, List<ScopeManager<?>>> beansMaps = new HashMap<>();

    private final Map<Class<?>, ConditionalTest<?>> conditionTests = new HashMap<>();

    public DefaultApplicationContext(final DependencyChecker dependencyChecker) {
        add(ConditionalOnDependency.class, new ConditionalOnDependencyTest(dependencyChecker));
        add(ConditionalOnMissingBean.class, new ConditionalOnMissingBeanTest(this));
    }

    private <A extends Annotation> void add(final Class<A> annotationType,
                                            final ConditionalTest<A> test) {
        conditionTests.put(annotationType, test);
    }

    @Override
    public boolean isBeanOfTypePresent(final Class<?> beanType) {
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
        final var sm = createLazySingletonScopeManager(beanDefinition);
        if (sm != null) {
            final var list = this.beansMaps.computeIfAbsent(sm.getBeanType(), (key) -> new ArrayList<>());
            list.add(sm);
        }
    }

    private @Nullable LazySingletonScopeManager<?> createLazySingletonScopeManager(final BeanDefinition beanDefinition) {
        return new LazySingletonScopeManager<>(beanDefinition);
    }

    private boolean testConditions(final BeanDefinition beanDefinition) {
        var result = true;
        final var annotationIterator = beanDefinition.getAnnotations().values().iterator();

        while (result && annotationIterator.hasNext()) {
            final Annotation annotation = annotationIterator.next();
            final var annotationType = annotation.annotationType();

            if (isCondition(annotationType)) {
                final ConditionalTest<Annotation> test = (ConditionalTest<Annotation>) conditionTests.get(annotationType);
                if (test != null) {
                    if(!test.test(annotation, beanDefinition)) {
                        result = false;
                    }
                }
            }
        }

        return result;
    }

    private boolean hasConditions(final BeanDefinition beanDefinition) {
        return beanDefinition.getAnnotations().values().stream()
                .anyMatch(annotation -> isCondition(annotation.annotationType()));
    }

    private boolean isCondition(final Class<? extends Annotation> annotation) {
        return annotation.isAnnotationPresent(Conditional.class);
    }

    @SuppressWarnings("argument")
    public <T> void registerBean(final Class<?> beanClass,
                                 final T bean) {
        var list = beansMaps.computeIfAbsent(beanClass, (key) -> new ArrayList<>());
        list.add(new SingletonScopeManager<>(bean));
    }

    private <T> T getBeanOfType(final Class<T> beanType) {
        return resolveBean(beanType);
    }

    @Override
    public <T> Set<T> getBeansOfType(final Class<T> beanType) {
        return resolveBeans(beanType);
    }

    private <T> T resolveBean(final Class<T> beanType) {
        final var resolvedBeans = resolveBeans(beanType);

        if (resolvedBeans.isEmpty()) {
            throw new DIException(String.format("Failed to resolve bean of %s", beanType.getName()));
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

    <T> @NonNull T createBean(final BeanDefinition beanDefinition) {
        final Object[] arguments = resolveArguments(beanDefinition.getBeanMethod());
        try {
            final var instance = beanDefinition.getAutoConfigInstance();
            final var method = beanDefinition.getBeanMethod();
            T bean = (T) method.invoke(instance, arguments);
            registerBean(bean.getClass(), bean);
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
            return Arrays.stream(executable.getParameterTypes())
                    .map(this::getBeanOfType)
                    .toArray();
        }

        return arguments;
    }

    private <T> Optional<Constructor<T>> resolveConstructor(final Class<T> clazz) {
        final var declaredConstructors = (Constructor<T>[]) clazz.getDeclaredConstructors();

        return Optional.ofNullable(
                Arrays.stream(declaredConstructors)
                .filter(constructor -> constructor.isAnnotationPresent(Inject.class))
                .findFirst()
                .orElseGet(() -> {
                    try {
                        return clazz.getConstructor();
                    } catch (final NoSuchMethodException e) {
                        return null;
                    }
                })
        );
    }

}
