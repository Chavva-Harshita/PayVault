package com.payvault.transaction.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * payvault_transactions.idempotency_records
 *
 * idempotencyKey has a unique index - see IdempotencyRepository / how it's
 * inserted in TransactionService for how this prevents a retried transfer
 * request from being processed twice. expiresAt is a TTL index so old
 * records are cleaned up automatically instead of growing this collection
 * forever.
 */
@Document(collection = "idempotency_records")
public class IdempotencyRecord {

    @Id
    private String id;

    @Indexed(unique = true)
    private String idempotencyKey;

    private String userId;

    private String requestHash;

    private String transactionId;

    private IdempotencyStatus status;

    private Instant createdAt;

    @Indexed(name = "expiresAt_ttl_idx", expireAfter = "0s")
    private Instant expiresAt;

    public IdempotencyRecord() {
    }

    public IdempotencyRecord(String idempotencyKey, String userId, String requestHash) {
        this.idempotencyKey = idempotencyKey;
        this.userId = userId;
        this.requestHash = requestHash;
        this.status = IdempotencyStatus.IN_PROGRESS;
        this.createdAt = Instant.now();
        // 24h retention window - long enough to absorb any realistic client
        // retry, short enough that this collection doesn't grow unbounded.
        this.expiresAt = Instant.now().plusSeconds(24 * 60 * 60);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public void setRequestHash(String requestHash) {
        this.requestHash = requestHash;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public IdempotencyStatus getStatus() {
        return status;
    }

    public void setStatus(IdempotencyStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
