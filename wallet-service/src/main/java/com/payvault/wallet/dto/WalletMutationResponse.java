package com.payvault.wallet.dto;

public class WalletMutationResponse {

    private String walletId;
    private long balanceAfter;

    public WalletMutationResponse() {
    }

    public WalletMutationResponse(String walletId, long balanceAfter) {
        this.walletId = walletId;
        this.balanceAfter = balanceAfter;
    }

    public String getWalletId() {
        return walletId;
    }

    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }

    public long getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(long balanceAfter) {
        this.balanceAfter = balanceAfter;
    }
}
