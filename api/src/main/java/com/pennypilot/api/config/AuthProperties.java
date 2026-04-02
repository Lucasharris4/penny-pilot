package com.pennypilot.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        int passwordMinLength,
        String jwtSecret,
        long jwtExpirationMs
) {}
