package io.github.potjerodekool.openapi;

import com.github.javaparser.ast.body.FieldDeclaration;

public class FieldAsserter {

    private final FieldDeclaration fieldDeclaration;

    public FieldAsserter(final FieldDeclaration fieldDeclaration) {
        this.fieldDeclaration = fieldDeclaration;
    }

    public FieldDeclaration fieldDeclaration() {
        return fieldDeclaration;
    }
}
