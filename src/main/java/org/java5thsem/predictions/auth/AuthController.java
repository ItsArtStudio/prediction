package org.java5thsem.predictions.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/providers")
    public AuthProvidersResponse listProviders() {
        return authService.listProviders();
    }

    @PostMapping("/social")
    public ResponseEntity<AuthResponse> loginOrSignUp(@Valid @RequestBody SocialLoginRequest request) {
        AuthResponse response = authService.loginOrSignUp(request);
        HttpStatus status = response.newUser() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/me")
    public CurrentUserResponse currentUser(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        return authService.currentUser(authorization);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        authService.logout(authorization);
    }
}
