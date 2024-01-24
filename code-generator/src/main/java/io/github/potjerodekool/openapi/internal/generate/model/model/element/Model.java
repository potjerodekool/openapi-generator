package io.github.potjerodekool.openapi.internal.generate.model.model.element;

import java.util.List;
import java.util.Optional;

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

    public Optional<ModelProperty> getProperty(final String name) {
        return getProperties().stream()
                .filter(it -> it.getSimpleName().equals(name))
                .findFirst();
    }

    @Override
    protected Model self() {
        return this;
    }
}
