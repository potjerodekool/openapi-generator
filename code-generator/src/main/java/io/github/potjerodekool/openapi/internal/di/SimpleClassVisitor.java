package io.github.potjerodekool.openapi.internal.di;

import io.github.potjerodekool.openapi.common.dependency.Bean;
import io.github.potjerodekool.openapi.internal.di.bean.BeanDefinition;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

class SimpleClassVisitor extends ClassVisitor implements WithAnnotationsVisitor {

    private final Map<Class<?>, Annotation> annotations = new HashMap<>();

    private @Nullable String className = null;

    private @Nullable BeanDefinition beanDefinition;

    protected SimpleClassVisitor() {
        super(Opcodes.ASM9);
    }

    @Override
    public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {

        super.visit(version, access, name, signature, superName, interfaces);
        this.className = name.replace('/', '.');
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
        final var className = descriptorToClassName(descriptor);
        return new SimpleAnnotationVisitor(api, className, this);
    }

    private String descriptorToClassName(final String descriptor) {
        String className = descriptor.substring(1);
        className = className.substring(0, className.length() - 1);
        return className.replace('/', '.');
    }

    @Override
    public void addAnnotation(final Class<?> annotationClass, final Annotation annotation) {
        annotations.put(annotationClass, annotation);
    }

    @Override
    public void visitEnd() {
        final var beanAnnotation = annotations.get(Bean.class);
        final String beanClassName = className;

        if (beanAnnotation != null && beanClassName != null) {
            this.beanDefinition = new BeanDefinition(beanClassName, annotations);
        }

        super.visitEnd();
    }

    public @Nullable BeanDefinition getBeanDefinition() {
        return beanDefinition;
    }
}

class SimpleAnnotationVisitor extends AnnotationVisitor implements WithAnnotationsVisitor {

    private final String className;
    private final WithAnnotationsVisitor parent;
    private final Map<String, Object> attributes = new HashMap<>();

    protected SimpleAnnotationVisitor(final int api,
                                      final String className,
                                      final WithAnnotationsVisitor parent) {
        super(api);
        this.className = className;
        this.parent = parent;
    }

    @Override
    public void visit(final String name, final Object value) {
        this.attributes.put(name, value);
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String name, final String descriptor) {
        return new SimpleAnnotationVisitor(
                api,
                name,
                this
        );
    }

    @Override
    public void visitEnd() {
        final var classLoader = ClassPathScanner.getClassLoader();
        final Class<?> annotationClass;

        try {
            annotationClass = classLoader.loadClass(className);
        } catch (final ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        final var annotation = (Annotation) Proxy.newProxyInstance(
                classLoader,
                new Class[]{annotationClass},
                new AnnotationInvocationHandler(
                        annotationClass,
                        this.attributes
                )
        );

        parent.addAnnotation(annotationClass, annotation);
    }

    @Override
    public void addAnnotation(final Class<?> annotationClass, final Annotation annotation) {
    }
}

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

        attributes.forEach((key,value) ->attributeJoiner.add(quoteString(key) + "=" + quoteString(value)));

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

interface WithAnnotationsVisitor {

    void addAnnotation(Class<?> annotationClass, Annotation annotation);
}

