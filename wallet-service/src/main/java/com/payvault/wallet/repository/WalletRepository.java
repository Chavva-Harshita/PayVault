package com.payvault.wallet.repository;

import com.payvault.wallet.model.Wallet;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface WalletRepository extends MongoRepository<Wallet, String> {

    Optional<Wallet> findByWalletId(String walletId);

    Optional<Wallet> findByUserId(String userId);

    boolean existsByUserId(String userId);
}
