package com.payvault.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Orchestrates transfers. Owns transactions, ledger entries, and
 * idempotency records - but NEVER writes a wallet balance directly. Every
 * balance change goes through wallet-service's debit()/credit() over
 * OpenFeign, so wallet-service remains the single source of truth for money.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableRetry
public class TransactionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionServiceApplication.class, args);
    }
}
