annotation(annotation) ::= <<
@<annotation.name>(<annotation.attributes.keys:{key | <key> = </expression/expression(annotation.attributes.(key))>};separator=", ">)
>>