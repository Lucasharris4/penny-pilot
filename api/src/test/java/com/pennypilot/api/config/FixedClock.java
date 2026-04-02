package com.pennypilot.api.config;

import java.time.Instant;
import java.util.Date;

public class FixedClock implements Clock {

    private Instant fixedTime;

    public FixedClock(Instant fixedTime) {
        this.fixedTime = fixedTime;
    }

    @Override
    public Instant now() {
        return fixedTime;
    }

    @Override
    public Date nowAsDate() {
        return Date.from(fixedTime);
    }

    public void setTime(Instant newTime) {
        this.fixedTime = newTime;
    }
}
