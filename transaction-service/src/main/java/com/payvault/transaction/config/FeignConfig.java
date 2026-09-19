package com.payvault.transaction.config;

import com.payvault.transaction.exception.InsufficientBalanceException;
import com.payvault.transaction.exception.ReceiverNotFoundException;
import com.payvault.transaction.exception.WalletUnavailableException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Without this, a 400/404/409 from user-service or wallet-service would
 * surface here as a generic FeignException, and TransactionService would
 * have to inspect status codes inline at every call site. Centralizing the
 * translation here keeps the orchestration logic in TransactionService
 * readable.
 */
@Configuration
public class FeignConfig {

    @Bean
    public ErrorDecoder errorDecoder() {
        return new TranslatingErrorDecoder();
    }

    static class TranslatingErrorDecoder implements ErrorDecoder {

        private final ErrorDecoder defaultDecoder = new Default();

        @Override
        public Exception decode(String methodKey, Response response) {
            int status = response.status();

            if (status == 404) {
                return new ReceiverNotFoundException("unknown");
            }
            if (status == 400 && methodKey.contains("debit")) {
                return new InsufficientBalanceException();
            }
            if (status == 409) {
                return new WalletUnavailableException("Wallet is not currently available for this operation");
            }

            return defaultDecoder.decode(methodKey, response);
        }
    }
}
