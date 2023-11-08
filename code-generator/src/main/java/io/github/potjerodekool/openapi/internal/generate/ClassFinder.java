package io.github.potjerodekool.openapi.internal.generate;

import io.github.potjerodekool.codegen.model.element.Name;
import io.github.potjerodekool.codegen.model.element.TypeElement;
import io.github.potjerodekool.codegen.model.symbol.AbstractSymbol;
import io.github.potjerodekool.codegen.model.symbol.ModuleSymbol;
import io.github.potjerodekool.codegen.model.symbol.PackageSymbol;
import io.github.potjerodekool.codegen.model.util.Elements;
import io.github.potjerodekool.codegen.resolve.Scope;

public class ClassFinder {

    private final Elements elements;

    public ClassFinder(final Elements elements) {
        this.elements = elements;
    }

    public TypeElement findClass(final Name name,
                                 final Scope scope) {
        final var resolved = scope.resolveSymbol(name);

        if (resolved.isPresent()) {
            return (TypeElement) resolved.get();
        } else {
            final var module = resolveModule(scope);
            return elements.getTypeElement(module, name);
        }
    }

    private ModuleSymbol resolveModule(final Scope scope) {
        final var owner = scope.owner;

        if (owner instanceof PackageSymbol packageSymbol) {
            return packageSymbol.module;
        } else {
            final var enclosingElement = (AbstractSymbol) owner.getEnclosingElement();

            if (enclosingElement != null) {
                return resolveModule(enclosingElement.members());
            } else {
                return null;
            }
        }
    }
}
