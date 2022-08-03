package io.github.potjerodekool.openapi;

import com.github.javaparser.ast.CompilationUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CompilationUnitAsserter {

    public static ClassDeclarationAsserter assertClass(final CompilationUnit cu,
                                                       final String name) {
        final var clazzOptional = cu.getClassByName(name);
        assertTrue(clazzOptional.isPresent(), String.format("class %s is absent", name));
        return new ClassDeclarationAsserter(clazzOptional.get());
    }
}
