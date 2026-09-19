package com.payvault.transaction.repository;

import com.payvault.transaction.model.LedgerEntry;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface LedgerRepository extends MongoRepository<LedgerEntry, String> {

    List<LedgerEntry> findByTransactionId(String transactionId);
}
