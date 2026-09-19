package com.payvault.wallet.controller;

import com.payvault.wallet.dto.*;
import com.payvault.wallet.exception.MissingUserContextException;
import com.payvault.wallet.model.Wallet;
import com.payvault.wallet.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    private static final String DEFAULT_CURRENCY = "INR";

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * POST /api/wallets
     *
     * Creates a wallet for the authenticated user. Like user-service, reads
     * identity from X-User-Id for now - the Gateway injects this header
     * automatically starting Phase 7.
     */
    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        requireUserId(userId);
        Wallet wallet = walletService.createWallet(userId, DEFAULT_CURRENCY);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(wallet));
    }

    /**
     * GET /api/wallets/me
     */
    @GetMapping("/me")
    public ResponseEntity<WalletResponse> getMyWallet(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        requireUserId(userId);
        Wallet wallet = walletService.getByUserId(userId);
        return ResponseEntity.ok(toResponse(wallet));
    }

    /**
     * GET /api/wallets/me/balance
     */
    @GetMapping("/me/balance")
    public ResponseEntity<BalanceResponse> getMyBalance(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        requireUserId(userId);
        Wallet wallet = walletService.getByUserId(userId);
        return ResponseEntity.ok(new BalanceResponse(wallet.getBalance(), wallet.getCurrency()));
    }

    /**
     * GET /api/wallets/user/{userId} - internal.
     *
     * Lets transaction-service (Phase 6) resolve a userId to a walletId
     * before calling debit/credit, without exposing this lookup publicly
     * through the gateway.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<WalletResponse> getWalletByUserId(@PathVariable String userId) {
        Wallet wallet = walletService.getByUserId(userId);
        return ResponseEntity.ok(toResponse(wallet));
    }

    /**
     * POST /api/wallets/{walletId}/debit - internal.
     *
     * Called by transaction-service during a transfer. See
     * WalletService.debit() for exactly how this prevents double-spending
     * under concurrent requests.
     */
    @PostMapping("/{walletId}/debit")
    public ResponseEntity<WalletMutationResponse> debit(@PathVariable String walletId,
                                                          @Valid @RequestBody DebitRequest request) {
        Wallet wallet = walletService.debit(walletId, request.getAmount());
        return ResponseEntity.ok(new WalletMutationResponse(wallet.getWalletId(), wallet.getBalance()));
    }

    /**
     * POST /api/wallets/{walletId}/credit - internal.
     */
    @PostMapping("/{walletId}/credit")
    public ResponseEntity<WalletMutationResponse> credit(@PathVariable String walletId,
                                                           @Valid @RequestBody CreditRequest request) {
        Wallet wallet = walletService.credit(walletId, request.getAmount());
        return ResponseEntity.ok(new WalletMutationResponse(wallet.getWalletId(), wallet.getBalance()));
    }

    private void requireUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new MissingUserContextException();
        }
    }

    private WalletResponse toResponse(Wallet wallet) {
        return new WalletResponse(
                wallet.getWalletId(),
                wallet.getUserId(),
                wallet.getBalance(),
                wallet.getCurrency(),
                wallet.getStatus()
        );
    }
}
