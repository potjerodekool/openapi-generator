package io.github.potjerodekool.demo.api;

import io.github.potjerodekool.demo.api.model.LoginRequestDto;
import io.github.potjerodekool.demo.api.model.LoginResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@CrossOrigin
public class LoginController implements LoginApi {

    public LoginController() {
        System.out.println("");
    }

    @Override
    public ResponseEntity<LoginResponseDto> login(final LoginRequestDto body,
                                                  final HttpServletRequest request) {
        if ("test".equals(body.getUsername()) && "test".equals(body.getPassword())) {
            return ResponseEntity.ok(new LoginResponseDto("urteuirteureirtepiotrpioeptirep="));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
