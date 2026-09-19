package com.payvault.wallet.dto;

public class BalanceResponse {

    private long balance;
    private String currency;

    public BalanceResponse() {
    }

    public BalanceResponse(long balance, String currency) {
        this.balance = balance;
        this.currency = currency;
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
}
