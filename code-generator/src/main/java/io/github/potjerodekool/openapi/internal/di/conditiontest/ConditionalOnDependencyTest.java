package io.github.potjerodekool.openapi.internal.di.conditiontest;

import io.github.potjerodekool.openapi.common.dependency.DependencyChecker;
import io.github.potjerodekool.openapi.internal.di.bean.BeanDefinition;
import io.github.potjerodekool.openapi.common.dependency.ConditionalOnDependency;

public class ConditionalOnDependencyTest implements ConditionalTest<ConditionalOnDependency> {

    private final DependencyChecker dependencyChecker;

    public ConditionalOnDependencyTest(final DependencyChecker dependencyChecker) {
        this.dependencyChecker = dependencyChecker;
    }

    @Override
    public boolean test(final ConditionalOnDependency conditionalOnDependency,
                        final BeanDefinition beanDefinition) {
        final var groupId = conditionalOnDependency.groupId();
        final var artifactId = conditionalOnDependency.artifactId();
        return this.dependencyChecker.isDependencyPresent(groupId, artifactId);
    }
}
