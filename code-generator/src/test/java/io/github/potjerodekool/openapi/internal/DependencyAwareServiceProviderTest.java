package io.github.potjerodekool.openapi.internal;

import io.github.potjerodekool.openapi.internal.generate.TypesJava;
import io.github.potjerodekool.openapi.internal.generate.model.HibernateValidationModelAdapter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DependencyAwareServiceProviderTest {

    @Test
    void test() {
        final var types = new TypesJava();
        final DependencyAwareServiceProvider<HibernateValidationModelAdapter> provider = DependencyAwareServiceProvider.create(List.of(types));
        final var instance = provider.get(HibernateValidationModelAdapter.class);
        assertNotNull(instance);
    }
}
