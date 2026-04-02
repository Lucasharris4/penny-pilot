package com.pennypilot.api.config;

import java.time.Instant;
import java.util.Date;

public interface Clock {

    Instant now();

    Date nowAsDate();
}
