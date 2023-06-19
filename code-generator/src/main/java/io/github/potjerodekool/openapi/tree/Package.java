package io.github.potjerodekool.openapi.tree;

public class Package {

    public static final Package UNNAMED = new Package();

    private final String name;

    public Package(final String name) {
        this.name = name;
    }

    public static Package of(final String name) {
        if ("".equals(name)) {
            return UNNAMED;
        } else {
            return new Package(name);
        }
    }

    private Package() {
        this("");
    }

    public String getName() {
        return name;
    }

    public boolean isUnnamed() {
        return this == UNNAMED;
    }

    @Override
    public String toString() {
        return name;
    }
}
