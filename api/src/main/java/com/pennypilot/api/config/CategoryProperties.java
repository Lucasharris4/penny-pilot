package com.pennypilot.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.categories")
public record CategoryProperties(
        boolean seedOnRegistration,
        List<DefaultCategory> defaults
) {
    public record DefaultCategory(
            String name,
            String icon,
            String color,
            boolean isSubscription
    ) {}
}
