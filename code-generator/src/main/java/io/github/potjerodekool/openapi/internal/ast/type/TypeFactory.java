package io.github.potjerodekool.openapi.internal.ast.type;

import io.github.potjerodekool.openapi.internal.ast.element.AnnotationMirror;
import io.github.potjerodekool.openapi.internal.ast.element.PackageElement;
import io.github.potjerodekool.openapi.internal.ast.element.TypeElement;
import io.github.potjerodekool.openapi.internal.ast.type.java.JavaDeclaredType;
import io.github.potjerodekool.openapi.internal.util.QualifiedName;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.lang.model.element.ElementKind;
import java.util.List;
import java.util.Set;

public class TypeFactory {

    private TypeFactory() {
    }

    public static DeclaredType createDeclaredType(final TypeElement typeElement,
                                                  final List<AnnotationMirror> annotations,
                                                  final @Nullable List<? extends Type<?>> typeArguments,
                                                  final boolean isNullable) {
        return new JavaDeclaredType(
                typeElement,
                annotations,
                typeArguments,
                isNullable
        );
    }

    public static DeclaredType createDeclaredType(final TypeElement typeElement,
                                                  final boolean isNullable) {
        return new JavaDeclaredType(typeElement, isNullable);
    }

    public static DeclaredType createDeclaredType(final ElementKind elementKind,
                                                  final String className) {
        final var qualifiedName = QualifiedName.from(className);
        final var packageElement = PackageElement.create(qualifiedName.packageName());
        final var typeElement = TypeElement.create(elementKind, List.of(), Set.of(), qualifiedName.simpleName());
        typeElement.setEnclosingElement(packageElement);
        return new JavaDeclaredType(typeElement, true);
    }
}
