package com.payvault.transaction.dto;

import com.payvault.transaction.model.TransactionStatus;
import com.payvault.transaction.model.TransactionType;

import java.time.Instant;

public class TransactionResponse {

    private String transactionId;
    private String senderId;
    private String receiverId;
    private long amount;
    private String currency;
    private TransactionStatus status;
    private TransactionType type;
    private String note;
    private Instant createdAt;
    private Instant completedAt;
    private String failureReason;

    public TransactionResponse() {
    }

    public TransactionResponse(String transactionId, String senderId, String receiverId, long amount,
                                String currency, TransactionStatus status, TransactionType type, String note,
                                Instant createdAt, Instant completedAt, String failureReason) {
        this.transactionId = transactionId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.type = type;
        this.note = note;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.failureReason = failureReason;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

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

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}
