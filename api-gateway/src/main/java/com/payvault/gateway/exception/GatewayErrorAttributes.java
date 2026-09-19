package com.payvault.gateway.exception;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Without this, a request to a path with no matching route, or one where
 * the target service is completely unreachable, falls back to Spring
 * Boot's default WebFlux error body shape - which looks different from
 * every other service's ErrorResponse. This makes Gateway-level failures
 * (404 no route, 503 no instances available) match that same shape.
 */
@Component
public class GatewayErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        Map<String, Object> defaults = super.getErrorAttributes(request, options);

        Map<String, Object> shaped = new LinkedHashMap<>();
        shaped.put("timestamp", Instant.now().toString());
        shaped.put("status", defaults.getOrDefault("status", 500));
        shaped.put("error", defaults.getOrDefault("error", "GATEWAY_ERROR"));
        shaped.put("message", defaults.getOrDefault("message", "No available route or service"));
        shaped.put("path", defaults.getOrDefault("path", request.path()));

        return shaped;
    }
}
