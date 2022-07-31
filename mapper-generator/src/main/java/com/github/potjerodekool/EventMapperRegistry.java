package com.github.potjerodekool;

public class EventMapperRegistry implements MapperRegistry {

    @Override
    public <A, B> Mapper<A, B> getMapper(final Class<A> source,
                                         final Class<B> target) {
        if (source == EventDto.class && target == Event.class) {
            return (Mapper<A, B>) new EventDto2EventMapper();
        }
        return null;
    }
}
