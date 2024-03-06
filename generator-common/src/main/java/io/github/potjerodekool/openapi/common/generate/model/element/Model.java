package io.github.potjerodekool.openapi.common.generate.model.element;

import io.github.potjerodekool.openapi.common.generate.model.type.ReferenceType;

import java.util.List;
import java.util.Optional;

public class Model extends AbstractElement<Model> {

    private String packageName;

    private ReferenceType superType;

    private List<String> typeArgs;

    public String getPackageName() {
        return packageName;
    }

    public ReferenceType getSuperType() {
        return superType;
    }

    public List<String> getTypeArgs() {
        return typeArgs;
    }

    public Model packageName(final String packageName) {
        this.packageName = packageName;
        return this;
    }

    public Model superType(final ReferenceType superType) {
        this.superType = superType;
        return this;
    }

    public Model typeArgs(final List<String> typeArgs) {
        this.typeArgs = typeArgs;
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
