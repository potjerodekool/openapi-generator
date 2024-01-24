package io.github.potjerodekool.openapi.internal.generate.model.st.adapter;

import org.stringtemplate.v4.Interpreter;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.misc.STNoSuchPropertyException;

public class EnumModelAdapter extends DelegatingModelAdapter<Enum> {

    @Override
    public Object getProperty(final Interpreter interp,
                              final ST self,
                              final Enum model,
                              final Object property,
                              final String propertyName) throws STNoSuchPropertyException {
        if (propertyName.startsWith("is")) {
            final var enumValue = propertyName.substring(2);
            return enumValue.equals(model.name());
        }

        return super.getProperty(interp, self, model, property, propertyName);
    }
}