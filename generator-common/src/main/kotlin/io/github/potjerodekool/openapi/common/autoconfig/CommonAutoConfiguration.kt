package io.github.potjerodekool.openapi.common.autoconfig

import io.github.potjerodekool.openapi.common.dependency.Bean
import io.github.potjerodekool.openapi.common.dependency.ConditionalOnDependency
import io.github.potjerodekool.openapi.common.dependency.ConditionalOnMissingBean
import io.github.potjerodekool.openapi.common.generate.model.adapter.*

@AutoConfiguration
class CommonAutoConfiguration {
    @Bean
    @ConditionalOnDependency(groupId = "org.checkerframework", artifactId = "checker-qual")
    fun checkerModelAdapter(): CheckerModelAdapter {
        return CheckerModelAdapter()
    }

    @Bean
    @ConditionalOnDependency(groupId = "org.hibernate", artifactId = "hibernate-validator")
    fun hibernateValidationModelAdapter(): HibernateValidationModelAdapter {
        return HibernateValidationModelAdapter()
    }

    @Bean
    @ConditionalOnMissingBean(type = ValidationModelAdapter::class)
    fun jakartaValidationModelAdapter(): JakartaValidationModelAdapter {
        return JakartaValidationModelAdapter()
    }

    @Bean
    fun jaxsonModelAdapter(): JaxsonModelAdapter {
        return JaxsonModelAdapter()
    }

    @Bean
    fun customAnnotationsModelAdapter(): CustomAnnotationsModelAdapter {
        return CustomAnnotationsModelAdapter()
    }
}
