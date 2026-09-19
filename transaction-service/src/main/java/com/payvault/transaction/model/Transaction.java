package com.payvault.transaction.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * payvault_transactions.transactions
 *
 * referenceId mirrors the client-supplied Idempotency-Key for this
 * transfer attempt, indexed so a transaction can be found by it directly
 * without going through the idempotency_records collection.
 */
@Document(collection = "transactions")
@CompoundIndexes({
        @CompoundIndex(name = "sender_created_idx", def = "{'senderId': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "receiver_created_idx", def = "{'receiverId': 1, 'createdAt': -1}")
})
public class Transaction {

    @Id
    private String id;

    @Indexed(unique = true)
    private String transactionId;

    @Indexed
    private String referenceId;

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

    public Transaction() {
    }

    public Transaction(String transactionId, String referenceId, String senderId, String receiverId,
                        long amount, String currency, String note) {
        this.transactionId = transactionId;
        this.referenceId = referenceId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.amount = amount;
        this.currency = currency;
        this.note = note;
        this.status = TransactionStatus.PENDING;
        this.type = TransactionType.TRANSFER;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
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
