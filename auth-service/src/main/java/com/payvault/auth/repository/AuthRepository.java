package com.payvault.auth.repository;

import com.payvault.auth.model.UserCredentials;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AuthRepository extends MongoRepository<UserCredentials, String> {

    Optional<UserCredentials> findByEmail(String email);

    boolean existsByEmail(String email);
}
