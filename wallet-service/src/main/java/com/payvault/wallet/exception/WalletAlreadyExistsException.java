package com.payvault.wallet.exception;

public class WalletAlreadyExistsException extends RuntimeException {

    public WalletAlreadyExistsException(String userId) {
        super("A wallet already exists for userId: " + userId);
    }
}
