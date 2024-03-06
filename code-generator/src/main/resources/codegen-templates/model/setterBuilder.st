setterBuilder(property, model)::= <<
public
 <if(model.typeArguments)>SELF<else><model.simpleName><endif> <property.builderSetterName>(final <type(property.type)> <property.simpleName>) {
    this.<property.simpleName> = <property.simpleName>;
    return <if(model.typeArguments)>(SELF)<endif> this;
}
>>
