package com.pennypilot.api.config;

import com.pennypilot.api.service.JwtService;
import com.pennypilot.api.util.FixedClock;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Instant;

@TestConfiguration
public class TestJwtConfig {

    @Bean
    JwtService jwtService() {
        AuthProperties props = new AuthProperties(8, "test-secret-key-that-is-long-enough-for-hmac-sha", 86400000L);
        return new JwtService(props, new FixedClock(Instant.now()));
    }
}
