package io.github.potjerodekool.openapi.internal.generate.model;

import io.github.potjerodekool.openapi.internal.ast.type.Type;

import java.util.Set;

public class TypeTest {

    private final Set<Type<?>> typeList;

    private final Set<Type<?>> resolvedTypeList;

    public TypeTest(final Set<Type<?>> typeSet,
                    final Set<Type<?>> resolvedTypeSet) {
        this.typeList = typeSet;
        this.resolvedTypeList = resolvedTypeSet;
    }

    public boolean test(final Type<?> type) {
        if (this.typeList.contains(type)) {
            return true;
        }

        try {
            return resolvedTypeList.stream()
                    .anyMatch(rt -> rt.isAssignableBy(type));
        } catch (final Exception e) {
            return false;
        }
    }
}
