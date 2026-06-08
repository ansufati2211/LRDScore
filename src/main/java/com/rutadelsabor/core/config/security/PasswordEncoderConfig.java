package com.rutadelsabor.core.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Fuerza de 10 es el estándar actual. Seguro pero no ralentiza el login.
        return new BCryptPasswordEncoder(10);
    }
}