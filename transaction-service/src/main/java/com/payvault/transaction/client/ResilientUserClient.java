package com.payvault.transaction.client;

import com.payvault.transaction.exception.ReceiverNotFoundException;
import com.payvault.transaction.exception.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;

/**
 * Wraps UserClient with a circuit breaker so TransactionService never talks
 * to the raw Feign interface directly. The actual retry logic lives in the
 * separate RetryableUserLookup bean - Spring's @Retryable is AOP-proxy
 * based, so a private (or self-invoked) method annotated @Retryable in this
 * same class would silently never retry; it has to be a public method on a
 * different bean, called through that bean's proxy.
 */
@Component
public class ResilientUserClient {

    private static final Logger log = LoggerFactory.getLogger(ResilientUserClient.class);
    private static final String CIRCUIT_BREAKER_NAME = "userService";

    private final RetryableUserLookup retryableUserLookup;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public ResilientUserClient(RetryableUserLookup retryableUserLookup, CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.retryableUserLookup = retryableUserLookup;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    public UserDto getByUserId(String userId) {
        return circuitBreakerFactory.create(CIRCUIT_BREAKER_NAME).run(
                () -> retryableUserLookup.lookup(userId),
                throwable -> fallback(userId, throwable)
        );
    }

    private UserDto fallback(String userId, Throwable throwable) {
        if (throwable instanceof ReceiverNotFoundException notFound) {
            // A genuine 404 - not a fault-tolerance case, let it propagate
            // as-is so the caller gets RECEIVER_NOT_FOUND, not
            // SERVICE_UNAVAILABLE.
            throw notFound;
        }
        log.error("USER-SERVICE circuit open or exhausted retries for userId={}: {}", userId, throwable.getMessage());
        throw new ServiceUnavailableException("USER-SERVICE");
    }
}
