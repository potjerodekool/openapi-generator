package io.github.potjerodekool.openapi.internal.generate.model;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.model.type.TypeMirror;
import io.github.potjerodekool.codegen.model.util.Elements;
import io.github.potjerodekool.codegen.model.util.type.Types;
import io.github.potjerodekool.openapi.internal.di.Bean;
import jakarta.inject.Inject;

import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;

@Bean
public class TypeTestLoader {

    private final Properties properties;

    private final Elements elements;

    private final Types types;

    @Inject
    public TypeTestLoader(final Environment environment) {
        this.properties = new Properties();
        this.elements = environment.getElementUtils();
        this.types = environment.getTypes();

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
        final var typeSet = new HashSet<TypeMirror>();
        final var resolvedTypeSet = new HashSet<TypeMirror>();

        if (property != null) {
            for (final var className : property.split(",")) {
                if (className.startsWith("+")) {
                    try {
                        final var typeElement = elements.getTypeElement(className.substring(1));

                        if (typeElement != null) {
                            resolvedTypeSet.add(types.getDeclaredType(typeElement));
                        }
                    } catch (final Exception e) {
                        //Ignore
                    }
                } else {
                    final var typeElement = elements.getTypeElement(className);
                    if (typeElement != null) {
                        typeSet.add(types.getDeclaredType(typeElement));
                    }
                }
            }
        }

        return new TypeTest(typeSet, resolvedTypeSet, types);
    }
}
