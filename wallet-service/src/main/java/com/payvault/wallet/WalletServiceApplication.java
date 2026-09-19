package com.payvault.wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Owns the wallet balance - the single source of truth for money in
 * PayVault. No other service is allowed to write to payvault_wallets
 * directly; even transaction-service must call debit()/credit() here.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class WalletServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WalletServiceApplication.class, args);
    }
}
