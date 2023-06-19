package io.github.potjerodekool.demo.api;

import io.swagger.v3.oas.annotations.media.Schema;

public class Person {

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private int id;

    private String name;

    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}
