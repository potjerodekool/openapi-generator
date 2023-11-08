package io.github.potjerodekool.openapi.internal.util;

import io.github.potjerodekool.codegen.model.symbol.ClassSymbol;
import io.github.potjerodekool.codegen.model.type.*;
import io.github.potjerodekool.codegen.model.type.immutable.WildcardType;


// A basic assignable visitor to test if one schema is assignable to another schema that ignores the schema variables.
public class BasicAssignableTypeVisitor implements TypeVisitor<Boolean, TypeMirror> {

    @Override
    public Boolean visit(final TypeMirror t, final TypeMirror type) {
        return false;
    }

    @Override
    public Boolean visit(final TypeMirror t) {
        return false;
    }

    @Override
    public Boolean visitPrimitive(final PrimitiveType t, final TypeMirror type) {
        return false;
    }

    @Override
    public Boolean visitNull(final NullType t, final TypeMirror type) {
        return false;
    }

    @Override
    public Boolean visitArray(final ArrayType t, final TypeMirror type) {
        return false;
    }

    @Override
    public Boolean visitDeclared(final DeclaredType t, final TypeMirror type) {
        if (type instanceof DeclaredType dt) {
            final var classSymbol = (ClassSymbol) t.asElement();
            if (classSymbol.getQualifiedName().contentEquals(((ClassSymbol)dt.asElement()).getQualifiedName())) {
                return true;
            } else {
                final var superclass = classSymbol.getSuperclass();

                if (superclass != null) {
                    if (superclass.accept(this, type)) {
                        return true;
                    }
                }

                return classSymbol.getInterfaces().stream()
                        .anyMatch(anInterface -> anInterface.accept(this, type));
            }
        }

        return false;
    }

    @Override
    public Boolean visitError(final ErrorType t, final TypeMirror type) {
        return false;
    }

    @Override
    public Boolean visitTypeVariable(final TypeVariable t, final TypeMirror type) {
        return false;
    }

    @Override
    public Boolean visitWildcard(final WildcardType t, final TypeMirror type) {
        return false;
    }

    @Override
    public Boolean visitExecutable(final ExecutableType t, final TypeMirror type) {
        return false;
    }

    @Override
    public Boolean visitNoType(final NoType t, final TypeMirror type) {
        return false;
    }

    @Override
    public Boolean visitUnknown(final TypeMirror t, final TypeMirror type) {
        return false;
    }

    @Override
    public Boolean visitUnion(final UnionType t, final TypeMirror type) {
        return false;
    }

    @Override
    public Boolean visitIntersection(final IntersectionType t, final TypeMirror type) {
        return false;
    }

    @Override
    public Boolean visitVarType(final VarType varType,
                                final TypeMirror typeMirror) {
        if (varType.getInterferedType() == null) {
            return false;
        }

        return varType.getInterferedType().accept(this, typeMirror);
    }
}