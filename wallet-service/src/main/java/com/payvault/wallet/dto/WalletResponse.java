package com.payvault.wallet.dto;

import com.payvault.wallet.model.WalletStatus;

public class WalletResponse {

    private String walletId;
    private String userId;
    private long balance;
    private String currency;
    private WalletStatus status;

    public WalletResponse() {
    }

    public WalletResponse(String walletId, String userId, long balance, String currency, WalletStatus status) {
        this.walletId = walletId;
        this.userId = userId;
        this.balance = balance;
        this.currency = currency;
        this.status = status;
    }

    public String getWalletId() {
        return walletId;
    }

    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getBalance() {
        return balance;
    }

    public void setBalance(long balance) {
        this.balance = balance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public WalletStatus getStatus() {
        return status;
    }

    public void setStatus(WalletStatus status) {
        this.status = status;
    }
}
