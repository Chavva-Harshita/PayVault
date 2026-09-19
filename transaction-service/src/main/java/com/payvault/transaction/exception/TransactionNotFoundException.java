package com.payvault.transaction.exception;

public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(String transactionId) {
        super("No transaction found for transactionId: " + transactionId);
    }
}
