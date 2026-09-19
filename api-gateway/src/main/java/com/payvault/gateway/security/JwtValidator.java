package com.payvault.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Mirrors auth-service's JwtService for the parsing/validation half only -
 * this class never generates a token. Both services must be configured
 * with the SAME jwt.secret (see .env.example / JWT_SECRET) or every token
 * auth-service issues will fail validation here.
 */
@Component
public class JwtValidator {

    private final SecretKey signingKey;

    public JwtValidator(@Value("${jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Returns the userId (JWT subject) if the token is well-formed,
     * correctly signed, and not expired. Returns null for anything else -
     * callers treat null as "reject the request", not as an exception to
     * propagate, since an invalid token is an expected, routine case here.
     */
    public String validateAndExtractUserId(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            if (claims.getExpiration().before(new Date())) {
                return null;
            }
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException malformedOrInvalidToken) {
            return null;
        }
    }
}
