openApiConfiguration(packageName, generatorName, date) ::= <<
package <packageName>.config;

import org.springframework.context.annotation.Configuration;
import javax.annotation.processing.Generated;
import org.springframework.context.annotation.Bean;
import io.swagger.v3.oas.models.OpenAPI;
import java.util.List;

@Configuration()
@Generated(value = "<generatorName>", date="<date>")
public class OpenApiConfiguration {

	@Bean()
	public OpenAPI api() {
	    return new io.swagger.v3.oas.models.OpenAPI();
	}

}

>>