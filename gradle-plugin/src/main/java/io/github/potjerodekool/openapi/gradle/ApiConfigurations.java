package io.github.potjerodekool.openapi.gradle;

import java.util.ArrayList;
import java.util.List;

public class  ApiConfigurations {

    private List<ApiConfiguration> apis = new ArrayList<>();

    public void api(final ApiConfiguration configuration) {
        apis.add(configuration);
    }
}
