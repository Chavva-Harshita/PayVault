package com.payvault.wallet.exception;

public class WalletNotFoundException extends RuntimeException {

    public WalletNotFoundException(String identifier) {
        super("No wallet found for: " + identifier);
    }
}
