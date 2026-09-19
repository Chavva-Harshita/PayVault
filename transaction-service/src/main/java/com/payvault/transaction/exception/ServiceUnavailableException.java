package com.payvault.transaction.exception;

/**
 * Thrown by the circuit breaker fallback in ResilientUserClient /
 * ResilientWalletClient when a downstream service is unreachable, timing
 * out repeatedly, or the circuit is currently open. Deliberately distinct
 * from InsufficientBalanceException/ReceiverNotFoundException - those are
 * definitive business answers ("no", not "try later"), whereas this one
 * means "we don't actually know, the infrastructure failed us."
 */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String serviceName) {
        super(serviceName + " is currently unavailable - please try again shortly");
    }
}
