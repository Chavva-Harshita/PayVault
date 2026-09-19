package com.payvault.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * POST /api/transactions/transfer body.
 *
 * Deliberately has NO senderId field. The sender is always the caller
 * identified by X-User-Id (derived from the JWT once the Gateway is in
 * place from Phase 7) - never something the client gets to assert. See
 * TransactionController for where senderId actually comes from.
 */
public class TransferRequest {

    @NotBlank(message = "receiverId is required")
    private String receiverId;

    @Positive(message = "amount must be greater than zero")
    private long amount;

    private String note;

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
