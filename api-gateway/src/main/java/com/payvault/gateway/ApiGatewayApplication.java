package com.payvault.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * The single entry point the frontend talks to. Routes requests to backend
 * services by logical Eureka name (never a hardcoded host:port) and
 * validates every JWT at the edge, so downstream services never have to
 * parse a token themselves - they just trust the X-User-Id header this
 * gateway sets after validation.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
