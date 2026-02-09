package com.brimmatech.docflow.v2.task.delegates;

import com.brimmatech.docflow.enums.TaskFailures;
import com.brimmatech.docflow.exception.LoanLockException;
import com.brimmatech.docflow.exception.TaskFailureException;
import com.brimmatech.docflow.v2.services.EncompassUpdater;
import com.brimmatech.docflow.v2.task.TaskFactory;
import com.brimmatech.docflow.v2.task.dto.CreationArgs;
import com.brimmatech.docflow.v2.task.dto.CreationArgs.EncompassStepInput;
import com.brimmatech.docflow.v2.task.dto.CreationArgs.EncompassUpdateTaskInput;
import com.brimmatech.docflow.v2.task.dto.EncompassUpdateStrategy;
import com.brimmatech.docflow.v2.task.dto.TaskStepDef;
import com.brimmatech.docflow.v2.task.repository.ConnectionProviderTaskRepository;
import com.brimmatech.docflow.v2.tracing.RouteTracingService;
import com.brimmatech.docflow.v2.tracing.TaskTracingService;
import com.brimmatech.general.config.TemplateConfig.TOPICS;
import com.brimmatech.general.infra.ConnectionProvider;
import com.brimmatech.general.types.ThrowingSupplier;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.Task;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.TaskDecision;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.TaskResult;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;


@Component @Scope("prototype") @Slf4j @AllArgsConstructor
public class UpdateEncompassDelegate extends TaskDelegate<EncompassUpdateTaskInput, Object> {

    public static final String GLOBAL_TENANT_NAME = "__vdx__";
    private final static String TASK_NAME = "UPDATE_ENCOMPASS";

    private final EncompassUpdater encompassUpdater;
    private final TaskTracingService taskTracingService;
    private final RouteTracingService routeTracingService;

    ConnectionProviderTaskRepository extractDocTypeTaskSink;
    ConnectionProvider connectionProvider;
    TaskFactory taskFactory;

    public TaskDecision process(Task task, String routeName)

            throws IOException, InterruptedException, SQLException {

        AtomicReference<TaskDecision> taskDecision = new AtomicReference<>(TaskDecision.FAILURE);

        val input = getTopicInput(task);

        val taskInputArgsOptional = getTaskInput(task);

        Map<String, String> customTraceDetails = new HashMap<>();
        customTraceDetails.put("tenantId", String.valueOf(taskInputArgsOptional.get().tenantId()));
        customTraceDetails.put("qualifier", taskInputArgsOptional.get().qualifier());
        customTraceDetails.put("taskSequenceId", String.valueOf(task.getSequence()));

        if (input.isEmpty()) {
            taskDecision.set(new TaskDecision(TaskResult.SUCCESS, "no steps"));
            return taskDecision.get();
        }

        try {
            taskTracingService.executeWithTracing(TASK_NAME, input.get().traceHeaders(), customTraceDetails, () -> {
                String qualifierFromCompletedTask =
                        taskUtils.getTaskInputAsJson(task).get("qualifier").asText();

                long tenantId = taskUtils.getTaskInputAsJson(task).get("tenantId").asLong();


                val outSteps = new java.util.ArrayList<>(List.<EncompassStepInput>of());

                for (final EncompassStepInput step : input.get().steps()) {
                    if (step.isCompleted()) {
                        outSteps.add(step);
                        continue;
                    }

                    try {
                        log.info("StepName: {}, Time: {}", step.stepName(), ZonedDateTime.now());
                        switch (step.stepName()) {
                            case UPDATE_LOAN_NOTES:
                                encompassUpdater.updateNotes(input.get(), tenantId);
                                break;
                            case UPDATE_CONDITION_COMMENTS:
                                encompassUpdater.updateComments(input.get(), tenantId);
                                break;
                            case UPDATE_CONDITION_STATUS:
                                encompassUpdater.updateConditionStatus(input.get(), tenantId);
                                break;
                            case ASSIGN_DOCUMENTS:
                                encompassUpdater.updateConditionDocs(input.get(), tenantId);
                                break;
                            case UPLOAD_ATTACHMENT:
                                encompassUpdater.uploadAttachment(input.get(), tenantId);
                                break;
                            case REMOVE_ATTACHMENT:
                                encompassUpdater.removeAttachment(input.get(), tenantId);
                                break;
                            default:
                                break;
                        }
                        if (taskDecision.get().getResult().equals(TaskResult.SUSPENSION)) {
                            outSteps.add(step.withCompleted(false));
                        } else {
                            outSteps.add(step.withCompleted(true));
                        }
                    } catch (WebClientResponseException e) {
                        if (e.getStatusCode().equals(HttpStatus.CONFLICT)) {
                            taskDecision.set(new TaskDecision(TaskResult.SUSPENSION, e.getMessage()));
                            return taskDecision.get();
                        }
                    } catch (LoanLockException ex) {
                        taskDecision.set(new TaskDecision(TaskResult.SUSPENSION, ex.getMessage()));
                        return taskDecision.get();
                    } catch (Exception e) {
                        log.info("exception while updating encompass", e);
                        // TODO: If retryable exception return SUSPENDED and isCompleted as false;
                        outSteps.add(step.withCompleted(true).withErrorMessage(e.getMessage()));
                    }
                }

                var updatedInput = input.get().withSteps(outSteps);

                final var updatedResult = getTaskInputArgs(task, updatedInput);
                if(!updatedResult.getResult().equals(TaskResult.SUCCESS)) {
                    taskDecision.set(updatedResult);
                }

                return taskDecision;

            });
        } catch (Exception e) {
            String
                    qualifierFromCompletedTask =
                    taskUtils.getTaskInputAsJson(task).get("qualifier").asText();
            routeTracingService.endParentSpan(qualifierFromCompletedTask);

            taskDecision.set(new TaskDecision(TaskResult.FAILURE, e.getMessage()));
            return taskDecision.get();
        }

        return taskDecision.get();
    }

    private TaskDecision getTaskInputArgs(Task task, EncompassUpdateTaskInput updatedInput) {
        val
                updatedTaskInput =
                getTaskInput(task).get().withCreationArgs(objectMapper.valueToTree(updatedInput));

        try (Connection conn = connectionProvider.provideConnection()) {
            extractDocTypeTaskSink.updateInput(task, conn, updatedTaskInput);
        } catch (SQLException e) {
            return new TaskDecision(TaskResult.FAILURE,
                    new TaskFailureException(TaskFailures.ERROR_UPDATE_INPUT.toString(), e.getMessage()));

        }

        return TaskDecision.SUCCESS;
    }


    @Override public void setup() {
        inputArgsType = EncompassUpdateTaskInput.class;
        outputArgsType = Object.class;
    }

    @Override public void preProcess(TOPICS topic, Task task) {
    }

    @Override
    public void postProcess(TOPICS topic,
                            Task completedTask,
                            TaskDecision decision,
                            Optional<TaskStepDef> nextTaskDef,
                            CreationArgs.ChildTaskInput.ChildTaskInputBuilder nextTaskArgsBuilder,
                            int nextIndex) throws Exception {

        if (nextTaskDef.isEmpty()) {
            return;
        }

        val nextTaskTopic = nextTaskDef.get().getTopic();
        if (Objects.requireNonNull(nextTaskTopic) == TOPICS.END_RUN) {

            String
                    qualifierFromCompletedTask =
                    taskUtils.getTaskInputAsJson(completedTask).get("qualifier").asText();

            routeTracingService.endParentSpan(qualifierFromCompletedTask);
//            persistClassifiedDocumentResultEntity(completedTask);
            taskFactory.createEndNode(completedTask, topic);
        }
    }

    @Override public void postProcessOnException(TOPICS topic, Task task, Exception e) {
    }


    public int getMaxSuspensions(TaskDelegate<?, ?> processor, Task task) {
        return 40;
    }

    public Function<Integer, Integer> getNextActiveAtDurationSecs(int startDelay, TaskDecision result) {
        return (steps) -> {
            val defaultDuration = super.getNextActiveAtDurationSecs(startDelay, result).apply(startDelay);
            if (result.getResult().equals(TaskResult.SUSPENSION)) {
                return result.getThrowable()
                        .flatMap(v -> Arrays.stream(v.getSuppressed())
                                .filter(s -> s instanceof TaskFailureException)
                                .findFirst())
                        .map(v -> {
                            val tfe = (TaskFailureException) v;
                            return tfe.getCode();
                        })
                        .flatMap(s -> ThrowingSupplier.getCapturingExceptions(() -> TaskFailures.valueOf(s)))
                        .filter(s -> s.equals(TaskFailures.ENCOMPASS_LOAN_LOCKED))
                        .map(s -> 60 * 5)
                        .orElse(15);
            } else {
                return defaultDuration;
            }
        };
    }

}
