package io.github.potjerodekool.openapi.asserter;

import io.github.potjerodekool.codegen.model.symbol.VariableSymbol;
import io.github.potjerodekool.codegen.model.type.TypeMirror;
import io.github.potjerodekool.codegen.model.util.type.JavaTypes;
import org.junit.jupiter.api.Assertions;

public class VariableElementAsserter {

    private final VariableSymbol variableElement;
    private final JavaTypes types;

    public VariableElementAsserter(final VariableSymbol variableElement,
                                   final JavaTypes types) {
        this.variableElement = variableElement;
        this.types = types;
    }

    public void checkType(final TypeMirror type) {
        final var variableType = variableElement.asType();
        if (!types.isSameType(type, variableType)) {
            Assertions.fail(String.format("Expected type %s but was %s", type, variableType));
        }
    }
}
