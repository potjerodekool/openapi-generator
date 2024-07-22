package io.github.potjerodekool.openapi.common.generate.model;

import io.github.potjerodekool.codegen.template.model.type.ClassOrInterfaceTypeExpr;
import io.github.potjerodekool.openapi.common.generate.model.element.AbstractElement;
import io.github.potjerodekool.openapi.common.generate.model.element.Model;
import io.github.potjerodekool.openapi.common.generate.model.element.ModelProperty;
import org.junit.jupiter.api.Assertions;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class ModelAsserter {

    private final Model model;

    public ModelAsserter(final Model model) {
        Assertions.assertNotNull(model);
        this.model = model;
    }

    public ModelAsserter assertProperty(final String name,
                                        final Consumer<PropertyAsserter> consumer) {
        model.getProperty(name).ifPresentOrElse(
                it -> consumer.accept(new PropertyAsserter(it)),
                () -> fail("missing property" + name)
        );
        return this;
    }

    public ModelAsserter assertEnumConstants(final String... names) {
        final var missing = Arrays.stream(names)
                        .collect(Collectors.toSet());

        missing.removeAll(
                model.getEnumConstants().stream()
                        .map(AbstractElement::getSimpleName)
                        .collect(Collectors.toSet())
        );

        if (!missing.isEmpty()) {
            fail(String.format("missing %s", missing));
        }

        return this;
    }

    public static class PropertyAsserter {

        private final ModelProperty modelProperty;

        PropertyAsserter(final ModelProperty modelProperty) {
            this.modelProperty = modelProperty;
        }

        public PropertyAsserter assertType(final String typeName) {
            final var type = modelProperty.getType();
            Assertions.assertNotNull(type, "expected " + typeName + " but got null");

            if (type instanceof ClassOrInterfaceTypeExpr classOrInterfaceTypeExpr) {
                assertEquals(
                        typeName,
                        classOrInterfaceTypeExpr.getName()
                );

            }

            return this;
        }
    }

}
