package com.payvault.transaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * "USER-SERVICE" is the logical name user-service registers under in
 * Eureka (spring.application.name). Feign + Spring Cloud LoadBalancer
 * resolve this to a real instance address at call time - no host:port here.
 */
@FeignClient(name = "USER-SERVICE")
public interface UserClient {

    @GetMapping("/api/users/{userId}")
    UserDto getByUserId(@PathVariable("userId") String userId);
}
