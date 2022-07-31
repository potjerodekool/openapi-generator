package com.github.potjerodekool;

public interface MapperRegistry {

    <A,B> Mapper<A,B> getMapper(Class<A> source, Class<B> target);
}
