package io.github.potjerodekool.openapi.internal.ast;

import io.github.potjerodekool.openapi.Language;
import io.github.potjerodekool.openapi.internal.ast.element.Element;
import io.github.potjerodekool.openapi.internal.ast.element.PackageElement;
import io.github.potjerodekool.openapi.internal.ast.element.TypeElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CompilationUnit implements AstNode {

    private PackageElement packageElement = PackageElement.DEFAULT_PACKAGE;

    private final List<String> imports = new ArrayList<>();

    private final List<Element> elements = new ArrayList<>();

    private final Language language;

    public CompilationUnit(final Language language) {
        this.language = language;
    }

    public Language getLanguage() {
        return language;
    }

    public PackageElement getPackageElement() {
        return packageElement;
    }

    public void setPackageElement(final PackageElement packageElement) {
        this.packageElement = packageElement;
    }

    public List<String> getImports() {
        return Collections.unmodifiableList(imports);
    }

    public void addImport(final String importStr) {
        this.imports.add(importStr);
    }

    public List<Element> getElements() {
        return Collections.unmodifiableList(elements);
    }

    public void addElement(final Element element) {
        this.elements.add(element);
    }

    public TypeElement addClass(final String name) {
        return addClassOrInterface(TypeElement.createClass(name));
    }

    public TypeElement addClass(final String name,
                                final Modifier... modifiers) {
        final var typeElement = addClassOrInterface(TypeElement.createClass(name));
        typeElement.addModifiers(modifiers);
        return typeElement;
    }

    public TypeElement addInterface(final String name) {
        return addClassOrInterface(TypeElement.createInterface(name));
    }

    private TypeElement addClassOrInterface(final TypeElement typeElement) {
        typeElement.setEnclosingElement(packageElement);
        addElement(typeElement);
        return typeElement;
    }

    public <R, P> R accept(final CompilationUnitVisitor<R, P> visitor,
                           final P param) {
        return visitor.visitCompilationUnit(this, param);
    }
}
