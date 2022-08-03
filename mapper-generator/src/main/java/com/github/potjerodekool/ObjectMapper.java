package com.github.potjerodekool;

public class ObjectMapper {

    private final MapperRegistry mapperRegistry;

    public ObjectMapper(final MapperRegistry mapperRegistry) {
        this.mapperRegistry = mapperRegistry;
    }

    public <A,B> B map(final A source,
                       final Class<B> target) {
        Mapper<A,B> mapper = (Mapper<A, B>) mapperRegistry.getMapper(source.getClass(), target);

        if (mapper == null) {
            throw new MissingMapperException(String.format("Missing mapper from %s to %s", source.getClass(), target.getClass()));
        }

        return mapper.map(source, new MapperContext(this));
    }
}
