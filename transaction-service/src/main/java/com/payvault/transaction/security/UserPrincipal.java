package com.payvault.transaction.security;

import java.security.Principal;

/**
 * Spring's convertAndSendToUser() resolves the destination "/user/{name}/..."
 * using Principal.getName() of whoever is attached to a given WebSocket
 * session - this is that attachment, set once during STOMP CONNECT by
 * StompAuthChannelInterceptor.
 */
public record UserPrincipal(String userId) implements Principal {

    @Override
    public String getName() {
        return userId;
    }
}
