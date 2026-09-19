package com.payvault.transaction.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * payvault_transactions.ledger_entries
 *
 * Append-only: the application layer never updates or deletes a document
 * in this collection. Every transfer produces exactly two entries (one
 * DEBIT on the sender's wallet, one CREDIT on the receiver's wallet), which
 * makes this collection a full, replayable audit trail independent of
 * whatever the current wallets.balance value happens to be.
 */
@Document(collection = "ledger_entries")
public class LedgerEntry {

    @Id
    private String id;

    @Indexed(unique = true)
    private String ledgerEntryId;

    @Indexed
    private String transactionId;

    @Indexed
    private String walletId;

    @Indexed
    private String userId;

    private EntryType entryType;

    private long amount;

    private long balanceBefore;

    private long balanceAfter;

    private Instant timestamp;

    public LedgerEntry() {
    }

    public LedgerEntry(String ledgerEntryId, String transactionId, String walletId, String userId,
                        EntryType entryType, long amount, long balanceBefore, long balanceAfter) {
        this.ledgerEntryId = ledgerEntryId;
        this.transactionId = transactionId;
        this.walletId = walletId;
        this.userId = userId;
        this.entryType = entryType;
        this.amount = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.timestamp = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLedgerEntryId() {
        return ledgerEntryId;
    }

    public void setLedgerEntryId(String ledgerEntryId) {
        this.ledgerEntryId = ledgerEntryId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
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

    public EntryType getEntryType() {
        return entryType;
    }

    public void setEntryType(EntryType entryType) {
        this.entryType = entryType;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public long getBalanceBefore() {
        return balanceBefore;
    }

    public void setBalanceBefore(long balanceBefore) {
        this.balanceBefore = balanceBefore;
    }

    public long getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(long balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
