package io.github.potjerodekool.openapi.common.generate.model.element;

import io.github.potjerodekool.codegen.model.util.StringUtils;
import io.github.potjerodekool.codegen.template.model.type.TypeExpr;

public class ModelProperty extends AbstractElement<ModelProperty> {

    private TypeExpr type;

    private Kind kind = Kind.PROPERTY;

    public TypeExpr getType() {
        return type;
    }

    public ModelProperty type(final TypeExpr type) {
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

    @Override
    public Kind getKind() {
        return kind;
    }

    public ModelProperty kind(final Kind kind) {
        this.kind = kind;
        return this;
    }
}
