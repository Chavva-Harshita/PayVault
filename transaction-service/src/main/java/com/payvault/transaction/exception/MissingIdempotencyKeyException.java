package com.payvault.transaction.exception;

public class MissingIdempotencyKeyException extends RuntimeException {

    public MissingIdempotencyKeyException() {
        super("Missing Idempotency-Key header - required on every transfer attempt");
    }
}
