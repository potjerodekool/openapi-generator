package io.github.potjerodekool.openapi;

import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.*;

class OrderedPropertiesTest {

    @Test
    void load() {
        for (String name : Charset.availableCharsets().keySet()) {
            System.out.println(name);
        }
    }
}