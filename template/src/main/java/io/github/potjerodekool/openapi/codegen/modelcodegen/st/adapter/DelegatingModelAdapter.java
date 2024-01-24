package io.github.potjerodekool.openapi.codegen.modelcodegen.st.adapter;

import org.stringtemplate.v4.Interpreter;
import org.stringtemplate.v4.ModelAdaptor;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.misc.ObjectModelAdaptor;
import org.stringtemplate.v4.misc.STNoSuchPropertyException;

public abstract class DelegatingModelAdapter<T> implements ModelAdaptor<T> {

    private final ObjectModelAdaptor<Object> defaultAdapter = new ObjectModelAdaptor<>();

    @Override
    public Object getProperty(final Interpreter interp,
                              final ST self,
                              final T model,
                              final Object property,
                              final String propertyName) throws STNoSuchPropertyException {
        return defaultAdapter.getProperty(
                interp,
                self,
                model,
                property,
                propertyName
        );
    }
}
