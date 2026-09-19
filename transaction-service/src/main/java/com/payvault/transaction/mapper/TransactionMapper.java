package com.payvault.transaction.mapper;

import com.payvault.transaction.dto.TransactionResponse;
import com.payvault.transaction.model.Transaction;

public class TransactionMapper {

    private TransactionMapper() {
    }

    public static TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getTransactionId(),
                transaction.getSenderId(),
                transaction.getReceiverId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getStatus(),
                transaction.getType(),
                transaction.getNote(),
                transaction.getCreatedAt(),
                transaction.getCompletedAt(),
                transaction.getFailureReason()
        );
    }
}
