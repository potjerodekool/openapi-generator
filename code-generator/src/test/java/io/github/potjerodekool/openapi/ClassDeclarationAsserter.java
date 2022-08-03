package io.github.potjerodekool.openapi;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import org.junit.jupiter.api.Assertions;

import java.util.function.Consumer;

public final class ClassDeclarationAsserter {

    private final ClassOrInterfaceDeclaration clazz;

    public ClassDeclarationAsserter(final ClassOrInterfaceDeclaration clazz) {
        this.clazz = clazz;
    }

    public static ClassDeclarationAsserter create(final ClassOrInterfaceDeclaration clazz) {
        return new ClassDeclarationAsserter(clazz);
    }

    public ClassDeclarationAsserter assertField(final String name,
                                                final Consumer<FieldAsserter> consumer) {
        final var fieldOptional = clazz.getFieldByName(name);
        Assertions.assertTrue(fieldOptional.isPresent(), String.format("field is %s missing", name));
        consumer.accept(new FieldAsserter(fieldOptional.get()));
        return this;
    }
}
