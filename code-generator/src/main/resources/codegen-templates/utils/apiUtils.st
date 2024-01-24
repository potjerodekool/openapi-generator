apiUtils(packageName, generatorName, date)::= <<
package <packageName>;

import javax.annotation.processing.Generated;
import java.net.URI;
import jakarta.servlet.http.HttpServletRequest;

@Generated(value = "<generatorName>", date="<date>")
public final class ApiUtils {

	private ApiUtils() {
	}

	public static URI createLocation(final HttpServletRequest request, final Object id) {
		final StringBuffer locationBuffer = request.getRequestURL();

		if (locationBuffer.charAt(locationBuffer.length() - 1) != '/') {
		    locationBuffer.append('/');
		}

		return URI.create(locationBuffer.append(id).toString());
	}

}

>>