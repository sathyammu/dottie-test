package com.brimmatech.docflow.v2.task;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.brimmatech.docflow.enums.TaskFailures;
import com.brimmatech.docflow.exception.TaskFailureException;
import com.brimmatech.docflow.v2.models.TaskTree;
import com.brimmatech.docflow.v2.repository.TaskPrettyRepository;
import com.brimmatech.docflow.v2.repository.TaskTreeRepository;
import com.brimmatech.docflow.v2.services.TaskUtils;
import com.brimmatech.docflow.v2.services.TenantSettingsService;
import com.brimmatech.docflow.v2.task.dto.CreationArgs.*;
import com.brimmatech.docflow.v2.task.dto.CurNextTaskDefs;
import com.brimmatech.docflow.v2.task.dto.EncompassUpdateStrategy;
import com.brimmatech.docflow.v2.task.dto.TaskOutputArgs;
import com.brimmatech.docflow.v2.task.dto.TaskStepDef;
import com.brimmatech.docflow.v2.task.repository.ConnectionProviderTaskRepository;
import com.brimmatech.docflow.v2.tracing.RouteTracingService;
import com.brimmatech.general.config.TemplateConfig.TOPICS;
import com.brimmatech.general.infra.ConnectionProvider;
import com.brimmatech.general.types.ThrowingSupplier;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.Task;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.TaskCreation;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.TaskDecision;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.TaskResult;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.TaskSink.Insertion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

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
    @Autowired TaskPrettyRepository taskPrettyRepository;
    @Autowired RouteTracingService routeTracingService;
    @Autowired TaskUtils taskUtils;
    @Autowired TenantSettingsService tenantSettingsService;
    @Autowired

    public static TaskDecision getTaskFailure(TaskFailures failure, String humanMessage) {
        return new TaskDecision(TaskResult.FAILURE,
                new TaskFailureException(failure.toString(),
                        humanMessage));
    }

    public Optional<Task> createGenericTask(Task parent, ChildTaskInput newTaskInput) {
        try {
            val
                    task =
                    extractDocTypeTaskSink.push(connectionProvider,
                            newTaskInput.topic().toString(),
                            Insertion.APPEND,
                            getTaskCreation(parent, newTaskInput));
            return Optional.of(task);
        } catch (SQLException e) {
            log.error("Task creation failed; Topic: {}", newTaskInput.topic(), e);
        }
        return Optional.empty();
    }

    public Optional<Task> createGenericTask(RootTaskInput taskInput) {
        try {
            val
                    task =
                    extractDocTypeTaskSink.push(connectionProvider,
                            taskInput.topic().toString(),
                            Insertion.APPEND,
                            taskUtils.getTaskCreation(taskInput));
            return Optional.of(task);
        } catch (SQLException e) {
            log.error("Task creation failed; Topic: {}", taskInput.topic(), e);
        }
        return Optional.empty();
    }

    private TaskCreation getTaskCreation(Task completedTask, ChildTaskInput taskInputFactory) {
        var parentArgs = TaskFactory.extractTreeArgs.apply(completedTask, objectMapper);
        parentArgs =
                taskInputFactory.isInProgress() ?
                        parentArgs.withParent(String.format("%s", parentArgs.parent())) :
                        parentArgs.withParent(String.format("%s.%s", parentArgs.parent(), completedTask.getSequence()));

        var
                taskCreationArgs =
                new TaskCreation(taskInputFactory.identifier(),
                        parentArgs.childTaskInput(taskInputFactory, objectMapper));

        return taskCreationArgs.withSuspension(taskInputFactory.isSuspended());
    }

    public void createChildTask(Task parent, List<ChildTaskInput> newTaskInput) {
        newTaskInput.forEach(v -> createChildTask(parent, v));
    }

    public void createChildTask(Task parent, ChildTaskInput newTaskInput) {
        val inputParts = TaskFactory.extractTreeArgs.apply(parent, objectMapper);
        val childTask = createGenericTask(parent, newTaskInput);
        val
                newPath =
                newTaskInput.isInProgress() ?
                        String.format("%s.%s", inputParts.parent(), parent.getSequence()) :
                        String.format("%s.%s.%s",
                                inputParts.parent(),
                                parent.getSequence(),
                                childTask.get().getSequence());

        val newTreeNode = new TaskTree();
        newTreeNode.setLevels(newPath);
        newTreeNode.setQualifier(inputParts.qualifier());
        newTreeNode.setMeta(objectMapper.createObjectNode().put("topic", newTaskInput.topic().toString()));

        log.info("Adding treenode {} for topic:{}, parent: {}",
                newTreeNode,
                newTaskInput.topic(),
                parent.getSequence());
        taskTreeRepository.save(newTreeNode);

    }

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

    public TOPICS getNextTaskTopic(Task completedTask) {
        return getNextTaskDef(completedTask).right.map(TaskStepDef::getTopic).orElse(null);
    }

    public CurNextTaskDefs getNextTaskDef(Task completedTask) {

        val treeArgs = TaskFactory.extractTreeArgs.apply(completedTask, objectMapper);

        if (!StringUtils.hasText(treeArgs.parent())) {
            return new CurNextTaskDefs(Optional.empty(),
                    Optional.empty(),
                    "",
                    0);
        }

        val travelledPath = new ArrayList<>(List.of(treeArgs.parent().split("\\.")));

        CurNextTaskDefs emptyResult = new CurNextTaskDefs(Optional.empty(), Optional.empty(), null, -1);

        travelledPath.add(String.format("%d", completedTask.getSequence()));

        val
                travelledPathRemovedTaskTree =
                new ArrayList<>(travelledPath.subList(2, travelledPath.size())).stream()
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());

        val tasks = taskPrettyRepository.findBySequenceInOrderByCreatedAsc(travelledPathRemovedTaskTree);

        return taskTreeRepository.findRootByQualifier(treeArgs.qualifier()).map(root -> {
            return taskUtils.pickRouteFromRootMeta(root).map(route -> {
                val configs = route.getConfig();
                for (var i = 0; i < travelledPathRemovedTaskTree.size(); i++) {
                    if (tasks.size() <= i ||
                            configs.size() <= i ||
                            !tasks.get(i).getTopic().equals(configs.get(i).getTopic())) {
                        return emptyResult;
                    }
                }
                return new CurNextTaskDefs(ThrowingSupplier.getCapturingExceptions(() -> {
                    return configs.get(travelledPathRemovedTaskTree.size() - 1);
                }), ThrowingSupplier.getCapturingExceptions(() -> {
                    return configs.get(travelledPathRemovedTaskTree.size());
                }), route.getName(), travelledPathRemovedTaskTree.size());

            }).orElse(emptyResult);
        }).orElse(emptyResult);
    }


    public void persistUnrecognizedStrategy(Task completedTask, String strategy, String category) {
        val inputParts = TaskFactory.extractTreeArgs.apply(completedTask, objectMapper);
        log.error("Unrecognized strategy: {} for category: {}", strategy, category);
        val newTreeNode = new TaskTree();
        newTreeNode.setQualifier(inputParts.qualifier());
        newTreeNode.setLevels(String.format("%s.__END__", inputParts.parent()));
        taskTreeRepository.save(newTreeNode);

    }

    // Supervisor
    public void createSupervisorNode(TaskStepDef taskDef, Task currentTask) {
        taskDef.getSupervisor().ifPresent(sup -> {
            if (sup.creationMeta().hasCheckEnabled()) {
                return;
            }
            val
                    supervisorIdentifier =
                    String.format("%s:%s",
                            taskUtils.getTaskInput(currentTask).map(TaskInputArgs::qualifier).orElse("_missing_"),
                            sup.creationMeta().id());
            log.debug("Creating supervisor node with id:{}", supervisorIdentifier);
            val
                    creationArgs =
                    ChildTaskInput.builder()
                            .creationArgs(objectMapper.valueToTree(sup.creationMeta()))
                            .identifier(supervisorIdentifier)
                            .isSuspended(true)
                            .topic(TOPICS.NOOP_SUPERVISOR)
                            .build();
            createChildTask(currentTask, creationArgs);
        });
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
