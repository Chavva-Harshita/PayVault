package com.payvault.transaction.repository;

import com.payvault.transaction.model.IdempotencyRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface IdempotencyRepository extends MongoRepository<IdempotencyRecord, String> {

    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);
}
