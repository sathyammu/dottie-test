package com.brimmatech.docflow.v2.task.globalapi;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public interface SuspensionPolicy {


    /**
     * @param scheduleEpoch the reference point for elapsed-time windows (task createdAt or first execution start)
     * @param now           current time
     * @param nextCount     (suspensionsSoFar + 1); use this if your policy also caps by count
     * @return next delay, or empty() if no more suspensions are permitted
     */
    Optional<Duration> nextDelay(Instant scheduleEpoch, Instant now, int nextCount);

    /**
     * Decide which action to emit when the task finishes at 'when'.
     */
    Optional<String> actionForResult(boolean success, Instant scheduleEpoch, Instant when);
}
