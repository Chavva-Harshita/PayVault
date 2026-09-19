package com.payvault.transaction.client;

/**
 * Mirrors wallet-service's DebitRequest / CreditRequest shape: both take
 * the same {amount, transactionId} fields, so one class covers both calls.
 */
public class WalletMutationRequest {

    private long amount;
    private String transactionId;

    public WalletMutationRequest() {
    }

    public WalletMutationRequest(long amount, String transactionId) {
        this.amount = amount;
        this.transactionId = transactionId;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
}
