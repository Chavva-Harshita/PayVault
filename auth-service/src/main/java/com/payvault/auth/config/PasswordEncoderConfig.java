package com.payvault.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt: passwords are NEVER stored as plaintext or with a
        // reversible hash. Default strength (10) balances hashing cost
        // against login latency for this project's scale.
        return new BCryptPasswordEncoder();
    }
}
