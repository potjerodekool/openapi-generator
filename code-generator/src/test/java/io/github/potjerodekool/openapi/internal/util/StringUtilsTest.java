package io.github.potjerodekool.openapi.internal.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilsTest {

    @Test
    void toValidClassName() {
        assertEquals("AgendaItems", StringUtils.toValidClassName("agenda-items"));
        assertEquals("AgendaItems", StringUtils.toValidClassName("agenda_items"));


    }
}