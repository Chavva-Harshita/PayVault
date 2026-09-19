package com.payvault.transaction.security;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * Runs on every inbound STOMP frame, but only acts on CONNECT. Reads an
 * "Authorization: Bearer <token>" STOMP header - a plain text header sent
 * over the already-open WebSocket connection, not an HTTP header, so the
 * browser's WS-handshake header restriction doesn't apply here - validates
 * it, and attaches a UserPrincipal to the STOMP session. Every message
 * after this on the same session is treated as coming from that user.
 *
 * A CONNECT with a missing or invalid token throws, which Spring turns
 * into a STOMP ERROR frame and closes the connection - there is no
 * "connected but anonymous" state for this socket.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtValidator jwtValidator;

    public StompAuthChannelInterceptor(JwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new IllegalArgumentException("Missing Authorization STOMP header on CONNECT");
            }

            String token = authHeader.substring(7);
            String userId = jwtValidator.validateAndExtractUserId(token);

            if (userId == null) {
                throw new IllegalArgumentException("Invalid or expired token on CONNECT");
            }

            accessor.setUser(new UserPrincipal(userId));
        }

        return message;
    }
}
