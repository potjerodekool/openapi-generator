package io.github.potjerodekool.openapi.internal.generate;

import io.github.potjerodekool.codegen.Diagnostic;
import io.github.potjerodekool.codegen.DiagnosticListener;

public class NoOpDiagnosticListener implements DiagnosticListener<Object> {

    private static final NoOpDiagnosticListener INSTANCE = new NoOpDiagnosticListener();

    public static <S> DiagnosticListener<S> getInstance() {
        return (DiagnosticListener<S>) INSTANCE;
    }

    @Override
    public void report(final Diagnostic<?> diagnostic) {

    }
}
