package com.payvault.transaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * "WALLET-SERVICE" is the logical name every wallet-service instance
 * registers under. From Phase 8 onward, when a second instance is running
 * on :8093 alongside the first on :8083, Spring Cloud LoadBalancer picks
 * between them (round-robin by default) on every call made through this
 * client - this interface's code never changes to support that.
 */
@FeignClient(name = "WALLET-SERVICE")
public interface WalletClient {

    @GetMapping("/api/wallets/user/{userId}")
    WalletDto getWalletByUserId(@PathVariable("userId") String userId);

    @PostMapping("/api/wallets/{walletId}/debit")
    WalletMutationResponse debit(@PathVariable("walletId") String walletId, @RequestBody WalletMutationRequest request);

    @PostMapping("/api/wallets/{walletId}/credit")
    WalletMutationResponse credit(@PathVariable("walletId") String walletId, @RequestBody WalletMutationRequest request);
}
