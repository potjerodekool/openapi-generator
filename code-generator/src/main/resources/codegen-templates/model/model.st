model(model) ::= <<
<if(model.packageName)>
package <model.packageName>;
<endif>

<model.annotations:{annotation | <annotation(annotation)>};separator="\n">
public <if(model.kind.isENUM)>enum<else>class<endif> <model.simpleName><typeArguments(model.typeArguments)><if(model.superType)> extends <type(model.superType)> <endif>{

    <if(model.kind.isENUM)>
    <model.properties:{property | <field(property)>};separator=",\n">
    <else>
    <model.properties:{property | <field(property)>};separator="\n">
    <model.properties:{property | <getter(property)>};separator="\n">
    <model.properties:{property | <setter(property)>};separator="\n">
    <model.properties:{property | <setterBuilder(property, model)>};separator="\n">
    <model.models:{model | <model(model)>};separator="\n">
    <endif>
}
>>
