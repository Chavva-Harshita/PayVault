package com.payvault.transaction.client;

import com.payvault.transaction.exception.ReceiverNotFoundException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class RetryableUserLookup {

    private final UserClient userClient;

    public RetryableUserLookup(UserClient userClient) {
        this.userClient = userClient;
    }

    /**
     * Retries on transient/infrastructure exceptions (timeouts, connection
     * refused, unexpected 5xx) but explicitly NOT on ReceiverNotFoundException
     * - a 404 is a definitive "this user does not exist," and retrying it
     * would just waste time re-confirming the same true answer three times.
     */
    @Retryable(
            retryFor = Exception.class,
            noRetryFor = ReceiverNotFoundException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 200, multiplier = 2)
    )
    public UserDto lookup(String userId) {
        return userClient.getByUserId(userId);
    }
}
