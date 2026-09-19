package com.payvault.wallet.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * payvault_wallets.wallets
 *
 * balance is stored as a long in minor currency units (paise for INR) -
 * never double/float, and not BigDecimal either (see the architecture doc's
 * "monetary representation decision" for why: plain integer arithmetic has
 * no rounding ambiguity and no Decimal128 serialization overhead for a
 * simple add/subtract workload like this one).
 *
 * version is Spring Data's optimistic-locking field. It is a second, coarser
 * safety net on top of the atomic findAndModify used in WalletService for
 * debit/credit - useful for any future update path that goes through
 * save() directly instead of the atomic operations.
 */
@Document(collection = "wallets")
public class Wallet {

    @Id
    private String id;

    @Indexed(unique = true)
    private String walletId;

    @Indexed(unique = true)
    private String userId;

    private long balance;

    private String currency;

    private WalletStatus status;

    @Version
    private Long version;

    private Instant createdAt;

    private Instant updatedAt;

    public Wallet() {
    }

    public Wallet(String walletId, String userId, String currency) {
        this.walletId = walletId;
        this.userId = userId;
        this.balance = 0L;
        this.currency = currency;
        this.status = WalletStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
