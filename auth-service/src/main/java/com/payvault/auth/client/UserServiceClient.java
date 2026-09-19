package com.payvault.auth.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Calls user-service using its logical Eureka name (USER-SERVICE) - never
 * a hardcoded host:port. The RestTemplate bean this depends on is annotated
 * @LoadBalanced (see RestTemplateConfig), so Spring Cloud LoadBalancer
 * resolves "USER-SERVICE" to a real instance address via Eureka on every call.
 */
@Component
public class UserServiceClient {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClient.class);
    private static final String USER_SERVICE_URL = "http://USER-SERVICE/api/users";

    private final RestTemplate restTemplate;

    public UserServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Best-effort call: if user-service is temporarily unreachable, we log
     * and let registration still succeed with credentials created. This is a
     * simple choice for Phase 4; Phase 9 (fault tolerance) revisits whether
     * this should instead be retried, queued, or made part of a stronger
     * consistency guarantee between the two services.
     */
    public void createProfile(String userId, String name, String email, String phone) {
        UserProfileRequest request = new UserProfileRequest(userId, name, email, phone);
        try {
            restTemplate.postForEntity(USER_SERVICE_URL, request, Void.class);
        } catch (RestClientException ex) {
            log.error("Failed to create user-service profile for userId={}: {}", userId, ex.getMessage());
        }
    }
}
