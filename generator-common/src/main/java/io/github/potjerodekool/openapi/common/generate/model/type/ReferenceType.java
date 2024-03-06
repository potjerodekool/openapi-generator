package io.github.potjerodekool.openapi.common.generate.model.type;

import io.github.potjerodekool.openapi.common.generate.model.element.Annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReferenceType implements Type {

    private String name;

    private List<Type> typeArgs;

    private List<Annotation> annotations;

    public String getName() {
        return name;
    }

    public String getPackageName() {
        if (name.contains(".")) {
            return name.substring(0, name.lastIndexOf("."));
        }
        return null;
    }

    public String getSimpleName() {
        if (name.contains(".")) {
            return name.substring(name.lastIndexOf(".") + 1);
        }
        return name;
    }


    public ReferenceType name(final String name) {
        this.name = name;
        return this;
    }

    public List<Type> getTypeArgs() {
        return typeArgs;
    }

    public ReferenceType typeArgs(final List<Type> typeArgs) {
        if (this.typeArgs == null) {
            this.typeArgs = new ArrayList<>();
        }
        this.typeArgs.addAll(typeArgs);
        return this;
    }

    public ReferenceType typeArgs(final Type... typeArgs) {
        return typeArgs(Arrays.asList(typeArgs));
    }

    public ReferenceType typeArg(final Type typeArg) {
        if (this.typeArgs == null) {
            this.typeArgs = new ArrayList<>();
        }

        this.typeArgs.add(typeArg);
        return this;
    }

    public List<Annotation> getAnnotations() {
        return annotations;
    }

    public ReferenceType annotations(final List<Annotation> annotations) {
        if (this.annotations == null) {
            this.annotations = new ArrayList<>();
        }
        this.annotations.addAll(annotations);
        return this;
    }

    public ReferenceType annotation(final Annotation annotation) {
        if (this.annotations == null) {
            this.annotations = new ArrayList<>();
        }
        this.annotations.add(annotation);
        return this;
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.REFERENCE;
    }
}
