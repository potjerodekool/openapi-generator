package io.github.potjerodekool.demo.service;

import com.github.potjerodekool.Mapper;
import com.github.potjerodekool.MapperRegistry;

public class SimpleMapperRegistry implements MapperRegistry {
    @Override
    public <A, B> Mapper<A, B> getMapper(final Class<A> source, final Class<B> target) {
        return null;
    }
}
