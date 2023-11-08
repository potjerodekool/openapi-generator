package io.github.potjerodekool.demo.api;

import io.github.potjerodekool.demo.api.model.LoginRequest;
import io.github.potjerodekool.demo.api.model.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface AuthApi {

    @Operation(summary = "Auth", operationId = "auth")
    @SecurityRequirements()
    @ApiResponses(value = {@ApiResponse(description = "Login success response", content = {@Content(schema = @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, implementation = LoginResponse.class), mediaType = "application/json")}, responseCode = "200"), @ApiResponse(description = "Unauthorised", responseCode = "401")})
    @PostMapping(produces = {"application/json"}, value = {"/auth"}, consumes = {"application/json"})
    default ResponseEntity<LoginResponse> auth(@RequestBody final LoginRequest body, final HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
