package io.github.potjerodekool.openapi.internal.di;

public class AllClassFilter implements ClassFilter {

    public static final ClassFilter FILTER = new AllClassFilter();

    private AllClassFilter() {
    }

    @Override
    public boolean filter(final BeanDefinition beanDefinition) {
        return true;
    }
}
