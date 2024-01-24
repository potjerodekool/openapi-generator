field(field)::= <<
@com.fasterxml.jackson.annotation.JsonProperty(value = "<field.simpleName>")
<field.annotations:{annotation | <annotation(annotation)>};separator="\n">
private <type(field.type)> <field.simpleName>;
>>