package com.payvault.transaction.exception;

public class WalletUnavailableException extends RuntimeException {

    public WalletUnavailableException(String message) {
        super(message);
    }
}
