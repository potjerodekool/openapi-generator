package com.github.potjerodekool;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MapperTest {

    @Test
    void test(){
        final var registry = new EventMapperRegistry();
        final var objectMapper = new ObjectMapper(registry);
        final var eventDto = new EventDto(
                "test",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1)
        );

        final var event = objectMapper.map(eventDto, Event.class);
        assertEquals(eventDto.getName(), event.getName());
        assertEquals(eventDto.getStart(), event.getStart());
        assertEquals(eventDto.getEnd(), event.getEnd());
    }
}
