package com.payvault.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payvault.gateway.security.JwtValidator;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs on every request routed through the Gateway. Two responsibilities:
 *
 *   1. Strip any client-supplied X-User-Id header, on every path, public
 *      or not - a caller must never be able to assert their own identity
 *      by just setting a header. This is what makes it safe for
 *      downstream services (user-service, wallet-service,
 *      transaction-service) to trust X-User-Id unconditionally: it is now
 *      only ever set here, after real verification.
 *
 *   2. For any path not on the public allowlist, require a valid
 *      "Authorization: Bearer <token>" header, validate it via
 *      JwtValidator, and set X-User-Id to the token's subject before
 *      forwarding. Reject with 401 otherwise.
 *
 * This filter is intentionally a plain GlobalFilter rather than a full
 * Spring Security reactive filter chain - simpler to read for a project
 * at this scale, while still centralizing JWT validation in exactly one
 * place at the edge.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/actuator/**",
            // A browser's native WebSocket handshake cannot carry a custom
            // Authorization header (unlike a normal fetch/XHR call) - it's
            // a browser platform limitation, not something fixable here.
            // transaction-service's STOMP layer authenticates the
            // connection itself once it's established, by reading an
            // Authorization header on the STOMP CONNECT frame (a text
            // frame sent over the already-open socket, which has no such
            // restriction). See transaction-service's StompAuthChannelInterceptor.
            "/ws/**"
    );

    private final JwtValidator jwtValidator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtAuthenticationFilter(JwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        ServerHttpRequest.Builder sanitized = request.mutate().headers(headers -> headers.remove("X-User-Id"));

        if (isPublicPath(path)) {
            return chain.filter(exchange.mutate().request(sanitized.build()).build());
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return reject(exchange, path, "Missing or malformed Authorization header");
        }

        String token = authHeader.substring(7);
        String userId = jwtValidator.validateAndExtractUserId(token);
        if (userId == null) {
            return reject(exchange, path, "Invalid or expired token");
        }

        ServerHttpRequest mutatedRequest = sanitized.header("X-User-Id", userId).build();
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        // Run early - before routing - so an unauthenticated request never
        // reaches a downstream service at all.
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    private Mono<Void> reject(ServerWebExchange exchange, String path, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", 401);
        body.put("error", "UNAUTHENTICATED");
        body.put("message", message);
        body.put("path", path);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            bytes = ("{\"error\":\"UNAUTHENTICATED\"}").getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
