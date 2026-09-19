package com.payvault.transaction.repository;

import com.payvault.transaction.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TransactionRepository extends MongoRepository<Transaction, String> {

    Optional<Transaction> findByTransactionId(String transactionId);

    Page<Transaction> findBySenderIdOrReceiverIdOrderByCreatedAtDesc(String senderId, String receiverId, Pageable pageable);
}
