package io.github.potjerodekool.demo.api;

/*
import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.annotations.Pact;

 */
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

//@ExtendWith(PactConsumerTestExt.class)
//@PactTestFor(providerName = "petstore_provider", hostInterface="localhost")
public class PetControllerPactTest {

    /*
    @Pact(provider="petstore_provider", consumer = "petstore_consumer")
    public au.com.dius.pact.core.model.V4Pact createPact(final PactBuilder builder) {
        final var headers = Map.of(
                "Content-Type", "application/json",
                HttpHeaders.AUTHORIZATION, "Bearer test"
        );

        return builder.usingLegacyDsl()
                .given("test Create a pet")
                .uponReceiving("post request")
                .method("POST")
                .headers(headers)
                .body("""
                        {
                            "name": "Mie auw",
                            "tag": "cat"
                        }
                        """)
                .path("/pets")
                .willRespondWith()
                .status(201)
                .toPact(au.com.dius.pact.core.model.V4Pact.class);
    }

    @Test
    //@PactTestFor
    public void givenCreatePet(final MockServer mockServer) {
        final var httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer test");

        String jsonBody = """
                        {
                            "name": "Mie auw",
                            "tag": "cat"
                        }
                        """;

// when
        ResponseEntity<String> postResponse = new RestTemplate()
                .exchange(
                        mockServer.getUrl() + "/pets",
                        HttpMethod.POST,
                        new HttpEntity<>(jsonBody, httpHeaders),
                        String.class
                );

//then
        Assertions.assertThat(postResponse.getStatusCode().value()).isEqualTo(201);
    }

     */
}
