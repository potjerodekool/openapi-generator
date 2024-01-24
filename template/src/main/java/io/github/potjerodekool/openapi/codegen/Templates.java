package io.github.potjerodekool.openapi.codegen;

import io.github.potjerodekool.openapi.codegen.modelcodegen.st.adapter.EnumModelAdapter;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupDir;

public class Templates {

    private final STGroupDir group = new STGroupDir("templates");

    public Templates() {
        group.registerModelAdaptor(Enum.class, new EnumModelAdapter());
    }

    public ST getInstanceOf(String name) {
        return group.getInstanceOf(name);
    }
}
