setterBuilder(property, model)::= <<
public <model.simpleName> <property.builderSetterName>(final <type(property.type)> <property.simpleName>) {
    this.<property.simpleName> = <property.simpleName>;
    return this;
}
>>