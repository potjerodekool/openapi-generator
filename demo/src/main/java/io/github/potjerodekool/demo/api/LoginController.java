package io.github.potjerodekool.demo.api;

import io.github.potjerodekool.petstore.api.PetstoreApi;
import io.github.potjerodekool.petstore.api.model.LoginRequest;
import io.github.potjerodekool.petstore.api.model.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
public class LoginController implements PetstoreApi {

    @Override
    public ResponseEntity<LoginResponse> login(final LoginRequest body,
                                        final HttpServletRequest request) {
        if ("test".equals(body.getUsername()) && "test".equals(body.getPassword())) {
            return ResponseEntity.ok(new LoginResponse("urteuirteureirtepiotrpioeptirep="));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
