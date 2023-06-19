package io.github.potjerodekool.openapi.asserter;

import io.github.potjerodekool.codegen.model.element.ElementFilter;
import io.github.potjerodekool.codegen.model.symbol.ClassSymbol;
import io.github.potjerodekool.codegen.model.util.type.JavaTypes;
import org.junit.jupiter.api.Assertions;

public final class TypeElementAsserter {

    private final JavaTypes types;
    private final ClassSymbol typeElement;

    public TypeElementAsserter(final ClassSymbol typeElement,
                               final JavaTypes types) {
        this.typeElement = typeElement;
        this.types = types;
    }

    public VariableElementAsserter hasField(final String name) {
        final var fieldOptional = ElementFilter.fields(typeElement)
                .filter(field -> field.getSimpleName().contentEquals(name))
                .findFirst();

        if (fieldOptional.isEmpty()) {
            Assertions.fail(String.format("field %s is missing", name));
        }

        final var field = fieldOptional.get();
        return new VariableElementAsserter(field, types);
    }

}
