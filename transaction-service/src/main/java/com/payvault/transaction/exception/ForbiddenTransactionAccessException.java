package com.payvault.transaction.exception;

public class ForbiddenTransactionAccessException extends RuntimeException {

    public ForbiddenTransactionAccessException() {
        super("You are not authorized to view this transaction");
    }
}
