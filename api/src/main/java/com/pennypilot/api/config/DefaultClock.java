package com.pennypilot.api.config;

import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class DefaultClock implements Clock {

    @Override
    public Instant now() {
        return Instant.now();
    }
}
