package com.payvault.transaction.exception;

public class DuplicateRequestInProgressException extends RuntimeException {

    public DuplicateRequestInProgressException() {
        super("A request with this Idempotency-Key is already being processed");
    }
}
