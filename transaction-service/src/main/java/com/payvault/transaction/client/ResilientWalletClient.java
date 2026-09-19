package com.payvault.transaction.client;

import com.payvault.transaction.exception.InsufficientBalanceException;
import com.payvault.transaction.exception.ReceiverNotFoundException;
import com.payvault.transaction.exception.ServiceUnavailableException;
import com.payvault.transaction.exception.WalletUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;

/**
 * Wraps WalletClient with a circuit breaker so TransactionService never
 * calls the raw Feign interface directly.
 *
 * IMPORTANT ASYMMETRY, on purpose: getWalletByUserId() is retried (via
 * RetryableWalletLookup) because it's a pure read with no side effects.
 * debit() and credit() are wrapped in a circuit breaker for fail-fast
 * behavior, but are NEVER auto-retried here. wallet-service's debit/credit
 * endpoints are not idempotent by transactionId in this implementation -
 * if a debit's response is lost to a network blip after it already
 * succeeded, blindly retrying it would debit the sender twice. Failing
 * fast and surfacing the error to the caller (who can decide whether to
 * retry the whole /transfer call with the SAME Idempotency-Key, which IS
 * safe - see TransactionService) is the correct behavior here, not a
 * silent automatic retry at this layer.
 */
@Component
public class ResilientWalletClient {

    private static final Logger log = LoggerFactory.getLogger(ResilientWalletClient.class);
    private static final String CIRCUIT_BREAKER_NAME = "walletService";

    private final WalletClient walletClient;
    private final RetryableWalletLookup retryableWalletLookup;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public ResilientWalletClient(WalletClient walletClient,
                                  RetryableWalletLookup retryableWalletLookup,
                                  CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.walletClient = walletClient;
        this.retryableWalletLookup = retryableWalletLookup;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    public WalletDto getWalletByUserId(String userId) {
        return circuitBreakerFactory.create(CIRCUIT_BREAKER_NAME).run(
                () -> retryableWalletLookup.lookupByUserId(userId),
                throwable -> lookupFallback(userId, throwable)
        );
    }

    public WalletMutationResponse debit(String walletId, WalletMutationRequest request) {
        return circuitBreakerFactory.create(CIRCUIT_BREAKER_NAME).run(
                () -> walletClient.debit(walletId, request),
                throwable -> mutationFallback(walletId, throwable)
        );
    }

    public WalletMutationResponse credit(String walletId, WalletMutationRequest request) {
        return circuitBreakerFactory.create(CIRCUIT_BREAKER_NAME).run(
                () -> walletClient.credit(walletId, request),
                throwable -> mutationFallback(walletId, throwable)
        );
    }

    private WalletDto lookupFallback(String userId, Throwable throwable) {
        if (throwable instanceof ReceiverNotFoundException notFound) {
            throw notFound;
        }
        log.error("WALLET-SERVICE circuit open or exhausted retries for userId={}: {}", userId, throwable.getMessage());
        throw new ServiceUnavailableException("WALLET-SERVICE");
    }

    private WalletMutationResponse mutationFallback(String walletId, Throwable throwable) {
        // Definitive business outcomes (insufficient funds, frozen wallet)
        // pass through unchanged - the circuit breaker only intervenes for
        // genuine infrastructure trouble.
        if (throwable instanceof InsufficientBalanceException insufficientBalance) {
            throw insufficientBalance;
        }
        if (throwable instanceof WalletUnavailableException walletUnavailable) {
            throw walletUnavailable;
        }
        log.error("WALLET-SERVICE circuit open for walletId={} during mutation: {}", walletId, throwable.getMessage());
        throw new ServiceUnavailableException("WALLET-SERVICE");
    }
}
