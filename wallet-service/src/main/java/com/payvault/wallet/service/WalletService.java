package com.payvault.wallet.service;

import com.payvault.wallet.exception.InsufficientBalanceException;
import com.payvault.wallet.exception.WalletAlreadyExistsException;
import com.payvault.wallet.exception.WalletFrozenException;
import com.payvault.wallet.exception.WalletNotFoundException;
import com.payvault.wallet.model.Wallet;
import com.payvault.wallet.model.WalletStatus;
import com.payvault.wallet.repository.WalletRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final MongoTemplate mongoTemplate;

    public WalletService(WalletRepository walletRepository, MongoTemplate mongoTemplate) {
        this.walletRepository = walletRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public Wallet createWallet(String userId, String currency) {
        if (walletRepository.existsByUserId(userId)) {
            throw new WalletAlreadyExistsException(userId);
        }
        Wallet wallet = new Wallet(UUID.randomUUID().toString(), userId, currency);
        return walletRepository.save(wallet);
    }

    public Wallet getByUserId(String userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("userId " + userId));
    }

    public Wallet getByWalletId(String walletId) {
        return walletRepository.findByWalletId(walletId)
                .orElseThrow(() -> new WalletNotFoundException("walletId " + walletId));
    }

    /**
     * Atomic debit - the double-spend guard.
     *
     * This is a SINGLE findAndModify call whose filter includes both the
     * wallet identity AND "balance >= amount". MongoDB guarantees a single
     * document's findAndModify is atomic, so two concurrent debit requests
     * against the same wallet cannot both read the same stale balance and
     * both succeed:
     *
     *   Balance = 10,000
     *   Request A: debit 8,000  -> filter matches (10000 >= 8000), balance becomes 2,000
     *   Request B: debit 8,000  -> filter now evaluates against the CURRENT
     *                              document (2,000 >= 8000 is false) -> matches
     *                              nothing -> InsufficientBalanceException
     *
     * Whichever request's update reaches MongoDB second (even by milliseconds)
     * is evaluated against the balance the first one already wrote - there is
     * no window where both reads see 10,000. No application-level locking,
     * no read-then-write race, no multi-document transaction is needed for
     * this specific guarantee.
     *
     * A frozen wallet is checked separately up front - it's a business rule,
     * not part of the concurrency guard, but a wallet can theoretically be
     * frozen between the check and the update; that's an acceptable, narrow
     * gap for this project (a stricter version would fold status into the
     * same atomic filter, e.g. status: "ACTIVE").
     */
    public Wallet debit(String walletId, long amount) {
        Wallet wallet = getByWalletId(walletId);
        if (wallet.getStatus() == WalletStatus.FROZEN) {
            throw new WalletFrozenException(walletId);
        }

        Query query = new Query(Criteria.where("walletId").is(walletId)
                .and("balance").gte(amount)
                .and("status").is(WalletStatus.ACTIVE.name()));

        Update update = new Update()
                .inc("balance", -amount)
                .inc("version", 1)
                .set("updatedAt", Instant.now());

        Wallet updated = mongoTemplate.findAndModify(query, update, Wallet.class);

        if (updated == null) {
            // Either the balance was insufficient at the moment the atomic
            // update ran, or the wallet was frozen in that same instant.
            // Re-check status to report the more specific error.
            Wallet current = getByWalletId(walletId);
            if (current.getStatus() == WalletStatus.FROZEN) {
                throw new WalletFrozenException(walletId);
            }
            throw new InsufficientBalanceException(walletId);
        }

        return getByWalletId(walletId);
    }

    /**
     * Atomic credit. No balance floor check needed (crediting can't go
     * negative), but the wallet must still be ACTIVE.
     */
    public Wallet credit(String walletId, long amount) {
        Wallet wallet = getByWalletId(walletId);
        if (wallet.getStatus() == WalletStatus.FROZEN) {
            throw new WalletFrozenException(walletId);
        }

        Query query = new Query(Criteria.where("walletId").is(walletId)
                .and("status").is(WalletStatus.ACTIVE.name()));

        Update update = new Update()
                .inc("balance", amount)
                .inc("version", 1)
                .set("updatedAt", Instant.now());

        Wallet updated = mongoTemplate.findAndModify(query, update, Wallet.class);

        if (updated == null) {
            throw new WalletFrozenException(walletId);
        }

        return getByWalletId(walletId);
    }
}
