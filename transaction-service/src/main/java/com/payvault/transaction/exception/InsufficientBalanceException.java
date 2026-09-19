package com.payvault.transaction.exception;

public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException() {
        super("Insufficient wallet balance");
    }
}
