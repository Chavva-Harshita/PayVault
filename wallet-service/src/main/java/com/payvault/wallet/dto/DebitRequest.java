package com.payvault.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * POST /api/wallets/{walletId}/debit - internal, called by
 * transaction-service (Phase 6) during a transfer. amount must be strictly
 * positive; the debit itself can still fail with INSUFFICIENT_BALANCE at
 * the atomic-update layer even if this DTO validates fine.
 */
public class DebitRequest {

    @Positive(message = "amount must be greater than zero")
    private long amount;

    @NotBlank(message = "transactionId is required")
    private String transactionId;

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
