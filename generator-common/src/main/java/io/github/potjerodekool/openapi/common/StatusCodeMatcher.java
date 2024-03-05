package io.github.potjerodekool.openapi.common;

import java.util.regex.Pattern;

public final class StatusCodeMatcher {

    private static final Pattern TWO_X_X = Pattern.compile("2[0-9]{2}");

    private StatusCodeMatcher() {
    }

    public static boolean is2XX(final String status) {
        return TWO_X_X.matcher(status).matches();
    }

}
