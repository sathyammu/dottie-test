package com.brimmatech.general.config;

import com.brimmatech.docflow.enums.SettingsCategory;
import com.brimmatech.docflow.enums.TaskFailures;
import com.brimmatech.docflow.exception.TaskFailureException;
import com.brimmatech.docflow.v2.services.TenantSettingsService;
import com.brimmatech.docflow.v2.task.TaskFactory;
import com.brimmatech.docflow.v2.task.delegates.UpdateEncompassDelegate;
import com.brimmatech.docflow.v2.task.dto.CreationArgs;
import com.brimmatech.docflow.v2.task.dto.TaskOutputArgs.Execution;
import com.brimmatech.docflow.v2.task.dto.TaskOutputArgs.Suspensions;
import com.brimmatech.docflow.v2.task.dto.TaskStepDef;
import com.brimmatech.docflow.v2.task.globalapi.SuspensionPolicies;
import com.brimmatech.docflow.v2.task.globalapi.SuspensionPolicy;
import com.brimmatech.general.infra.ConnectionProvider;
import com.brimmatech.general.types.ThrowingSupplier;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.Task;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.TaskDecision;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.TaskResult;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.processor.BufferingTaskHandlerFactory;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.processor.TaskHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.util.StopWatch;

import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.brimmatech.docflow.v2.services.TenantSettingsService.DEFAULT_TASK_PROCESSOR_CONFIG;

@Configuration @Slf4j @Profile("!test") public class TaskConfig {

    @Autowired ApplicationContext applicationContext;

    @Autowired ObjectMapper objectMapper;

    @Autowired Supplier<ZonedDateTime> nowUtcZoned;

    @Autowired TaskFactory taskFactory;

    @Autowired TenantSettingsService tenantSettingsService;

    @Bean @Qualifier("task-runner-pool") Executor sequentialExecutor() {
        val exec = new SimpleAsyncTaskExecutor();
        exec.setVirtualThreads(true);
        exec.setConcurrencyLimit(500);
        return exec;
    }

    @Bean BufferingTaskHandlerFactory<ConnectionProvider, SQLException, Void> taskHandlerFactory() {
        log.info("Returning a new instance of BufferingTaskHandlerFactory");

        val
                config =
                tenantSettingsService.getGlobalSettingTyped(SettingsCategory.TASK_PROCESSOR_CONFIG,
                        TenantSettingsService.TaskProcessorConfig.class).orElse(DEFAULT_TASK_PROCESSOR_CONFIG);

        return BufferingTaskHandlerFactory.withoutBaggage(sequentialExecutor(),
                true,
                config.bufferSize(),
                config.workerSize(),
                15,
                TimeUnit.SECONDS,
                topic -> {
                    var enumTopic = TemplateConfig.TOPICS.valueOf(topic);
                    return this.getHandlerForTopic(enumTopic);
                });
    }

    private TaskHandler<ConnectionProvider, SQLException> getHandlerForTopic(TemplateConfig.TOPICS topic) {
        return (tasks, callback, onFailure) -> {
            callback.accept(connection -> tasks.stream()
                    .collect(Collectors.toMap(Function.identity(), task -> {
                        try {
                            return processTask(topic).apply(task);
                        } catch (Exception e) {
                            onFailure.accept(e);
                            return TaskDecision.FAILURE;
                        }
                    })));
        };
    }

    private Function<? super Task, ? extends TaskDecision> processTask(TemplateConfig.TOPICS topic) {
        return task -> {
            final StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            val startTime = nowUtcZoned.get();
            log.info("Exec Task..{}:{}", topic, task.getSequence());
            val processorClazz = switch (topic) {
                case UPDATE_ENCOMPASS -> UpdateEncompassDelegate.class;
                case END_RUN -> null;
                default -> throw new IllegalArgumentException("Unexpected value: " + topic);
            };
            val processor = applicationContext.getBean(processorClazz);
            try {

                val taskDefs = taskFactory.getNextTaskDef(task);
                val nextTaskDef = taskDefs.right;
                val currentTaskDef = taskDefs.left;

                val isCurrentTaskCheckpointForSupervisor = TaskStepDef.hasCheckEnabled(currentTaskDef);

                processor.setup();
                processor.preProcess(topic, task);
                TaskDecision result = TaskDecision.FAILURE;
                try {
                    result = processor.process(task, taskDefs.getRouteName());
                } catch (Exception e) {
                    log.debug("Got exception while processing task", e);
                    result =
                            new TaskDecision(result.getResult(),
                                    new TaskFailureException(TaskFailures.INTERNAL_ERROR.getName(),
                                            String.format("%s\n:%s",
                                                    e.getMessage(),
                                                    Arrays.stream(e.getStackTrace()).limit(2).toList()), e));
                }

                stopWatch.stop();

                TaskDecision finalResult = result;
                ArrayNode
                        collectedLogs =
                        ThrowingSupplier.getCapturingExceptions(() -> (ArrayNode) objectMapper.readTree(finalResult.getThrowable()
                                .orElse(new Throwable("UNKNOWN"))
                                .getMessage())).orElse(objectMapper.createArrayNode());

                val
                        appendedResult =
                        processor.getTaskOutput(task)
                                .appendExecution(Execution.builder()
                                        .endTime(nowUtcZoned.get())
                                        .startTime(startTime)
                                        .processTime(stopWatch.getTotalTimeMillis())
                                        .logs(collectedLogs)
                                        .build());


                if (List.of(TaskResult.FAILURE, TaskResult.SUCCESS).contains(result.getResult())) {
                    Execution last = appendedResult.getInternals().executions().getLast();
                    appendedResult.getInternals().executions().removeLast();
                    Throwable throwable = result.getThrowable().orElse(new Throwable("UNKNOWN"));
                    if ((throwable instanceof TaskFailureException tfe)) {
                        last = last.withErrorCode(tfe.getCode()).withErrorMessage(tfe.getMessage());
                    } else {
                        last =
                                last.withErrorCode(TaskFailures.INTERNAL_ERROR.toString())
                                        .withErrorMessage(throwable.getMessage());
                    }
                    appendedResult.getInternals().executions().add(last);
                    result = new TaskDecision(result.getResult(), objectMapper.writeValueAsString(appendedResult));
                }
                if (!result.getResult().equals(TaskResult.FAILURE)) {
                    result = new TaskDecision(result.getResult(), objectMapper.writeValueAsString(appendedResult));
                    switch (result.getResult()) {
                        case TaskResult.SUCCESS:
                            if (topic.equals(TemplateConfig.TOPICS.NOOP_SUPERVISOR)) {
                                processor.postProcess(topic,
                                        task,
                                        result,
                                        Optional.empty(),
                                        null,
                                        taskDefs.getNextIndex());
                            } else {
                                if (nextTaskDef.isEmpty()) {
                                    processor.postProcess(topic,
                                            task,
                                            result,
                                            nextTaskDef,
                                            null,
                                            taskDefs.getNextIndex());

                                } else {
                                    if (!isCurrentTaskCheckpointForSupervisor) {
                                        val isCheckEnabled = nextTaskDef.get().hasCheckEnabled();


                                        val
                                                nextTaskArgs =
                                                CreationArgs.ChildTaskInput.builder()
                                                        .priority(ThrowingSupplier.getCapturingExceptions(() -> nextTaskDef.get()
                                                                .getCreationMeta()
                                                                .get("priority")
                                                                .asInt()).orElse(0))
                                                        .notificationTopic(nextTaskDef.get()
                                                                .getNotificationTopic()
                                                                .orElse(null))
                                                        .checkingSupervisor(isCheckEnabled ?
                                                                nextTaskDef.get()
                                                                        .getSupervisor()
                                                                        .map(TaskStepDef.SupervisorDef::creationMeta)
                                                                        .orElse(null) :
                                                                null);
                                        processor.postProcess(topic,
                                                task,
                                                result,
                                                nextTaskDef,
                                                nextTaskArgs,
                                                taskDefs.getNextIndex());
                                    }
                                    nextTaskDef.ifPresent(v -> taskFactory.createSupervisorNode(v, task));
                                }
                            }
                            break;
                        case TaskResult.SUSPENSION: {
                            final java.time.Instant now = java.time.Instant.now();

                            final int previous = appendedResult.getInternals().suspensions().size();
                            final int nextCount = previous + 1;

                            final SuspensionPolicy policy = SuspensionPolicies.forTask(task, processor, finalResult);

                            final Optional<ZonedDateTime> firstSuspensionStart =
                                    appendedResult.getInternals().suspensions().stream()
                                            .map(Suspensions::suspendedAt)
                                            .min(java.util.Comparator.naturalOrder());

                            final java.time.Instant scheduleEpoch = firstSuspensionStart
                                    .map(ZonedDateTime::toInstant)
                                    .orElse(now);

                            final Optional<java.time.Duration> nextDelay =
                                    policy.nextDelay(scheduleEpoch, now, nextCount);

                            if (nextDelay.isEmpty()) {
                                Execution last = appendedResult.getInternals().executions().getLast();
                                appendedResult.getInternals().executions().removeLast();

                                last = last.withErrorCode(TaskFailures.MAX_RETRIES.toString())
                                        .withErrorMessage(String.format("Suspensions exceeded max retries:%d",
                                                processor.getMaxSuspensions(processor, task)));

                                appendedResult.getInternals().executions().add(last);
                                return new TaskDecision(TaskResult.FAILURE,
                                        objectMapper.writeValueAsString(appendedResult));
                            }

                            final java.time.ZoneId zone = firstSuspensionStart
                                    .map(ZonedDateTime::getZone)
                                    .orElse(java.time.ZoneOffset.UTC);

                            final ZonedDateTime suspendedAt = ZonedDateTime.ofInstant(now, zone);
                            final ZonedDateTime toBeActiveAt = suspendedAt.plus(nextDelay.get());

                            appendedResult.getInternals()
                                    .suspensions()
                                    .add(new Suspensions(suspendedAt, toBeActiveAt, "Suspension: " + nextCount));

                            result = new TaskDecision(result.getResult(),
                                    objectMapper.writeValueAsString(appendedResult));
                            break;
                        }

                        default: {
                        }
                    }

                }
                return result;
            } catch (Exception e) {

                processor.postProcessOnException(topic, task, e);

                log.debug("Task with topic {} failed with exception: {}", topic, e.getMessage());
                return new TaskDecision(e);
            }
        };
    }


}
