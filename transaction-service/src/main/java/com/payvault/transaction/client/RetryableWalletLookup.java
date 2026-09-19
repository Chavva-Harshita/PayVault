package com.payvault.transaction.client;

import com.payvault.transaction.exception.ReceiverNotFoundException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class RetryableWalletLookup {

    private final WalletClient walletClient;

    public RetryableWalletLookup(WalletClient walletClient) {
        this.walletClient = walletClient;
    }

    /**
     * Only the lookup is retried - see ResilientWalletClient for why debit
     * and credit deliberately are not.
     */
    @Retryable(
            retryFor = Exception.class,
            noRetryFor = ReceiverNotFoundException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 200, multiplier = 2)
    )
    public WalletDto lookupByUserId(String userId) {
        return walletClient.getWalletByUserId(userId);
    }
}
