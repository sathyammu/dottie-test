package com.brimmatech.docflow.v2.task;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.brimmatech.docflow.v2.models.TaskTree;
import com.brimmatech.docflow.v2.repository.TaskTreeRepository;
import com.brimmatech.docflow.v2.task.dto.CreationArgs.*;
import com.brimmatech.docflow.v2.task.dto.EncompassUpdateStrategy;
import com.brimmatech.docflow.v2.task.dto.TaskOutputArgs;
import com.brimmatech.docflow.v2.task.repository.ConnectionProviderTaskRepository;
import com.brimmatech.docflow.v2.tracing.RouteTracingService;
import com.brimmatech.general.config.TemplateConfig.TOPICS;
import com.brimmatech.general.infra.ConnectionProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.Task;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.TaskCreation;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.TaskSink.Insertion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.BiFunction;

@Service @Slf4j public class TaskFactory {

    public static BiFunction<Task, ObjectMapper, TaskInputArgs>
            extractTreeArgs =
            (t, objectMapper) -> t.getInput().map(v -> {
                try {
                    return objectMapper.readValue(v, TaskInputArgs.class);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                    return null;
                }
            }).orElse(null);
    @Autowired
    ConnectionProviderTaskRepository extractDocTypeTaskSink;
    @Autowired TaskTreeRepository taskTreeRepository;
    @Autowired ObjectMapper objectMapper;
    @Autowired ConnectionProvider connectionProvider;
    @Autowired RouteTracingService routeTracingService;

    public Optional<Task> createGenericSuspendedTask(RootTaskInput taskInput) {
        try {

            val task = extractDocTypeTaskSink.push(connectionProvider,
                    taskInput.topic().toString(),
                    Insertion.APPEND, new TaskCreation(taskInput.identifier(),
                            TaskInputArgs.rootTaskInput(taskInput, objectMapper)));

            TaskOutputArgs.Internals suspensionInternals = TaskOutputArgs.Internals.builder()
                    .suspensions(List.of(new TaskOutputArgs.Suspensions(ZonedDateTime.now(), ZonedDateTime.now(),
                            String.format("Suspension: %d", 1))))
                    .executions(List.of(TaskOutputArgs.Execution.builder().startTime(ZonedDateTime.now()).build()))
                    .build();

            TaskOutputArgs output = TaskOutputArgs.builder().internals(suspensionInternals).build();

            updateOutput(output, task);

            return Optional.of(task);
        } catch (SQLException e) {
            log.error("Task creation failed; Topic: {}", taskInput.topic(), e);
        }
        return Optional.empty();
    }

    public void updateOutput(TaskOutputArgs output, Task task) {
        try (Connection conn = connectionProvider.provideConnection()) {
            extractDocTypeTaskSink.updateOutput(task, conn,
                    output);
        } catch (SQLException e) {
            log.error("exception :{}", e.getMessage());
        }
    }

    public void createEndNode(Task completedTask, TOPICS topic) {
        val treeArgs = TaskFactory.extractTreeArgs.apply(completedTask, objectMapper);
        val taskTree = new TaskTree();
        taskTree.setLevels(String.format("%s.%s.__END__", treeArgs.parent(), completedTask.getSequence()));
        taskTree.setQualifier(String.format("%s", treeArgs.qualifier()));
        taskTree.setMeta(objectMapper.createObjectNode().put("topic", topic.getName()));
        taskTreeRepository.save(taskTree);
    }


    public <T> void initiateJobForUpdateEncompass(T payload, String loanId, long tenantId, String stepName) {

        List<Object> notesList;

        if (payload instanceof List<?> list) {
            notesList = new ArrayList<>(list);
        } else {
            notesList = List.of(payload);
        }

        Map<String, List<Object>> notesObject = new HashMap<>();
        notesObject.put(loanId, notesList);

        val taskInput = TaskInputFromDex.builder()
                .notes(notesObject)
                .isRetried(false)
                .build();

        String qualifierId = NanoIdUtils.randomNanoId();

        Map<String, String> parentTraceHeaders = routeTracingService
                .startParentSpan(qualifierId, "UpdateEncompass", Long.parseLong("54"));


        val encompassUpdateInput = EncompassUpdateTaskInput.builder()
                .steps(List.of(
                        EncompassStepInput.builder()
                                .isCompleted(false)
                                .stepName(EncompassUpdateStrategy.valueOf(stepName))
                                .stepArgs(null)
                                .genericFieldUpdateArgs(null)
                                .build()
                ))
                .taskInput(taskInput)
                .traceHeaders(parentTraceHeaders)
                .build();

        JsonNode creationArgs = objectMapper.valueToTree(encompassUpdateInput);

        TaskTree taskTree = new TaskTree();
        taskTree.setQualifier(qualifierId);

        RootTaskInput rootTaskInput = RootTaskInput.builder()
                .tenantId(tenantId)
                .identifier(qualifierId)
                .creationArgs(creationArgs).topic(TOPICS.UPDATE_ENCOMPASS).parent(taskTree).build();

        createGenericSuspendedTask(rootTaskInput);
    }
}
