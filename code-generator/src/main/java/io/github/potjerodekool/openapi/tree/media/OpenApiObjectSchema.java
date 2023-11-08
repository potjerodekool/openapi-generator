package io.github.potjerodekool.openapi.tree.media;

import io.github.potjerodekool.openapi.internal.OpenApiSchemaBuilder;
import io.github.potjerodekool.openapi.tree.Package;

import java.util.Objects;

public class OpenApiObjectSchema extends OpenApiSchema<Object> {

    private final Package pck;
    private final String name;

    public OpenApiObjectSchema(final OpenApiSchemaBuilder schemaBuilder,
                               final Package pck,
                               final String name) {
        super(schemaBuilder);
        Objects.requireNonNull(pck, "package should be non null");
        Objects.requireNonNull(name, "name should be non null");
        this.pck = pck;
        this.name = name;
    }

    public Package pck() {
        return pck;
    }

    @Override
    public String name() {
        return name;
    }

    private OpenApiSchemaBuilder builder() {
        return new OpenApiSchemaBuilder().format(format())
                .nullable(nullable())
                .requiredProperties(required())
                .readOnly(readOnly())
                .writeOnly(writeOnly())
                .description(description())
                .extensions(extensions())
                .itemsSchema(itemschema())
                .properties(properties())
                .additionalProperties(additionalProperties())
                .minimum(minimum())
                .exclusiveMinimum(exclusiveMinimum())
                .maximum(maximum())
                .exclusiveMaximum(exclusiveMaximum())
                .minLength(minLength())
                .maxLength(maxLength())
                .pattern(pattern())
                .minItems(minItems())
                .maxItems(maxItems())
                .uniqueItems(uniqueItems())
                .enums(enumeration());
    }

    public OpenApiObjectSchema withName(final String name) {
        return new OpenApiObjectSchema(
                builder(),
                pck(),
                name
        );
    }

    public OpenApiObjectSchema withPackage(final Package pck) {
        return new OpenApiObjectSchema(
                builder(),
                pck,
                name()
        );
    }
}
