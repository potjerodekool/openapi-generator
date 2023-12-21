package org.some.util;

import javax.annotation.processing.Generated;
import java.net.URI;
import jakarta.servlet.http.HttpServletRequest;

@Generated(value = {"io.github.potjerodekool.openapi.internal.generate.api.ApiUtilsGenerator"})
public final class ApiUtils {

    private ApiUtils() {}

    public static URI createLocation(final HttpServletRequest request, final Object id) {
        String location = request.getRequestURL().toString();

        if (!location.endsWith("/")) {
            location += "/";
        }

        return URI.create(location + id);
    }

}