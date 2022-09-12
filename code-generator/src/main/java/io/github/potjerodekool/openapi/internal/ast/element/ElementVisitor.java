package io.github.potjerodekool.openapi.internal.ast.element;

public interface ElementVisitor<R,P> {

    R visitUnknown(Element element, P param);

    R visitTypeElement(TypeElement typeElement, P param);

    R visitExecutableElement(MethodElement methodElement, P param);

    R visitPackageElement(PackageElement packageElement, P param);

    R visitVariableElement(VariableElement variableElement, P param);
}
