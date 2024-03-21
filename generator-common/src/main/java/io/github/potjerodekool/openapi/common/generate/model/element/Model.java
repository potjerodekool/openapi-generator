package io.github.potjerodekool.openapi.common.generate.model.element;

import io.github.potjerodekool.codegen.template.model.type.ClassOrInterfaceTypeExpr;
import io.github.potjerodekool.codegen.template.model.type.TypeVarExpr;

import java.util.List;
import java.util.Optional;

public class Model extends AbstractElement<Model> {

    private String packageName;

    private ClassOrInterfaceTypeExpr superType;

    private List<TypeVarExpr> typeArguments;

    private Kind kind = Kind.CLASS;

    public String getPackageName() {
        return packageName;
    }

    public ClassOrInterfaceTypeExpr getSuperType() {
        return superType;
    }

    public List<TypeVarExpr> getTypeArguments() {
        return typeArguments;
    }

    public Model packageName(final String packageName) {
        this.packageName = packageName;
        return this;
    }

    public Model superType(final ClassOrInterfaceTypeExpr superType) {
        this.superType = superType;
        return this;
    }

    public Model typeArguments(final List<TypeVarExpr> typeArguments) {
        this.typeArguments = typeArguments;
        return this;
    }

    public List<ModelProperty> getProperties() {
        return getEnclosedElements().stream()
                .filter(ModelProperty.class::isInstance)
                .map(ModelProperty.class::cast).toList();
    }

    public List<Model> getModels() {
        return getEnclosedElements().stream()
                .filter(Model.class::isInstance)
                .map(Model.class::cast).toList();
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

    @Override
    public Kind getKind() {
        return kind;
    }

    public Model kind(final Kind kind) {
        this.kind = kind;
        return this;
    }
}
