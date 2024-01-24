request(packageName, generatorName, date)::= <<
package <packageName>;

import javax.annotation.processing.Generated;
import java.util.Map;

@Generated(value = "<generatorName>", date="<date>")
public interface Request {
	String getParameter(String name);

	String[] getParameterValues(String name);

	Map\<String, String[]\> getParameterMap();

	String getRemoteAddr();

	String getRemoteHost();

	int getRemotePort();

}

>>