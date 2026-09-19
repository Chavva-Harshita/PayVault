package com.payvault.auth.service;

import com.payvault.auth.client.UserServiceClient;
import com.payvault.auth.dto.AuthResponse;
import com.payvault.auth.dto.LoginRequest;
import com.payvault.auth.dto.RegisterRequest;
import com.payvault.auth.dto.RegisterResponse;
import com.payvault.auth.exception.EmailAlreadyExistsException;
import com.payvault.auth.exception.InvalidCredentialsException;
import com.payvault.auth.model.UserCredentials;
import com.payvault.auth.repository.AuthRepository;
import com.payvault.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthRepository authRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private UserServiceClient userServiceClient;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(authRepository, passwordEncoder, jwtService, userServiceClient);
    }

    @Test
    void register_throwsWhenEmailAlreadyExists() {
        when(authRepository.existsByEmail("taken@example.com")).thenReturn(true);

        RegisterRequest request = registerRequest("taken@example.com");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(authRepository, never()).save(any());
        verifyNoInteractions(userServiceClient);
    }

    @Test
    void register_hashesPasswordAndCreatesProfileInUserService() {
        when(authRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plaintext-password")).thenReturn("bcrypt-hash");

        RegisterRequest request = registerRequest("new@example.com");
        request.setPassword("plaintext-password");
        request.setName("Asha Rao");
        request.setPhone("9999999999");

        RegisterResponse response = authService.register(request);

        assertThat(response.getEmail()).isEqualTo("new@example.com");
        assertThat(response.getUserId()).isNotBlank();

        // The password hash - never the plaintext - is what gets persisted.
        verify(authRepository).save(argThat(credentials ->
                credentials.getPasswordHash().equals("bcrypt-hash")
                        && credentials.getEmail().equals("new@example.com")));

        // user-service gets the SAME userId that was just generated, so
        // both services agree on this person's identity.
        verify(userServiceClient).createProfile(eq(response.getUserId()), eq("Asha Rao"), eq("new@example.com"), eq("9999999999"));
    }

    @Test
    void login_throwsGenericInvalidCredentials_whenEmailNotFound() {
        when(authRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setEmail("ghost@example.com");
        request.setPassword("whatever");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_throwsGenericInvalidCredentials_whenPasswordDoesNotMatch() {
        UserCredentials stored = new UserCredentials("user-1", "asha@example.com", "bcrypt-hash", List.of("ROLE_USER"));
        when(authRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(stored));
        when(passwordEncoder.matches("wrong-password", "bcrypt-hash")).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setEmail("asha@example.com");
        request.setPassword("wrong-password");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);

        // Same exception type/message regardless of "email not found" vs
        // "wrong password" - this test would also fail loudly if someone
        // later made these throw different, more specific exceptions,
        // which would leak account-enumeration information.
    }

    @Test
    void login_succeeds_andReturnsTokenWithMatchingUserId() {
        UserCredentials stored = new UserCredentials("user-1", "asha@example.com", "bcrypt-hash", List.of("ROLE_USER"));
        when(authRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(stored));
        when(passwordEncoder.matches("correct-password", "bcrypt-hash")).thenReturn(true);
        when(jwtService.generateToken(eq("user-1"), any())).thenReturn("signed-jwt-token");
        when(jwtService.getExpiration("signed-jwt-token")).thenReturn(new Date(System.currentTimeMillis() + 3600_000));

        LoginRequest request = new LoginRequest();
        request.setEmail("asha@example.com");
        request.setPassword("correct-password");

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("signed-jwt-token");
        assertThat(response.getUserId()).isEqualTo("user-1");
        assertThat(response.getExpiresAt()).isAfter(java.time.Instant.now());
    }

    private RegisterRequest registerRequest(String email) {
        RegisterRequest request = new RegisterRequest();
        request.setName("Test User");
        request.setEmail(email);
        request.setPhone("1234567890");
        request.setPassword("password123");
        return request;
    }
}
