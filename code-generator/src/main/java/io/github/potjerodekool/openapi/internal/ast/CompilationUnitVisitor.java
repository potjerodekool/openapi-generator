package io.github.potjerodekool.openapi.internal.ast;

public interface CompilationUnitVisitor<R,P> {

    R visitCompilationUnit(CompilationUnit compilationUnit, P param);
}
