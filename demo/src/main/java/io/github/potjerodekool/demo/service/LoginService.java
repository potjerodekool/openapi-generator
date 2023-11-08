package io.github.potjerodekool.demo.service;

import io.github.potjerodekool.demo.api.LoginControllerServiceApi;
import io.github.potjerodekool.demo.api.Request;
import io.github.potjerodekool.demo.api.model.LoginRequest;
import io.github.potjerodekool.demo.api.model.LoginResponse;
import org.springframework.stereotype.Service;

@Service
public class LoginService implements LoginControllerServiceApi {

    public LoginService() {

    }
    @Override
    public LoginResponse login(final LoginRequest body, final Request request) {
        return new LoginResponse("aviapoipoaias4950");
    }
}
