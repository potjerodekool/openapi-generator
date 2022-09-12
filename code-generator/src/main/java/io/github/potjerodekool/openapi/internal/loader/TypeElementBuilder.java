package io.github.potjerodekool.openapi.internal.loader;

import io.github.potjerodekool.openapi.internal.ast.element.PackageElement;
import io.github.potjerodekool.openapi.internal.ast.element.TypeElement;

public class TypeElementBuilder {

    private final ReflectionTypeElementLoader classLoader;

    public TypeElementBuilder(final ReflectionTypeElementLoader classLoader) {
        this.classLoader = classLoader;
    }

    public TypeElement build(final Class<?> clazz) {
        final TypeElement typeElement;

        if (clazz.isInterface()) {
            typeElement = TypeElement.createInterface(clazz.getSimpleName());
        } else {
            typeElement = TypeElement.createClass(clazz.getSimpleName());
        }

        final var packageOfClazz = clazz.getPackage();

        if (packageOfClazz != null) {
            final var packageElement = PackageElement.create(packageOfClazz.getName());
            typeElement.setEnclosingElement(packageElement);
        } else {
            typeElement.setEnclosingElement(PackageElement.DEFAULT_PACKAGE);
        }

        classLoader.addClass(typeElement);

        final var superClazz = clazz.getSuperclass();

        if (superClazz != null) {
            final var superType = build(superClazz).asType();
            typeElement.setSuperType(superType);
        }

        for (final var anInterface : clazz.getInterfaces()) {
            final var interfaceType = build(anInterface).asType();
            typeElement.addInterface(interfaceType);
        }

        return typeElement;
    }
}
