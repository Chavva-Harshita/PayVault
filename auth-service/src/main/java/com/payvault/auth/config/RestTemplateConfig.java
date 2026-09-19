package com.payvault.auth.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    /**
     * @LoadBalanced makes Spring Cloud LoadBalancer intercept calls made
     * with this RestTemplate, resolving a logical name like "USER-SERVICE"
     * (as used in UserServiceClient) to an actual instance address looked
     * up from Eureka - including picking between multiple instances once
     * Phase 8 adds a second one.
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
