package com.payvault.transaction.service;

import com.payvault.transaction.client.*;
import com.payvault.transaction.dto.TransferRequest;
import com.payvault.transaction.exception.*;
import com.payvault.transaction.model.*;
import com.payvault.transaction.repository.IdempotencyRepository;
import com.payvault.transaction.repository.LedgerRepository;
import com.payvault.transaction.repository.TransactionRepository;
import com.payvault.transaction.ws.WalletUpdateMessage;
import com.payvault.transaction.ws.WalletUpdateNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
    private static final String CURRENCY = "INR";

    private final TransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final MongoTemplate mongoTemplate;
    private final ResilientUserClient userClient;
    private final ResilientWalletClient walletClient;
    private final WalletUpdateNotifier walletUpdateNotifier;

    public TransactionService(TransactionRepository transactionRepository,
                               LedgerRepository ledgerRepository,
                               IdempotencyRepository idempotencyRepository,
                               MongoTemplate mongoTemplate,
                               ResilientUserClient userClient,
                               ResilientWalletClient walletClient,
                               WalletUpdateNotifier walletUpdateNotifier) {
        this.transactionRepository = transactionRepository;
        this.ledgerRepository = ledgerRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.mongoTemplate = mongoTemplate;
        this.userClient = userClient;
        this.walletClient = walletClient;
        this.walletUpdateNotifier = walletUpdateNotifier;
    }

    /**
     * The full transfer flow, in order:
     *
     *   1. Validate amount / self-transfer
     *   2. Reserve the idempotency key (atomic insert) - duplicate retries
     *      short-circuit here and never reach step 3+
     *   3. Validate receiver exists (User Service)
     *   4. Resolve sender + receiver wallets (Wallet Service)
     *   5. Debit sender (atomic, can fail with INSUFFICIENT_BALANCE)
     *   6. Credit receiver (atomic; if this fails, compensate by crediting
     *      the sender back so money is never "lost" mid-transfer)
     *   7. Persist transaction + two ledger entries, mark idempotency COMPLETED
     */
    public Transaction transfer(String senderId, String idempotencyKey, TransferRequest request) {
        validateAmount(request.getAmount());
        validateNotSelfTransfer(senderId, request.getReceiverId());

        String requestHash = hashRequest(request.getReceiverId(), request.getAmount(), request.getNote());
        Transaction existing = reserveIdempotencyKeyOrReturnExisting(idempotencyKey, senderId, requestHash);
        if (existing != null) {
            return existing;
        }

        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = new Transaction(transactionId, idempotencyKey, senderId,
                request.getReceiverId(), request.getAmount(), CURRENCY, request.getNote());
        transactionRepository.save(transaction);

        try {
            // Fail fast if the receiver doesn't exist as a real, known user
            // before touching any money.
            userClient.getByUserId(request.getReceiverId());

            WalletDto senderWallet = walletClient.getWalletByUserId(senderId);
            WalletDto receiverWallet = walletClient.getWalletByUserId(request.getReceiverId());

            WalletMutationResponse debitResult = walletClient.debit(
                    senderWallet.getWalletId(), new WalletMutationRequest(request.getAmount(), transactionId));

            WalletMutationResponse creditResult;
            try {
                creditResult = walletClient.credit(
                        receiverWallet.getWalletId(), new WalletMutationRequest(request.getAmount(), transactionId));
            } catch (RuntimeException creditFailure) {
                // Compensating action: the debit already succeeded, so if the
                // credit fails we must give the sender's money back rather
                // than leave it debited with nothing credited anywhere.
                log.error("Credit failed after successful debit for transactionId={}, compensating sender", transactionId);
                walletClient.credit(senderWallet.getWalletId(),
                        new WalletMutationRequest(request.getAmount(), transactionId + "-compensation"));
                throw creditFailure;
            }

            recordLedgerEntries(transactionId, senderWallet, receiverWallet, request.getAmount(), debitResult, creditResult);

            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setCompletedAt(Instant.now());
            transactionRepository.save(transaction);

            markIdempotencyCompleted(idempotencyKey, transactionId);

            // Fire-and-forget: the transfer is already committed above, so a
            // disconnected socket or notification failure here can never
            // affect whether the transfer itself succeeded.
            notifyParties(transaction, debitResult, creditResult);

            return transaction;

        } catch (RuntimeException ex) {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setCompletedAt(Instant.now());
            transaction.setFailureReason(ex.getMessage());
            transactionRepository.save(transaction);
            markIdempotencyFailed(idempotencyKey);
            throw ex;
        }
    }

    private void validateAmount(long amount) {
        if (amount <= 0) {
            throw new InvalidAmountException();
        }
    }

    private void validateNotSelfTransfer(String senderId, String receiverId) {
        if (senderId.equals(receiverId)) {
            throw new SelfTransferException();
        }
    }

    /**
     * Returns the existing transaction if this key was already completed;
     * returns null (proceed with a new transfer) if the key is fresh or
     * belonged to a previously failed attempt; throws if a request with
     * this key is still in flight or was reused with a different payload.
     */
    private Transaction reserveIdempotencyKeyOrReturnExisting(String idempotencyKey, String userId, String requestHash) {
        var existingOpt = idempotencyRepository.findByIdempotencyKey(idempotencyKey);

        if (existingOpt.isPresent()) {
            IdempotencyRecord existing = existingOpt.get();

            if (!existing.getRequestHash().equals(requestHash)) {
                throw new IdempotencyKeyReusedException();
            }

            return switch (existing.getStatus()) {
                case COMPLETED -> transactionRepository.findByTransactionId(existing.getTransactionId())
                        .orElseThrow(() -> new TransactionNotFoundException(existing.getTransactionId()));
                case IN_PROGRESS -> throw new DuplicateRequestInProgressException();
                case FAILED -> {
                    // Previous attempt truly failed and nothing persisted
                    // successfully - safe to let this same key try again.
                    idempotencyRepository.delete(existing);
                    insertIdempotencyRecord(idempotencyKey, userId, requestHash);
                    yield null;
                }
            };
        }

        insertIdempotencyRecord(idempotencyKey, userId, requestHash);
        return null;
    }

    private void insertIdempotencyRecord(String idempotencyKey, String userId, String requestHash) {
        try {
            mongoTemplate.insert(new IdempotencyRecord(idempotencyKey, userId, requestHash));
        } catch (DuplicateKeyException raceLostToConcurrentRequest) {
            // Two requests with the same key arrived at almost the same
            // instant and both passed the findByIdempotencyKey check above
            // before either inserted. The unique index is the real guard;
            // whichever loses this race simply reports "already in progress".
            throw new DuplicateRequestInProgressException();
        }
    }

    private void markIdempotencyCompleted(String idempotencyKey, String transactionId) {
        idempotencyRepository.findByIdempotencyKey(idempotencyKey).ifPresent(record -> {
            record.setStatus(IdempotencyStatus.COMPLETED);
            record.setTransactionId(transactionId);
            idempotencyRepository.save(record);
        });
    }

    private void markIdempotencyFailed(String idempotencyKey) {
        idempotencyRepository.findByIdempotencyKey(idempotencyKey).ifPresent(record -> {
            record.setStatus(IdempotencyStatus.FAILED);
            idempotencyRepository.save(record);
        });
    }

    private void notifyParties(Transaction transaction, WalletMutationResponse debitResult, WalletMutationResponse creditResult) {
        walletUpdateNotifier.notify(transaction.getSenderId(), new WalletUpdateMessage(
                transaction.getTransactionId(), "DEBIT", transaction.getAmount(),
                debitResult.getBalanceAfter(), transaction.getCurrency(),
                transaction.getReceiverId(), transaction.getCompletedAt()));

        walletUpdateNotifier.notify(transaction.getReceiverId(), new WalletUpdateMessage(
                transaction.getTransactionId(), "CREDIT", transaction.getAmount(),
                creditResult.getBalanceAfter(), transaction.getCurrency(),
                transaction.getSenderId(), transaction.getCompletedAt()));
    }

    private void recordLedgerEntries(String transactionId, WalletDto senderWallet, WalletDto receiverWallet,
                                      long amount, WalletMutationResponse debitResult, WalletMutationResponse creditResult) {
        long senderBalanceAfter = debitResult.getBalanceAfter();
        long senderBalanceBefore = senderBalanceAfter + amount;

        long receiverBalanceAfter = creditResult.getBalanceAfter();
        long receiverBalanceBefore = receiverBalanceAfter - amount;

        ledgerRepository.save(new LedgerEntry(UUID.randomUUID().toString(), transactionId,
                senderWallet.getWalletId(), senderWallet.getUserId(), EntryType.DEBIT,
                amount, senderBalanceBefore, senderBalanceAfter));

        ledgerRepository.save(new LedgerEntry(UUID.randomUUID().toString(), transactionId,
                receiverWallet.getWalletId(), receiverWallet.getUserId(), EntryType.CREDIT,
                amount, receiverBalanceBefore, receiverBalanceAfter));
    }

    private String hashRequest(String receiverId, long amount, String note) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String raw = receiverId + ":" + amount + ":" + (note == null ? "" : note);
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed available on every standard JVM.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public Transaction getByTransactionId(String transactionId, String requestingUserId) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        if (!transaction.getSenderId().equals(requestingUserId) && !transaction.getReceiverId().equals(requestingUserId)) {
            throw new ForbiddenTransactionAccessException();
        }

        return transaction;
    }
}
