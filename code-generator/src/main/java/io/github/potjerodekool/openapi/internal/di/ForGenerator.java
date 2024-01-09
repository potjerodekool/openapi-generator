package io.github.potjerodekool.openapi.internal.di;

@Conditional
public @interface ForGenerator {
    String generator() default "";
}
