package com.payvault.wallet.exception;

/**
 * Thrown when the atomic debit's conditional update matches zero documents,
 * meaning at the instant the update ran, balance was less than the
 * requested amount. See WalletService.debit() for how this ties into
 * preventing double-spending under concurrent requests.
 */
public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(String walletId) {
        super("Insufficient balance in wallet: " + walletId);
    }
}
