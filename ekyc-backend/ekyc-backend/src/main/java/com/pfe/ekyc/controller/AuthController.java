package com.pfe.ekyc.controller;

import com.pfe.ekyc.dto.Dtos.AuthResponse;
import com.pfe.ekyc.dto.Dtos.LoginRequest;
import com.pfe.ekyc.dto.Dtos.RegisterRequest;
import com.pfe.ekyc.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentification")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer un compte client")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Se connecter et obtenir un jeton JWT")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
