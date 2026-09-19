package com.payvault.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtValidatorTest {

    private static final String SECRET = "gateway-test-secret-at-least-32-bytes!!";

    @Test
    void validToken_returnsTheSubjectAsUserId() {
        JwtValidator validator = new JwtValidator(SECRET);
        String token = mintToken(SECRET, "user-123", 60_000);

        assertThat(validator.validateAndExtractUserId(token)).isEqualTo("user-123");
    }

    @Test
    void expiredToken_returnsNull() throws InterruptedException {
        JwtValidator validator = new JwtValidator(SECRET);
        String token = mintToken(SECRET, "user-123", 1);

        Thread.sleep(20);

        assertThat(validator.validateAndExtractUserId(token)).isNull();
    }

    @Test
    void tokenSignedWithADifferentSecret_returnsNull() {
        JwtValidator validator = new JwtValidator(SECRET);
        String tamperedToken = mintToken("a-completely-different-secret-32b!!", "user-123", 60_000);

        assertThat(validator.validateAndExtractUserId(tamperedToken)).isNull();
    }

    @Test
    void malformedToken_returnsNullInsteadOfThrowing() {
        JwtValidator validator = new JwtValidator(SECRET);

        assertThat(validator.validateAndExtractUserId("this-is-not-a-jwt")).isNull();
    }

    @Test
    void emptyToken_returnsNull() {
        JwtValidator validator = new JwtValidator(SECRET);

        assertThat(validator.validateAndExtractUserId("")).isNull();
    }

    /** Mints a token the same way auth-service's JwtService does, for test setup only. */
    private String mintToken(String secret, String userId, long expirationMs) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
