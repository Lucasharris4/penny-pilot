package com.pennypilot.api.config;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

@Component
public class DefaultClock implements Clock {

    @Override
    public Instant now() {
        return Instant.now();
    }

    @Override
    public Date nowAsDate() {
        return Date.from(now());
    }
}
