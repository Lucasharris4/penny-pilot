package com.pennypilot.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.providers")
public record ProviderProperties(
        boolean mockEnabled
) {}
