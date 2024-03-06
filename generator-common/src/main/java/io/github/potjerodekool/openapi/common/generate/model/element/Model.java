package io.github.potjerodekool.openapi.common.generate.model.element;

import io.github.potjerodekool.openapi.common.generate.model.type.ReferenceType;
import io.github.potjerodekool.openapi.common.generate.model.type.TypeVariable;

import java.util.List;
import java.util.Optional;

public class Model extends AbstractElement<Model> {

    private String packageName;

    private ReferenceType superType;

    private List<TypeVariable> typeArguments;

    public String getPackageName() {
        return packageName;
    }

    public ReferenceType getSuperType() {
        return superType;
    }

    public List<TypeVariable> getTypeArguments() {
        return typeArguments;
    }

    public Model packageName(final String packageName) {
        this.packageName = packageName;
        return this;
    }

    public Model superType(final ReferenceType superType) {
        this.superType = superType;
        return this;
    }

    public Model typeArguments(final List<TypeVariable> typeArguments) {
        this.typeArguments = typeArguments;
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
