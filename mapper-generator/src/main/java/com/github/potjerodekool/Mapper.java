package com.github.potjerodekool;

public interface Mapper<A,B> {

    B map(A source, MapperContext context);

    B map(A source, B target, MapperContext context);
}
