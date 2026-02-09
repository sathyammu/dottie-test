package com.brimmatech.docflow.v2.task.delegates;

import com.brimmatech.docflow.exception.TaskFailureException;
import com.brimmatech.docflow.v2.services.TaskUtils;
import com.brimmatech.docflow.v2.task.dto.CreationArgs;
import com.brimmatech.docflow.v2.task.dto.CreationArgs.TaskInputArgs;
import com.brimmatech.docflow.v2.task.dto.TaskOutputArgs;
import com.brimmatech.docflow.v2.task.dto.TaskStepDef;
import com.brimmatech.general.config.TemplateConfig.TOPICS;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.val;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.Task;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.TaskDecision;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;


@Data
public abstract class TaskDelegate<IN, OUT> {

    public Supplier<IN> defaultSupplier;
    public Supplier<OUT> defaultTopicOutputSupplier;
    @Autowired
    TaskUtils taskUtils;
    @Autowired
    ObjectMapper objectMapper;
    Class<IN> inputArgsType;
    Class<OUT> outputArgsType;

    public abstract TaskDecision process(Task Task, String routeName)
            throws IOException, InterruptedException, SQLException, TaskFailureException;

    public abstract void preProcess(TOPICS topic, Task task);

    //TODO: Make this return TaskDecision
    public abstract void postProcess(TOPICS topic,
                                     Task task,
                                     TaskDecision result,
                                     Optional<TaskStepDef> nextTaskDef,
                                     CreationArgs.ChildTaskInput.ChildTaskInputBuilder nextTaskArgsBuilder,
                                     int nextIndex)
            throws Exception;

    public abstract void postProcessOnException(TOPICS topic, Task task, Exception e);

    public Optional<IN> getTopicInput(Task task) {
        return taskUtils.getCreationArgs(task, getInputArgsType());
    }

    public <T> Optional<T> getTopicInput(Task task, Class<T> type) {
        return taskUtils.getCreationArgs(task, type);
    }


    public Optional<TaskInputArgs> getTaskInput(Task task) {
        return taskUtils.getTaskInput(task);
    }

    public TaskOutputArgs getTaskOutput(Task task) {
        return taskUtils.getOutput(task);
    }

    public OUT getTopicOutput(Task task) {
        return getTaskOutput(task).getTypedOutput(outputArgsType, objectMapper,
                defaultTopicOutputSupplier);
    }

    public void setup() {
    }

    public int getMaxSuspensions(TaskDelegate<? , ?> processor, Task task) {
        return 3;
    }

    public Function<Integer, Integer> getNextActiveAtDurationSecs(int startDelay, TaskDecision result) {
        return (steps) -> {
            val longValue = Long.valueOf((long) (Math.pow(2L, steps) * startDelay));
            return longValue.intValue();
        };
    }

}