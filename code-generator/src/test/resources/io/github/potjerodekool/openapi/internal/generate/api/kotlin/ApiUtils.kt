package org.some.util

import javax.annotation.processing.Generated
import java.net.URI
import jakarta.servlet.http.HttpServletRequest

@Generated(value = ["io.github.potjerodekool.openapi.internal.generate.api.ApiUtilsGenerator"])
object ApiUtils {

    fun createLocation(request : HttpServletRequest, id : Any): URI {
        var location = request.requestURL.toString()

        if (!location.endsWith("/")) {
            location += "/"
        }

        return URI.create(location + id)
    }

}