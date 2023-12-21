package io.github.potjerodekool.openapi;

import io.github.potjerodekool.openapi.tree.Package;
import org.stringtemplate.v4.Interpreter;
import org.stringtemplate.v4.ModelAdaptor;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.misc.STNoSuchPropertyException;

public class PackageModelAdapter implements ModelAdaptor<Package> {
    @Override
    public Object getProperty(final Interpreter interp,
                              final ST self,
                              final Package model,
                              final Object property,
                              final String propertyName) throws STNoSuchPropertyException {
        return switch (propertyName) {
            case "name" -> model.getName();
            default -> null;
        };
    }
}
