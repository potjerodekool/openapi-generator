package io.github.potjerodekool.openapi.internal.di;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Conditional
public @interface ConditionalOnDependency {

    String groupId();
    String artifactId();

}
