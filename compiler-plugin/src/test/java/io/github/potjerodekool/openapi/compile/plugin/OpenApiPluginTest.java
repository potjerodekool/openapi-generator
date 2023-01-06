package io.github.potjerodekool.openapi.compile.plugin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiPluginTest {

    private static final String CLASS_TEMPLATE =
            """
                    package com.baeldung.javac;

                    public class Test {
                        public static int service(int value) {
                            return value > -1 ? 1 : 0;
                        }
                    }
                    """;

    private final TestCompiler compiler = new TestCompiler();
    private final TestRunner   runner   = new TestRunner();

    //@Test(expected = IllegalArgumentException.class)
    public void givenInt_whenNegative_thenThrowsException() throws Throwable {
        compileAndRun(double.class,-1);
    }

    //@Test(expected = IllegalArgumentException.class)
    public void givenInt_whenZero_thenThrowsException() throws Throwable {
        compileAndRun(int.class,0);
    }

    @Test
    public void givenInt_whenPositive_thenSuccess() throws Throwable {
        assertEquals(1, compileAndRun(int.class, 1));
    }

    private Object compileAndRun(Class<?> argumentType, Object argument) throws Throwable {
        String qualifiedClassName = "com.baeldung.javac.Test";
        byte[] byteCode = compiler.compile(qualifiedClassName, String.format(CLASS_TEMPLATE, argumentType.getName()));
        return runner.run(byteCode, qualifiedClassName, "service", new Class[] {argumentType}, argument);
    }
}