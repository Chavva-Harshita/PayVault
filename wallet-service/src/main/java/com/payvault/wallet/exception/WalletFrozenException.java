package com.payvault.wallet.exception;

public class WalletFrozenException extends RuntimeException {

    public WalletFrozenException(String walletId) {
        super("Wallet is frozen and cannot be debited or credited: " + walletId);
    }
}
