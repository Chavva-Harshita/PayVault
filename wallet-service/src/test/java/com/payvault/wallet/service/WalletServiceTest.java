package com.payvault.wallet.service;

import com.payvault.wallet.exception.InsufficientBalanceException;
import com.payvault.wallet.exception.WalletAlreadyExistsException;
import com.payvault.wallet.exception.WalletFrozenException;
import com.payvault.wallet.exception.WalletNotFoundException;
import com.payvault.wallet.model.Wallet;
import com.payvault.wallet.model.WalletStatus;
import com.payvault.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * These tests deliberately do NOT touch a real MongoDB - they verify the
 * business rules WalletService layers on top of the atomic update (frozen
 * wallet checks, not-found handling, duplicate wallet creation). The
 * atomic update's actual concurrency guarantee is a real MongoDB behavior
 * and is covered separately in WalletConcurrencyIT, which uses a real
 * database via Testcontainers - mocking MongoTemplate.findAndModify()
 * here would only prove the mock does what we told it to, not that
 * MongoDB itself prevents a double-spend.
 */
@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    private WalletService walletService;

    @BeforeEach
    void setUp() {
        walletService = new WalletService(walletRepository, mongoTemplate);
    }

    @Test
    void createWallet_throwsWhenUserAlreadyHasOne() {
        when(walletRepository.existsByUserId("user-1")).thenReturn(true);

        assertThatThrownBy(() -> walletService.createWallet("user-1", "INR"))
                .isInstanceOf(WalletAlreadyExistsException.class);

        verify(walletRepository, never()).save(any());
    }

    @Test
    void createWallet_savesNewWalletWithZeroBalance() {
        when(walletRepository.existsByUserId("user-1")).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        Wallet result = walletService.createWallet("user-1", "INR");

        assertThat(result.getUserId()).isEqualTo("user-1");
        assertThat(result.getBalance()).isZero();
        assertThat(result.getStatus()).isEqualTo(WalletStatus.ACTIVE);
    }

    @Test
    void getByWalletId_throwsWhenNotFound() {
        when(walletRepository.findByWalletId("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.getByWalletId("missing"))
                .isInstanceOf(WalletNotFoundException.class);
    }

    @Test
    void debit_throwsWalletFrozenException_whenWalletIsFrozen() {
        Wallet frozen = new Wallet("w-1", "user-1", "INR");
        frozen.setStatus(WalletStatus.FROZEN);
        when(walletRepository.findByWalletId("w-1")).thenReturn(Optional.of(frozen));

        assertThatThrownBy(() -> walletService.debit("w-1", 100))
                .isInstanceOf(WalletFrozenException.class);

        // Frozen check happens before the atomic update is even attempted.
        verify(mongoTemplate, never()).findAndModify(any(Query.class), any(Update.class), eq(Wallet.class));
    }

    @Test
    void debit_throwsInsufficientBalance_whenAtomicUpdateMatchesNothing() {
        Wallet active = new Wallet("w-1", "user-1", "INR");
        active.setStatus(WalletStatus.ACTIVE);
        active.setBalance(500);
        // Called once for the up-front frozen check, once again for the
        // post-failure "was it frozen or just short?" check.
        when(walletRepository.findByWalletId("w-1")).thenReturn(Optional.of(active));
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), eq(Wallet.class))).thenReturn(null);

        assertThatThrownBy(() -> walletService.debit("w-1", 1000))
                .isInstanceOf(InsufficientBalanceException.class);
    }

    @Test
    void debit_succeeds_whenAtomicUpdateMatches() {
        Wallet active = new Wallet("w-1", "user-1", "INR");
        active.setStatus(WalletStatus.ACTIVE);
        active.setBalance(1000);

        Wallet afterDebit = new Wallet("w-1", "user-1", "INR");
        afterDebit.setStatus(WalletStatus.ACTIVE);
        afterDebit.setBalance(500);

        when(walletRepository.findByWalletId("w-1"))
                .thenReturn(Optional.of(active))  // up-front frozen check
                .thenReturn(Optional.of(afterDebit)); // final re-fetch for the return value
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), eq(Wallet.class)))
                .thenReturn(afterDebit);

        Wallet result = walletService.debit("w-1", 500);

        assertThat(result.getBalance()).isEqualTo(500);
    }
}
