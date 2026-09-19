package com.payvault.wallet.service;

import com.payvault.wallet.exception.InsufficientBalanceException;
import com.payvault.wallet.model.Wallet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This is the test that actually matters for Phase 5's core claim: that
 * two concurrent debits against the same wallet cannot both succeed even
 * when they'd individually pass a "balance >= amount" check against a
 * stale read. A mocked repository can't prove this - MongoDB's
 * findAndModify atomicity is a real database behavior, so this test uses
 * one via Testcontainers rather than an in-memory fake.
 *
 * Reproduces exactly the scenario from the architecture doc:
 *   Balance = 10,000
 *   Request A -> debit 8,000
 *   Request B -> debit 8,000
 *   Expected: exactly ONE succeeds, final balance is 2,000 - never both
 *   succeeding (which would leave -6,000) and never both failing.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class WalletConcurrencyIT {

    static final MongoDBContainer MONGO_CONTAINER = new MongoDBContainer("mongo:7");

    @BeforeAll
    static void startContainer() {
        MONGO_CONTAINER.start();
    }

    @AfterAll
    static void stopContainer() {
        MONGO_CONTAINER.stop();
    }

    @DynamicPropertySource
    static void overrideMongoUri(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGO_CONTAINER::getReplicaSetUrl);
        // No real Eureka server exists in this test's context - without
        // this, Spring Cloud would try to register with
        // http://localhost:8761 on every test run and either hang or spam
        // failed-registration warnings.
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Autowired
    private WalletService walletService;

    @Test
    void concurrentDebitsCannotBothSucceed_evenWhenBothWouldPassAStaleBalanceCheck() throws InterruptedException {
        Wallet wallet = walletService.createWallet("concurrency-test-user", "INR");
        walletService.credit(wallet.getWalletId(), 10_000);

        int attempts = 2;
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(attempts);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger insufficientBalanceCount = new AtomicInteger();
        List<Throwable> unexpectedFailures = new CopyOnWriteArrayList<>();

        for (int i = 0; i < attempts; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    walletService.debit(wallet.getWalletId(), 8_000);
                    successCount.incrementAndGet();
                } catch (InsufficientBalanceException expected) {
                    insufficientBalanceCount.incrementAndGet();
                } catch (Throwable unexpected) {
                    unexpectedFailures.add(unexpected);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Release both threads as close to simultaneously as possible, to
        // maximize the chance of them racing against the SAME starting
        // balance if the atomicity guarantee didn't actually hold.
        startGate.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).as("both debit attempts should finish within the timeout").isTrue();
        assertThat(unexpectedFailures).as("no unexpected exceptions").isEmpty();

        // The heart of the test: exactly one of the two ₹8,000 debits
        // against a ₹10,000 balance succeeds - never both (double-spend)
        // and never neither.
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(insufficientBalanceCount.get()).isEqualTo(1);

        Wallet finalState = walletService.getByWalletId(wallet.getWalletId());
        assertThat(finalState.getBalance()).isEqualTo(2_000);
    }

    @Test
    void manyConcurrentDebits_neverOverdraw() throws InterruptedException {
        // A second, higher-concurrency variant of the same guarantee:
        // ten threads each try to take ₹1,000 from a ₹5,000 balance -
        // exactly five should succeed, and the wallet must never go
        // negative no matter how the ten requests interleave.
        Wallet wallet = walletService.createWallet("concurrency-test-user-2", "INR");
        walletService.credit(wallet.getWalletId(), 5_000);

        int attempts = 10;
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(attempts);
        AtomicInteger successCount = new AtomicInteger();

        for (int i = 0; i < attempts; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    walletService.debit(wallet.getWalletId(), 1_000);
                    successCount.incrementAndGet();
                } catch (InsufficientBalanceException ignored) {
                    // Expected once the balance runs out.
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startGate.countDown();
        doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(5);

        Wallet finalState = walletService.getByWalletId(wallet.getWalletId());
        assertThat(finalState.getBalance()).isZero();
        assertThat(finalState.getBalance()).isGreaterThanOrEqualTo(0);
    }
}
