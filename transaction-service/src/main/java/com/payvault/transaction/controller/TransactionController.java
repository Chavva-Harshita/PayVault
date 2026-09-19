package com.payvault.transaction.controller;

import com.payvault.transaction.dto.PagedResponse;
import com.payvault.transaction.dto.TransactionResponse;
import com.payvault.transaction.dto.TransferRequest;
import com.payvault.transaction.exception.MissingIdempotencyKeyException;
import com.payvault.transaction.exception.MissingUserContextException;
import com.payvault.transaction.mapper.TransactionMapper;
import com.payvault.transaction.model.Transaction;
import com.payvault.transaction.repository.TransactionRepository;
import com.payvault.transaction.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionRepository transactionRepository;

    public TransactionController(TransactionService transactionService, TransactionRepository transactionRepository) {
        this.transactionService = transactionService;
        this.transactionRepository = transactionRepository;
    }

    /**
     * POST /api/transactions/transfer
     *
     * senderId comes ONLY from X-User-Id (the authenticated caller) - it is
     * never read from the request body. See TransferRequest for why that
     * field doesn't even exist there.
     */
    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @RequestHeader(value = "X-User-Id", required = false) String senderId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {

        requireUserId(senderId);
        requireIdempotencyKey(idempotencyKey);

        Transaction transaction = transactionService.transfer(senderId, idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionMapper.toResponse(transaction));
    }

    /**
     * GET /api/transactions?page=&size=
     *
     * Returns only transactions where the caller is sender or receiver.
     */
    @GetMapping
    public ResponseEntity<PagedResponse<TransactionResponse>> list(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        requireUserId(userId);

        Page<Transaction> result = transactionRepository.findBySenderIdOrReceiverIdOrderByCreatedAtDesc(
                userId, userId, PageRequest.of(page, size));

        PagedResponse<TransactionResponse> response = new PagedResponse<>(
                result.getContent().stream().map(TransactionMapper::toResponse).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/transactions/{transactionId}
     *
     * TransactionService verifies the caller is a party to the transaction
     * (403 otherwise) before returning it.
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getOne(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable String transactionId) {

        requireUserId(userId);
        Transaction transaction = transactionService.getByTransactionId(transactionId, userId);
        return ResponseEntity.ok(TransactionMapper.toResponse(transaction));
    }

    private void requireUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new MissingUserContextException();
        }
    }

    private void requireIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new MissingIdempotencyKeyException();
        }
    }
}
