package io.github.potjerodekool.demo.config;

import com.github.potjerodekool.MapperContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.potjerodekool.demo.mapper.PetRequestDtoToPetMapper;

@Configuration
public class MapperConfig {

    @Bean
    public MapperContext mapperContext() {
        return new MapperContext();
    }

    @Bean
    public PetRequestDtoToPetMapper petRequestDtoToPetMapper() {
        return new PetRequestDtoToPetMapper();
    }
}
