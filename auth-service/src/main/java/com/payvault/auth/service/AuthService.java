package com.payvault.auth.service;

import com.payvault.auth.client.UserServiceClient;
import com.payvault.auth.dto.AuthResponse;
import com.payvault.auth.dto.LoginRequest;
import com.payvault.auth.dto.RegisterRequest;
import com.payvault.auth.dto.RegisterResponse;
import com.payvault.auth.exception.EmailAlreadyExistsException;
import com.payvault.auth.exception.InvalidCredentialsException;
import com.payvault.auth.model.Role;
import com.payvault.auth.model.UserCredentials;
import com.payvault.auth.repository.AuthRepository;
import com.payvault.auth.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserServiceClient userServiceClient;

    public AuthService(AuthRepository authRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        UserServiceClient userServiceClient) {
        this.authRepository = authRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userServiceClient = userServiceClient;
    }

    public RegisterResponse register(RegisterRequest request) {
        if (authRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        // This service is the source of identity: userId is minted here,
        // then handed to user-service so both services agree on who this
        // person is going from day one.
        String userId = UUID.randomUUID().toString();
        String passwordHash = passwordEncoder.encode(request.getPassword());

        UserCredentials credentials = new UserCredentials(userId, request.getEmail(), passwordHash, List.of(Role.USER));
        authRepository.save(credentials);

        userServiceClient.createProfile(userId, request.getName(), request.getEmail(), request.getPhone());

        return new RegisterResponse(userId, request.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        UserCredentials credentials = authRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), credentials.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(credentials.getUserId(), credentials.getRoles());
        Instant expiresAt = jwtService.getExpiration(token).toInstant().atZone(ZoneOffset.UTC).toInstant();

        return new AuthResponse(token, credentials.getUserId(), expiresAt);
    }
}
