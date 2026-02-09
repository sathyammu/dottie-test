package com.brimmatech.docflow.v2.task.globalapi;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.IntUnaryOperator;

public final class ExponentialBackoffPolicy implements SuspensionPolicy {
    private final IntUnaryOperator secondsFn;
    private final int maxSuspensions;

    public ExponentialBackoffPolicy(IntUnaryOperator secondsFn, int maxSuspensions) {
        this.secondsFn = secondsFn;
        this.maxSuspensions = maxSuspensions;
    }

    @Override
    public Optional<Duration> nextDelay(Instant epoch, Instant now, int nextCount) {
        if (maxSuspensions > 0 && nextCount > maxSuspensions) return Optional.empty();
        int secs = secondsFn.applyAsInt(nextCount);
        return Optional.of(Duration.ofSeconds(secs));
    }

    @Override
    public Optional<String> actionForResult(boolean success, Instant epoch, Instant when) {
        return Optional.empty();
    }
}
