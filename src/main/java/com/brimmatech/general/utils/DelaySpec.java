package com.brimmatech.general.utils;

import java.time.Duration;
import java.util.Optional;

public final class DelaySpec {
    private final Duration period;
    private final Optional<Duration> window;

    private DelaySpec(Duration period, Optional<Duration> window) {
        this.period = period;
        this.window = window;
    }

    public Duration period() { return period; }
    public Optional<Duration> window() { return window; }


    public static DelaySpec parse(String raw) {
        String s = raw.trim();
        if (!s.startsWith("@")) {
            throw new IllegalArgumentException("delaySpec must start with '@': " + raw);
        }
        String body = s.substring(1); // remove '@'
        String[] parts = body.split(":", -1);

        Duration period = DurationParser.parse(parts[0]);
        Optional<Duration> window = Optional.empty();

        if (parts.length >= 2 && !parts[1].isBlank()) {
            String w = parts[1].trim();
            if (!w.equalsIgnoreCase("inf") && !w.equalsIgnoreCase("+inf") && !w.equals("*")) {
                window = Optional.of(DurationParser.parse(w));
            }
        }
        return new DelaySpec(period, window);
    }
}
