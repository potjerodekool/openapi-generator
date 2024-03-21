package io.github.potjerodekool.openapi.common.autoconfig;

import io.github.potjerodekool.openapi.common.dependency.Bean;
import io.github.potjerodekool.openapi.common.dependency.ConditionalOnDependency;
import io.github.potjerodekool.openapi.common.dependency.ConditionalOnMissingBean;
import io.github.potjerodekool.openapi.common.generate.model.adapter.*;

@AutoConfiguration
public class CommonAutoConfiguration {

    @Bean
    @ConditionalOnDependency(groupId = "org.checkerframework", artifactId = "checker-qual")
    public CheckerModelAdapter checkerModelAdapter() {
        return new CheckerModelAdapter();
    }

    @Bean
    @ConditionalOnDependency(groupId = "org.hibernate", artifactId = "hibernate-validator")
    public HibernateValidationModelAdapter hibernateValidationModelAdapter() {
        return new HibernateValidationModelAdapter();
    }

    @Bean
    @ConditionalOnMissingBean(type = ValidationModelAdapter.class)
    public JakartaValidationModelAdapter jakartaValidationModelAdapter() {
        return new JakartaValidationModelAdapter();
    }

    @Bean
    public JaxsonModelAdapter jaxsonModelAdapter() {
        return new JaxsonModelAdapter();
    }
}
