package com.payvault.transaction.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * A thin wrapper around SimpMessagingTemplate so TransactionService doesn't
 * need to know Spring Messaging's API directly. If the target user isn't
 * currently connected, convertAndSendToUser() simply has no session to
 * deliver to - this is fire-and-forget, not a guaranteed-delivery channel.
 * That's an acceptable tradeoff here: the REST endpoints remain the source
 * of truth (GET /api/wallets/me/balance, GET /api/transactions), and this
 * socket is purely a "don't make the person hit refresh" convenience on
 * top of that, per the architecture doc's "don't over-engineer this" note
 * for this phase.
 */
@Service
public class WalletUpdateNotifier {

    private static final Logger log = LoggerFactory.getLogger(WalletUpdateNotifier.class);
    private static final String DESTINATION = "/queue/wallet-updates";

    private final SimpMessagingTemplate messagingTemplate;

    public WalletUpdateNotifier(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void notify(String userId, WalletUpdateMessage message) {
        try {
            messagingTemplate.convertAndSendToUser(userId, DESTINATION, message);
        } catch (Exception ex) {
            // Never let a notification failure affect the transfer result -
            // the transfer already succeeded and was persisted before this
            // is called (see TransactionService.transfer()).
            log.warn("Failed to push wallet update to userId={}: {}", userId, ex.getMessage());
        }
    }
}
