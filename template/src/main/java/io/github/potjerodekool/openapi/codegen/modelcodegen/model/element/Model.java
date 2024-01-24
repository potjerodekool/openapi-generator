package io.github.potjerodekool.openapi.codegen.modelcodegen.model.element;

import java.util.List;

public class Model extends AbstractElement<Model> {

    private String packageName;

    public String getPackageName() {
        return packageName;
    }

    public Model packageName(final String packageName) {
        this.packageName = packageName;
        return this;
    }

    public List<ModelProperty> getProperties() {
        return getEnclosedElements().stream()
                .filter(ModelProperty.class::isInstance)
                .map(ModelProperty.class::cast).toList();
    }

    @Override
    protected Model self() {
        return this;
    }
}
