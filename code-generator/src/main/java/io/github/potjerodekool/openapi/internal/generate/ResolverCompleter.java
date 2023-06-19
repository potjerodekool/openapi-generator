package io.github.potjerodekool.openapi.internal.generate;

import io.github.potjerodekool.codegen.model.Completer;
import io.github.potjerodekool.codegen.model.symbol.AbstractSymbol;

public class ResolverCompleter implements Completer {

    private final FullResolver fullResolver;
    private boolean isComplete = false;

    public ResolverCompleter(final FullResolver fullResolver) {
        this.fullResolver = fullResolver;
    }

    @Override
    public boolean isComplete() {
        return isComplete;
    }

    @Override
    public void complete(final AbstractSymbol<?> symbol) {
        //symbol.accept(fullResolver, null);
        throw new UnsupportedOperationException("" + symbol);
    }
}
