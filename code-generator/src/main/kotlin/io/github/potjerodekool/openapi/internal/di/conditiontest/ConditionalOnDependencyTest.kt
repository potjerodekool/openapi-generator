package io.github.potjerodekool.openapi.internal.di.conditiontest

import io.github.potjerodekool.openapi.common.dependency.ConditionalOnDependency
import io.github.potjerodekool.openapi.common.dependency.DependencyChecker
import io.github.potjerodekool.openapi.internal.di.bean.BeanDefinition

class ConditionalOnDependencyTest(private val dependencyChecker: DependencyChecker) :
    ConditionalTest<ConditionalOnDependency> {
    override fun test(condition: ConditionalOnDependency, beanDefinition: BeanDefinition): Boolean {
        val groupId = condition.groupId
        val artifactId = condition.artifactId
        return dependencyChecker.isDependencyPresent(groupId, artifactId)
    }
}
