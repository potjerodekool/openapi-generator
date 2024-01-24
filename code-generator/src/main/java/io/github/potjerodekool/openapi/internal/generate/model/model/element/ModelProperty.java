package io.github.potjerodekool.openapi.internal.generate.model.model.element;

import io.github.potjerodekool.codegen.model.util.StringUtils;
import io.github.potjerodekool.openapi.internal.generate.model.model.type.Type;

public class ModelProperty extends AbstractElement<ModelProperty> {

    private Type type;

    public Type getType() {
        return type;
    }

    public ModelProperty type(final Type type) {
        this.type = type;
        return this;
    }

    @Override
    protected ModelProperty self() {
        return this;
    }

    public String getGetterName() {
        return "get" + StringUtils.firstUpper(getSimpleName());
    }

    public String getSetterName() {
        return "set" + StringUtils.firstUpper(getSimpleName());
    }

    public String getBuilderSetterName() {
        return getSimpleName();
    }
}
