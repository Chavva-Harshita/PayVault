package com.payvault.wallet.exception;

public class MissingUserContextException extends RuntimeException {

    public MissingUserContextException() {
        super("Missing X-User-Id header - request did not come through an authenticated path");
    }
}
