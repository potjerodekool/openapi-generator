package io.github.potjerodekool.openapi.internal.generate.model;

import io.github.potjerodekool.openapi.internal.ast.TypeUtils;
import io.github.potjerodekool.openapi.internal.ast.type.Type;
import io.github.potjerodekool.openapi.internal.di.Bean;
import jakarta.inject.Inject;

import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;

@Bean
public class TypeTestLoader {

    private final Properties properties;

    private final TypeUtils typeUtils;

    @Inject
    public TypeTestLoader(final TypeUtils typeUtils) {
        this.properties = new Properties();
        this.typeUtils = typeUtils;
        final var classLoader = TypeTestLoader.class.getClassLoader();

        if (classLoader != null) {
            try (final var inputStream = classLoader.getResourceAsStream("validator.properties")) {
                if (inputStream != null) {
                    this.properties.load(inputStream);
                }
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public TypeTest loadTypeTest(final String key) {
        final var property = this.properties.getProperty(key);
        final var typeSet = new HashSet<Type<?>>();
        final var resolvedTypeSet = new HashSet<Type<?>>();

        if (property != null) {
            for (final var className : property.split(",")) {
                if (className.startsWith("+")) {
                    try {
                        resolvedTypeSet.add(typeUtils.createDeclaredType(className.substring(1)));
                    } catch (final Exception e) {
                        //Ignore
                    }
                } else {
                    typeSet.add(typeUtils.createDeclaredType(className));
                }
            }
        }

        return new TypeTest(typeSet, resolvedTypeSet);
    }
}
