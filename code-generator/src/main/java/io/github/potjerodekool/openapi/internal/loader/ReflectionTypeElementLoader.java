package io.github.potjerodekool.openapi.internal.loader;

import io.github.potjerodekool.openapi.internal.ast.element.TypeElement;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ReflectionTypeElementLoader implements TypeElementLoader {

    private final ClassLoader classLoader;

    private final Map<String, TypeElement> classes = new HashMap<>();

    public ReflectionTypeElementLoader(final ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public TypeElement loadTypeElement(final String className) throws ClassNotFoundException {
        TypeElement typeElement = findClass(className);

        if (typeElement != null) {
            return typeElement;
        }
        final var clazz = resolveClass(className);
        return new TypeElementBuilder(this).build(clazz);

    }

    private @Nullable TypeElement findClass(final String className) {
        return classes.get(className);
    }

    void addClass(final TypeElement typeElement) {
        this.classes.put(typeElement.getQualifiedName(), typeElement);
    }

    private Class<?> resolveClass(final String className) throws ClassNotFoundException {
        return classLoader.loadClass(className);
    }
}
