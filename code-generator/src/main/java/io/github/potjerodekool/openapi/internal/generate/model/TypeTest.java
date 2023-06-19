package io.github.potjerodekool.openapi.internal.generate.model;

import io.github.potjerodekool.codegen.model.type.TypeMirror;
import io.github.potjerodekool.codegen.model.util.type.Types;

import java.util.Set;

public class TypeTest {

    private final Set<TypeMirror> typeList;

    private final Set<TypeMirror> resolvedTypeList;

    private final Types types;

    public TypeTest(final Set<TypeMirror> typeSet,
                    final Set<TypeMirror> resolvedTypeSet,
                    final Types types) {
        this.typeList = typeSet;
        this.resolvedTypeList = resolvedTypeSet;
        this.types = types;
    }

    public boolean test(final TypeMirror type) {
        if (this.typeList.contains(type)) {
            return true;
        }

        try {
            return resolvedTypeList.stream()
                    .anyMatch(rt -> types.isAssignable(rt, type));
        } catch (final Exception e) {
            return false;
        }
    }
}
