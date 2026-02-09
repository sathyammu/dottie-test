package com.brimmatech.docflow.v2.task.metrics;

import lombok.Builder;
import lombok.Data;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.TaskDecision;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Data
@Builder
public class TaskMetricsState {
    Map<TaskDecision, AtomicInteger> completionCounts;
    String topic;
    Long tenantId;
    String  tenantName;
    MovingAverage movingAverage;
    Long lastAlertTimes;

    @Builder.Default
    AtomicInteger errorCounts = new AtomicInteger();

    public Map<TaskDecision, AtomicInteger> getCompletionCounts() {
        return completionCounts;
    }

    public void incrementCompletionCount(TaskDecision decision) {
        completionCounts.computeIfAbsent(decision, k -> new AtomicInteger(0)).incrementAndGet();
    }
}
