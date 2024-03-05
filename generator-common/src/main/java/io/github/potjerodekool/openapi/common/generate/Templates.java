package io.github.potjerodekool.openapi.common.generate;

import io.github.potjerodekool.codegen.template.adapter.EnumModelAdapter;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupDir;

public class Templates {

    private final STGroupDir group = new STGroupDir("codegen-templates");

    public Templates() {
        group.registerModelAdaptor(Enum.class, new EnumModelAdapter());
    }

    public ST getInstanceOf(final String name) {
        return group.getInstanceOf(name);
    }
}
