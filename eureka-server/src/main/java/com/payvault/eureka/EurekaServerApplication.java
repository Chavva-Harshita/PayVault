package com.payvault.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * PayVault service registry.
 *
 * Every other service (api-gateway, auth-service, user-service, wallet-service,
 * transaction-service) registers itself here on startup, and looks up the
 * current location of other services here instead of using a hardcoded
 * host:port. This is what makes horizontal scaling (multiple wallet-service
 * instances) and fault tolerance possible later in the roadmap.
 *
 * Dashboard: http://localhost:8761
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
