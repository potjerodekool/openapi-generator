field(field)::= <<
<if(field.kind.isPROPERTY)>
@com.fasterxml.jackson.annotation.JsonProperty(value = "<field.simpleName>")

<field.annotations:{annotation | <annotation(annotation)>};separator="\n">
private <type(field.type)> <field.simpleName>;
<elseif(field.kind.isENUM_CONSTANT)>
<field.simpleName>
<endif>
>>