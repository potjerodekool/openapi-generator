package io.github.potjerodekool.openapi.internal.ast;

import io.github.potjerodekool.openapi.internal.ast.type.Type;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CodeContext {

    private final @Nullable CodeContext parentContext;

    private final AstNode astNode;

    private final Map<String, Type<?>> localVariables = new HashMap<>();

    public CodeContext(final AstNode astNode) {
        this(astNode, null);
    }

    private CodeContext(final AstNode astNode,
                        final @Nullable CodeContext parentContext) {
        this.astNode = astNode;
        this.parentContext = parentContext;
    }

    public CodeContext child() {
        return new CodeContext(astNode, this);
    }

    public CodeContext child(final AstNode astNode) {
        return new CodeContext(astNode, this);
    }

    public AstNode getAstNode() {
        return astNode;
    }

    public @Nullable CodeContext getParentContext() {
        return parentContext;
    }

    public void defineLocalVariable(final Type<?> type,
                                    final String name) {
        localVariables.put(name, type);
    }

    public Optional<? extends Type<?>> resolveLocalVariable(final String name) {
        final var resoledTypeOptional = Optional.ofNullable(localVariables.get(name));

        if (resoledTypeOptional.isPresent()) {
            return resoledTypeOptional;
        }

        if (parentContext == null) {
            return Optional.empty();
        }

        return parentContext.resolveLocalVariable(name);
    }

    public Optional<CompilationUnit> resolveCompilationUnit() {
        if (astNode instanceof CompilationUnit cu) {
            return Optional.of(cu);
        } else if (parentContext != null) {
            return parentContext.resolveCompilationUnit();
        } else {
            return Optional.empty();
        }
    }
}
