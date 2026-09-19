package com.payvault.auth.controller;

import com.payvault.auth.dto.AuthResponse;
import com.payvault.auth.dto.LoginRequest;
import com.payvault.auth.dto.RegisterRequest;
import com.payvault.auth.dto.RegisterResponse;
import com.payvault.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/auth/validate - protected by JwtAuthenticationFilter.
     *
     * Not part of the public API contract in the architecture doc; exists so
     * you can prove the filter/SecurityConfig actually work in this phase,
     * before the Gateway takes over edge-level JWT validation in Phase 7.
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
                "userId", authentication.getName(),
                "authorities", authentication.getAuthorities()
        ));
    }
}
