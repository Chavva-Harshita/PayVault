package com.payvault.wallet.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Adds an "X-Served-By" response header identifying this exact instance
 * (spring.application.name:server.port). With two wallet-service instances
 * running (:8083 and :8093, see application-instance2.yml), repeatedly
 * calling an endpoint through transaction-service's WalletClient - which
 * resolves "WALLET-SERVICE" via Eureka + Spring Cloud LoadBalancer - should
 * show this header alternating between the two ports, which is the
 * visible proof that load balancing is actually happening and not just
 * configured on paper.
 */
@Component
public class ServedByFilter extends OncePerRequestFilter {

    private final String servedBy;

    public ServedByFilter(@Value("${spring.application.name}") String appName,
                           @Value("${server.port}") String port) {
        this.servedBy = appName + ":" + port;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        response.setHeader("X-Served-By", servedBy);
        filterChain.doFilter(request, response);
    }
}
