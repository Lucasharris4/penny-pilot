package com.pennypilot.api.util;

import java.time.Instant;
import java.util.Date;

public interface Clock {

    Instant now();

    Date nowAsDate();
}
