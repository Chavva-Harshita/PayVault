package com.payvault.transaction.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payvault.transaction.client.*;
import com.payvault.transaction.exception.InsufficientBalanceException;
import com.payvault.transaction.ws.WalletUpdateNotifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Exercises the real HTTP -> controller -> service -> MongoDB path for the
 * transfer flow. Only user-service and wallet-service are mocked (via
 * @MockBean on their resilient wrapper beans) - everything transaction-
 * service itself owns (validation, idempotency, ledger, persistence) runs
 * for real against a Testcontainers-backed MongoDB, exactly as it would
 * with a real deployment.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class TransactionTransferIT {

    static final MongoDBContainer MONGO_CONTAINER = new MongoDBContainer("mongo:7");

    @BeforeAll
    static void startContainer() {
        MONGO_CONTAINER.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGO_CONTAINER::getReplicaSetUrl);
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ResilientUserClient userClient;

    @MockBean
    private ResilientWalletClient walletClient;

    @MockBean
    private WalletUpdateNotifier walletUpdateNotifier;

    private static final String SENDER = "it-sender";
    private static final String RECEIVER = "it-receiver";

    @BeforeEach
    void stubDownstreamServices() {
        when(userClient.getByUserId(RECEIVER)).thenReturn(new UserDto());

        WalletDto senderWallet = walletDto("w-sender", SENDER, 10_000);
        WalletDto receiverWallet = walletDto("w-receiver", RECEIVER, 0);
        when(walletClient.getWalletByUserId(SENDER)).thenReturn(senderWallet);
        when(walletClient.getWalletByUserId(RECEIVER)).thenReturn(receiverWallet);

        when(walletClient.debit(eq("w-sender"), any())).thenReturn(mutationResponse("w-sender", 9_500));
        when(walletClient.credit(eq("w-receiver"), any())).thenReturn(mutationResponse("w-receiver", 500));
    }

    @Test
    void fullTransferFlow_succeedsAndIsRetrievableAfterward() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("receiverId", RECEIVER, "amount", 500));
        String idempotencyKey = UUID.randomUUID().toString();

        String responseJson = mockMvc.perform(post("/api/transactions/transfer")
                        .header("X-User-Id", SENDER)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.senderId").value(SENDER))
                .andExpect(jsonPath("$.receiverId").value(RECEIVER))
                .andExpect(jsonPath("$.amount").value(500))
                .andReturn().getResponse().getContentAsString();

        String transactionId = objectMapper.readTree(responseJson).get("transactionId").asText();

        // The transaction actually persisted to MongoDB and is independently readable.
        mockMvc.perform(get("/api/transactions/" + transactionId).header("X-User-Id", SENDER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // A party who wasn't involved cannot read it.
        mockMvc.perform(get("/api/transactions/" + transactionId).header("X-User-Id", "someone-else"))
                .andExpect(status().isForbidden());
    }

    @Test
    void retryingWithSameIdempotencyKeyAndBody_returnsOriginalWithoutDebitingAgain() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("receiverId", RECEIVER, "amount", 500));
        String idempotencyKey = UUID.randomUUID().toString();

        String firstResponse = mockMvc.perform(post("/api/transactions/transfer")
                        .header("X-User-Id", SENDER)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String firstTransactionId = objectMapper.readTree(firstResponse).get("transactionId").asText();

        String secondResponse = mockMvc.perform(post("/api/transactions/transfer")
                        .header("X-User-Id", SENDER)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String secondTransactionId = objectMapper.readTree(secondResponse).get("transactionId").asText();

        org.assertj.core.api.Assertions.assertThat(secondTransactionId).isEqualTo(firstTransactionId);

        // The real proof this worked end-to-end: the sender's wallet was
        // only ever debited ONCE, even though the HTTP call was made twice.
        verify(walletClient, times(1)).debit(eq("w-sender"), any());
    }

    @Test
    void selfTransfer_isRejectedWithoutTouchingDownstream() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("receiverId", SENDER, "amount", 100));

        mockMvc.perform(post("/api/transactions/transfer")
                        .header("X-User-Id", SENDER)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("SELF_TRANSFER_NOT_ALLOWED"));

        verifyNoInteractions(walletClient);
    }

    @Test
    void insufficientBalance_marksTransactionFailedWithReason() throws Exception {
        WalletDto poorSenderWallet = walletDto("w-poor-sender", "it-poor-sender", 10);
        when(walletClient.getWalletByUserId("it-poor-sender")).thenReturn(poorSenderWallet);
        when(walletClient.debit(eq("w-poor-sender"), any())).thenThrow(new InsufficientBalanceException());

        String body = objectMapper.writeValueAsString(Map.of("receiverId", RECEIVER, "amount", 999_999));

        mockMvc.perform(post("/api/transactions/transfer")
                        .header("X-User-Id", "it-poor-sender")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_BALANCE"))
                .andReturn().getResponse().getContentAsString();

        // Never got far enough to credit the receiver.
        verify(walletClient, never()).credit(eq("w-receiver"), any());
    }

    @Test
    void missingIdempotencyKey_isRejected() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("receiverId", RECEIVER, "amount", 100));

        mockMvc.perform(post("/api/transactions/transfer")
                        .header("X-User-Id", SENDER)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MISSING_IDEMPOTENCY_KEY"));
    }

    private WalletDto walletDto(String walletId, String userId, long balance) {
        WalletDto dto = new WalletDto();
        dto.setWalletId(walletId);
        dto.setUserId(userId);
        dto.setBalance(balance);
        dto.setCurrency("INR");
        dto.setStatus("ACTIVE");
        return dto;
    }

    private WalletMutationResponse mutationResponse(String walletId, long balanceAfter) {
        WalletMutationResponse response = new WalletMutationResponse();
        response.setWalletId(walletId);
        response.setBalanceAfter(balanceAfter);
        return response;
    }
}
