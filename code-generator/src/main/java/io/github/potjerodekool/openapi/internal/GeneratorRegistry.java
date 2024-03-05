package io.github.potjerodekool.openapi.internal;

import io.github.potjerodekool.openapi.common.generate.api.CodeGenerator;

import java.lang.reflect.InvocationTargetException;

public class GeneratorRegistry {

    public static final String GENERATOR_NAME = "generatorName";
    public static final String GENERATOR_LANG = "generatorLang";
    public static final String GENERATOR_CLASS = "generatorClass";


    public CodeGenerator loadCodeGenerator(final String name,
                                           final String lang) {
        try {
            final var resources = getClass().getClassLoader().getResources(
                    "io/github/potjerodekool/openapi/common/generate/api/CodeGenerator"
            );

            String generatorName;
            String generatorLang;
            String generatorClassName;

            while (resources.hasMoreElements()) {
                generatorName = null;
                generatorLang = null;
                generatorClassName = null;
                final var url = resources.nextElement();
                try (final var inputStream = url.openStream()) {
                    final var lines = new String(inputStream.readAllBytes())
                            .replace("\r", "")
                            .split("\n");

                    for (final String line : lines) {
                        final var keyValue = line.split("=");
                        final var key = keyValue[0];
                        final var value = keyValue[1];

                        if (GENERATOR_NAME.equals(key)) {
                            generatorName = value;
                        } else if (GENERATOR_LANG.equals(key)) {
                            generatorLang = value;
                        } else if (GENERATOR_CLASS.equals(key)) {
                            generatorClassName = value;
                        }
                    }

                    if (generatorName != null
                            && generatorLang != null
                            && generatorClassName != null) {

                        if (generatorName.equals(name)
                            && generatorLang.equals(lang)) {
                            return createInstance(generatorClassName);
                        }
                    }
                }
            }
        } catch (final Exception e) {
            throw new GenerateException("Code generator not found");
        }

        throw new GenerateException("Code generator not found");
    }

    private CodeGenerator createInstance(final String className) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        final var generatorClazz = (Class<CodeGenerator>) getClass().getClassLoader().loadClass(className);
        return generatorClazz.getDeclaredConstructor().newInstance();
    }
}
