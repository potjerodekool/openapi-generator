package io.github.potjerodekool.openapi.internal.di;

import io.github.potjerodekool.openapi.common.dependency.Bean;
import io.github.potjerodekool.openapi.internal.di.bean.AutoConfigBeanDefinition;
import io.github.potjerodekool.openapi.internal.di.bean.BeanDefinition;
import org.objectweb.asm.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutoConfigReader extends ClassVisitor {

    private final List<BeanDefinition> beanDefinitions = new ArrayList<>();


    private final Object autoConfigInstance;

    protected AutoConfigReader(final Object autoConfigInstance) {
        super(Opcodes.ASM9);
        this.autoConfigInstance = autoConfigInstance;
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name,
                                     final String descriptor,
                                     final String signature, final String[] exceptions) {
        return new AutoConfigMethodReader(api, name, descriptor, this);
    }

    void addBeanDefinition(final BeanDefinition beanDefinition) {
        this.beanDefinitions.add(beanDefinition);
    }

    public List<BeanDefinition> getBeanDefinitions() {
        return beanDefinitions;
    }

    Object getAutoConfigInstance() {
        return autoConfigInstance;
    }
}

class AutoConfigMethodReader extends MethodVisitor {

    private final AutoConfigReader autoConfigReader;

    private final String methodName;

    private final String descriptor;

    private final Map<Class<?>, Annotation> annotations = new HashMap<>();

    protected AutoConfigMethodReader(final int api,
                                     final String methodName,
                                     final String descriptor,
                                     final AutoConfigReader autoConfigReader) {
        super(api);
        this.methodName = methodName;
        this.descriptor = descriptor;
        this.autoConfigReader = autoConfigReader;
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
        final var className = descriptorToClassName(descriptor);
        return new AutoConfigAnnotationReader(api, className, this);
    }

    private String descriptorToClassName(final String descriptor) {
        String className = descriptor.substring(1);
        className = className.substring(0, className.length() - 1);
        return className.replace('/', '.');
    }

    public void addAnnotation(final Class<?> annotationClass, final Annotation annotation) {
        this.annotations.put(annotationClass, annotation);
    }

    @Override
    public void visitEnd() {
        if (annotations.containsKey(Bean.class)) {
            try {
                final var returnType = getClass().getClassLoader().loadClass(Type.getMethodType(descriptor).getReturnType().getClassName());
                final var beanDefinition = new AutoConfigBeanDefinition(
                        autoConfigReader.getAutoConfigInstance(),
                        methodName,
                        returnType,
                        annotations
                );
                autoConfigReader.addBeanDefinition(beanDefinition);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

class AutoConfigAnnotationReader extends AnnotationVisitor {

    private final Map<String, Object> attributes = new HashMap<>();
    private final String className;
    private final AutoConfigMethodReader methodReader;

    protected AutoConfigAnnotationReader(final int api,
                                         final String className,
                                         final AutoConfigMethodReader methodReader) {
        super(api);
        this.className = className;
        this.methodReader = methodReader;
    }

    @Override
    public void visit(final String name, final Object value) {
        if (value instanceof Type type) {
            try {
                final var clazz = getClass().getClassLoader().loadClass(
                        type.getClassName()
                );
                this.attributes.put(name, clazz);
            } catch (final ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else {
            this.attributes.put(name, value);
        }
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

        methodReader.addAnnotation(annotationClass, annotation);
    }
}