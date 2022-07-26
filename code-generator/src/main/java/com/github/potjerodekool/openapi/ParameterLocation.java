package com.github.potjerodekool.openapi;

public enum ParameterLocation {
    QUERY,
    PATH;

    public static ParameterLocation parseIn(String in) {
        if ("query".equals(in)) {
            return QUERY;
        } else if ("path".equals(in)){
            return PATH;
        } else {
            throw new IllegalArgumentException(in);
        }
    }
}
