package io.github.potjerodekool.openapi.internal.di;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.StringJoiner;

class AnnotationInvocationHandler implements InvocationHandler {

    private final Class<?> annotationClass;

    private final Map<String, Object> attributes;

    public AnnotationInvocationHandler(final Class<?> annotationClass,
                                       final Map<String, Object> attributes) {
        this.annotationClass = annotationClass;
        this.attributes = attributes;
    }

    @Override
    @SuppressWarnings("return")
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        final var methodName = method.getName();

        if (method.isDefault()) {
            return InvocationHandler.invokeDefault(proxy, method, args);
        }

        return switch (methodName) {
            case "annotationType" -> annotationClass;
            case "equals" -> this.equals(args[0]);
            case "hashCode" -> this.hashCode();
            case "toString" -> this.toString();
            default -> {
                if (method.getReturnType() == Void.class || method.getReturnType() == Void.TYPE) {
                    yield null;
                } else if (method.getParameterCount() == 0) {
                    if (this.attributes.containsKey(methodName)) {
                        yield this.attributes.get(methodName);
                    }
                }

                yield null;
            }
        };
    }

    public String toString() {
        final var sb = new StringBuilder();
        sb.append("@");
        sb.append(annotationClass.getName());
        sb.append("(");

        final var attributeJoiner = new StringJoiner(",");

        attributes.forEach((key, value) -> attributeJoiner.add(quoteString(key) + "=" + quoteString(value)));

        sb.append(attributeJoiner);
        sb.append(")");

        return sb.toString();
    }

    private Object quoteString(final Object value) {
        if (value instanceof String) {
            return "\"" + value + "\"";
        } else {
            return value;
        }
    }
}
