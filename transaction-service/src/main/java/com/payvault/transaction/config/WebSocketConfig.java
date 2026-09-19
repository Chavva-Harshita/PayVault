package com.payvault.transaction.config;

import com.payvault.transaction.security.StompAuthChannelInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Plain STOMP over native WebSocket - deliberately no SockJS fallback.
 * Every modern browser supports WebSocket natively, and SockJS's HTTP
 * long-polling fallback endpoints would need their own extra Gateway
 * routing to work through the same "everything goes through :8080" design
 * from Phase 7. Keeping this to one real WebSocket endpoint matches the
 * project's "don't over-engineer this" guidance for this phase.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
    private final String allowedOrigin;

    public WebSocketConfig(StompAuthChannelInterceptor stompAuthChannelInterceptor,
                            @Value("${app.cors.allowed-origin}") String allowedOrigin) {
        this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
        this.allowedOrigin = allowedOrigin;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns(allowedOrigin);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // /topic for future broadcast-style messages (none yet); /queue is
        // what backs convertAndSendToUser's per-user destinations.
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        // setUserDestinationPrefix defaults to "/user" already - left
        // implicit here, but that's the prefix the frontend subscribes
        // under (see socketService.js: "/user/queue/wallet-updates").
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
