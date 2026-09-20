package com.payvault.transaction.service;

import com.payvault.transaction.client.*;
import com.payvault.transaction.dto.TransferRequest;
import com.payvault.transaction.exception.*;
import com.payvault.transaction.model.*;
import com.payvault.transaction.repository.IdempotencyRepository;
import com.payvault.transaction.repository.LedgerRepository;
import com.payvault.transaction.repository.TransactionRepository;
import com.payvault.transaction.ws.WalletUpdateNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private LedgerRepository ledgerRepository;
    @Mock private IdempotencyRepository idempotencyRepository;
    @Mock private MongoTemplate mongoTemplate;
    @Mock private ResilientUserClient userClient;
    @Mock private ResilientWalletClient walletClient;
    @Mock private WalletUpdateNotifier walletUpdateNotifier;

    private TransactionService transactionService;

    private static final String SENDER = "sender-1";
    private static final String RECEIVER = "receiver-1";

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(transactionRepository, ledgerRepository,
                idempotencyRepository, mongoTemplate, userClient, walletClient, walletUpdateNotifier);
        // Fresh key by default in every test unless a test overrides this stub.
        lenient().when(idempotencyRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
    }

    @Test
    void transfer_rejectsZeroOrNegativeAmount() {
        TransferRequest request = requestOf(RECEIVER, 0, null);

        assertThatThrownBy(() -> transactionService.transfer(SENDER, "key-1", request))
                .isInstanceOf(InvalidAmountException.class);

        verifyNoInteractions(userClient, walletClient, transactionRepository);
    }

    @Test
    void transfer_rejectsSelfTransfer() {
        TransferRequest request = requestOf(SENDER, 100, null);

        assertThatThrownBy(() -> transactionService.transfer(SENDER, "key-1", request))
                .isInstanceOf(SelfTransferException.class);

        verifyNoInteractions(userClient, walletClient, transactionRepository);
    }

    @Test
    void transfer_happyPath_marksSuccessAndRecordsTwoLedgerEntriesAndNotifiesBothParties() {
        TransferRequest request = requestOf(RECEIVER, 500, "lunch");

        WalletDto senderWallet = walletDto("w-sender", SENDER, 1000);
        WalletDto receiverWallet = walletDto("w-receiver", RECEIVER, 0);

        when(userClient.getByUserId(RECEIVER)).thenReturn(new UserDto());
        when(walletClient.getWalletByUserId(SENDER)).thenReturn(senderWallet);
        when(walletClient.getWalletByUserId(RECEIVER)).thenReturn(receiverWallet);
        when(walletClient.debit(eq("w-sender"), any())).thenReturn(mutationResponse("w-sender", 500));
        when(walletClient.credit(eq("w-receiver"), any())).thenReturn(mutationResponse("w-receiver", 500));

        Transaction result = transactionService.transfer(SENDER, "key-1", request);

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(result.getSenderId()).isEqualTo(SENDER);
        assertThat(result.getReceiverId()).isEqualTo(RECEIVER);
        assertThat(result.getAmount()).isEqualTo(500);

        verify(ledgerRepository, times(2)).save(any(LedgerEntry.class));
        verify(walletUpdateNotifier, times(2)).notify(any(), any());
        // Saved once as PENDING up front, once more as SUCCESS at the end.
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        // Never called for the compensation path since credit succeeded first try.
        verify(walletClient, times(1)).credit(eq("w-receiver"), any());
    }

    @Test
    void transfer_compensatesSender_whenCreditFailsAfterSuccessfulDebit() {
        TransferRequest request = requestOf(RECEIVER, 500, null);

        WalletDto senderWallet = walletDto("w-sender", SENDER, 1000);
        WalletDto receiverWallet = walletDto("w-receiver", RECEIVER, 0);

        when(userClient.getByUserId(RECEIVER)).thenReturn(new UserDto());
        when(walletClient.getWalletByUserId(SENDER)).thenReturn(senderWallet);
        when(walletClient.getWalletByUserId(RECEIVER)).thenReturn(receiverWallet);
        when(walletClient.debit(eq("w-sender"), any())).thenReturn(mutationResponse("w-sender", 500));
        // First credit call (to the receiver) fails; the compensation
        // credit (back to the sender) should still be attempted and succeed.
        when(walletClient.credit(eq("w-receiver"), any())).thenThrow(new WalletUnavailableException("frozen"));
        when(walletClient.credit(eq("w-sender"), any())).thenReturn(mutationResponse("w-sender", 1000));

        assertThatThrownBy(() -> transactionService.transfer(SENDER, "key-1", request))
                .isInstanceOf(WalletUnavailableException.class);

        // Exactly one compensating credit back to the sender's wallet.
        verify(walletClient, times(1)).credit(eq("w-sender"), any());
        verify(walletClient, times(1)).credit(eq("w-receiver"), any());

        // The transaction record reflects the failure, not a silent partial state.
        verify(transactionRepository, atLeastOnce()).save(argThat(tx ->
                tx.getStatus() == TransactionStatus.FAILED && tx.getFailureReason() != null));

        // No success notification should have gone out for a failed transfer.
        verify(walletUpdateNotifier, never()).notify(any(), any());
    }

    @Test
    void transfer_withCompletedIdempotencyKey_returnsOriginalTransactionWithoutCallingDownstream() {
        TransferRequest request = requestOf(RECEIVER, 500, null);
        String requestHash = hashRequestLikeProductionCode(RECEIVER, 500, null);

        IdempotencyRecord completed = new IdempotencyRecord("key-1", SENDER, requestHash);
        completed.setStatus(IdempotencyStatus.COMPLETED);
        completed.setTransactionId("tx-original");

        Transaction original = new Transaction("tx-original", "key-1", SENDER, RECEIVER, 500, "INR", null);
        original.setStatus(TransactionStatus.SUCCESS);

        when(idempotencyRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(completed));
        when(transactionRepository.findByTransactionId("tx-original")).thenReturn(Optional.of(original));

        Transaction result = transactionService.transfer(SENDER, "key-1", request);

        assertThat(result.getTransactionId()).isEqualTo("tx-original");
        verifyNoInteractions(userClient, walletClient);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_withSameKeyButDifferentPayload_throwsKeyReused() {
        TransferRequest request = requestOf(RECEIVER, 999, null);
        IdempotencyRecord existing = new IdempotencyRecord("key-1", SENDER, "a-completely-different-hash");
        existing.setStatus(IdempotencyStatus.COMPLETED);

        when(idempotencyRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> transactionService.transfer(SENDER, "key-1", request))
                .isInstanceOf(IdempotencyKeyReusedException.class);

        verifyNoInteractions(userClient, walletClient);
    }

    @Test
    void transfer_withInProgressKey_throwsDuplicateRequestInProgress() {
        TransferRequest request = requestOf(RECEIVER, 500, null);
        String requestHash = hashRequestLikeProductionCode(RECEIVER, 500, null);

        IdempotencyRecord inProgress = new IdempotencyRecord("key-1", SENDER, requestHash);
        inProgress.setStatus(IdempotencyStatus.IN_PROGRESS);

        when(idempotencyRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(inProgress));

        assertThatThrownBy(() -> transactionService.transfer(SENDER, "key-1", request))
                .isInstanceOf(DuplicateRequestInProgressException.class);

        verifyNoInteractions(userClient, walletClient);
    }

    @Test
    void transfer_whenTwoRequestsRaceOnTheSameFreshKey_secondLoserGetsDuplicateInProgress() {
        // Both threads see "no existing record" (findByIdempotencyKey ->
        // empty) before either has inserted - the unique index on insert()
        // is the real guard, simulated here by mongoTemplate.insert throwing.
        TransferRequest request = requestOf(RECEIVER, 500, null);
        doThrow(new DuplicateKeyException("duplicate key")).when(mongoTemplate).insert(any(IdempotencyRecord.class));

        assertThatThrownBy(() -> transactionService.transfer(SENDER, "key-1", request))
                .isInstanceOf(DuplicateRequestInProgressException.class);
    }

    private TransferRequest requestOf(String receiverId, long amount, String note) {
        TransferRequest request = new TransferRequest();
        request.setReceiverId(receiverId);
        request.setAmount(amount);
        request.setNote(note);
        return request;
    }

    private WalletDto walletDto(String walletId, String userId, long balance) {
        WalletDto dto = new WalletDto();
        dto.setWalletId(walletId);
        dto.setUserId(userId);
        dto.setBalance(balance);
        dto.setCurrency("INR");
        dto.setStatus("ACTIVE");
        return dto;
    }

    private WalletMutationResponse mutationResponse(String walletId, long balanceAfter) {
        WalletMutationResponse response = new WalletMutationResponse();
        response.setWalletId(walletId);
        response.setBalanceAfter(balanceAfter);
        return response;
    }

    /** Mirrors TransactionService's private hashRequest() exactly, for test setup only. */
    private String hashRequestLikeProductionCode(String receiverId, long amount, String note) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String raw = receiverId + ":" + amount + ":" + (note == null ? "" : note);
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
