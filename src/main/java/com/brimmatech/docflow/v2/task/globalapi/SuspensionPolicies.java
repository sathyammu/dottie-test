package com.brimmatech.docflow.v2.task.globalapi;

import com.brimmatech.docflow.v2.task.delegates.TaskDelegate;
import com.brimmatech.docflow.v2.task.dto.CreationArgs;
import lombok.val;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.Task;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.TaskDecision;

import java.util.List;
import java.util.function.Function;


public final class SuspensionPolicies {
    private SuspensionPolicies() {}

    public static SuspensionPolicy forTask(Task task,
                                           TaskDelegate<?, ?> processor, TaskDecision result) {
        int maxSuspensions = processor.getMaxSuspensions(processor, task);

        //TODO @Hari: Move this to TaskInput . Extract this as a method in TaskUtils.
        val input = processor.getTopicInput(task, CreationArgs.TaskInputFromDex.class);

        List<CreationArgs.SuspensionSpecConfig.Entry> cfg = input.isPresent()
                && input.get().suspensionSpec() != null
                && !input.get().suspensionSpec().isEmpty()
                ? input.get().suspensionSpec()
                : null;

        if (cfg != null) {

            CreationArgs.SuspensionSpecConfig suspensionSpecConfig = CreationArgs
                    .SuspensionSpecConfig.builder().entries(cfg).build();

            return WindowedSuspensionPolicy.from(
                    suspensionSpecConfig,
                    /* carryLastIndefinitely = */ true,
                    maxSuspensions);
        }


        Function<Integer, Integer> nextActiveAtDurationSecs = processor.getNextActiveAtDurationSecs(5, result);

        return new ExponentialBackoffPolicy(
                nextActiveAtDurationSecs::apply,
                maxSuspensions);
    }
}
