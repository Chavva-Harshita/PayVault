package com.payvault.transaction.ws;

import java.time.Instant;

/**
 * Deliberately small: just enough for the frontend to update the balance
 * figure and prepend one new ledger row without a full refetch. Anything
 * more detailed (e.g. the full TransactionResponse) is a GET
 * /api/transactions/{id} away if the UI ever needs it.
 */
public class WalletUpdateMessage {

    private String transactionId;
    private String direction; // "DEBIT" or "CREDIT" from this recipient's point of view
    private long amount;
    private long balanceAfter;
    private String currency;
    private String counterpartyId;
    private Instant timestamp;

    public WalletUpdateMessage() {
    }

    public WalletUpdateMessage(String transactionId, String direction, long amount, long balanceAfter,
                                String currency, String counterpartyId, Instant timestamp) {
        this.transactionId = transactionId;
        this.direction = direction;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.currency = currency;
        this.counterpartyId = counterpartyId;
        this.timestamp = timestamp;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public long getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(long balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCounterpartyId() {
        return counterpartyId;
    }

    public void setCounterpartyId(String counterpartyId) {
        this.counterpartyId = counterpartyId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
