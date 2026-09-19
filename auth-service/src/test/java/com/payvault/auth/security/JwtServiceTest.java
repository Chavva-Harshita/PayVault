package com.payvault.auth.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    // 32+ bytes, satisfying HS256's minimum key length - see JwtService's
    // constructor comment for why a short secret fails loudly instead of
    // silently producing a weak key.
    private static final String TEST_SECRET = "test-secret-key-at-least-32-bytes-long!!";

    @Test
    void generateToken_thenExtractUserId_roundTripsCorrectly() {
        JwtService jwtService = new JwtService(TEST_SECRET, 60_000);

        String token = jwtService.generateToken("user-123", List.of("ROLE_USER"));

        assertThat(jwtService.extractUserId(token)).isEqualTo("user-123");
        assertThat(jwtService.extractRoles(token)).containsExactly("ROLE_USER");
    }

    @Test
    void isValid_returnsTrue_forFreshlyIssuedToken() {
        JwtService jwtService = new JwtService(TEST_SECRET, 60_000);
        String token = jwtService.generateToken("user-123", List.of("ROLE_USER"));

        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    void isValid_returnsFalse_onceTokenHasExpired() throws InterruptedException {
        // A 1ms expiration means the token is already stale by the time we
        // check it, without needing to fake the system clock.
        JwtService jwtService = new JwtService(TEST_SECRET, 1);
        String token = jwtService.generateToken("user-123", List.of("ROLE_USER"));

        Thread.sleep(20);

        assertThat(jwtService.isValid(token)).isFalse();
    }

    @Test
    void isValid_returnsFalse_forGarbageInput() {
        JwtService jwtService = new JwtService(TEST_SECRET, 60_000);

        assertThat(jwtService.isValid("not-a-real-token")).isFalse();
    }

    @Test
    void isValid_returnsFalse_forTokenSignedWithADifferentSecret() {
        JwtService issuer = new JwtService(TEST_SECRET, 60_000);
        JwtService verifierWithDifferentSecret = new JwtService("a-totally-different-secret-32-bytes!!", 60_000);

        String token = issuer.generateToken("user-123", List.of("ROLE_USER"));

        assertThat(verifierWithDifferentSecret.isValid(token)).isFalse();
    }
}
