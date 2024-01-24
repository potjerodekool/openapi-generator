httpServletRequestWrapper(packageName, generatorName, date)::= <<

package <packageName>;

import javax.annotation.processing.Generated;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@Generated(value = "<generatorName>", date="<date>")
public class HttpServletRequestWrapper implements <packageName>.Request  {

	private final HttpServletRequest request;

	public HttpServletRequestWrapper(final HttpServletRequest request) {
		this.request = request;
	}

	@Override()
	public String getParameter(final String name) {
		return this.request.getParameter(name);
	}

	@Override()
	public String[] getParameterValues(final String name) {
		return this.request.getParameterValues(name);
	}

	@Override()
	public Map\<String, String[]\> getParameterMap() {
		return this.request.getParameterMap();
	}

	@Override()
	public String getRemoteAddr() {
		return this.request.getRemoteAddr();
	}

	@Override()
	public String getRemoteHost() {
		return this.request.getRemoteHost();
	}

	@Override()
	public int getRemotePort() {
		return this.request.getRemotePort();
	}

}

>>