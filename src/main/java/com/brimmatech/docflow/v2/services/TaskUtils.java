package com.brimmatech.docflow.v2.services;

import com.brimmatech.docflow.v2.models.TaskPretty;
import com.brimmatech.docflow.v2.models.TaskRoute;
import com.brimmatech.docflow.v2.models.TaskTree;
import com.brimmatech.docflow.v2.repository.TaskPrettyRepository;
import com.brimmatech.docflow.v2.repository.TenantSettingsRepository;
import com.brimmatech.docflow.v2.task.dto.CreationArgs.RootTaskInput;
import com.brimmatech.docflow.v2.task.dto.CreationArgs.TaskInputArgs;
import com.brimmatech.docflow.v2.task.dto.TaskOutputArgs;
import com.brimmatech.general.types.ThrowingSupplier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.Task;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.TaskCreation;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service @Slf4j @RequiredArgsConstructor public class TaskUtils {

    public static final String START_NODE_ID = "S";

    private final ObjectMapper objectMapper;
    private final TaskPrettyRepository taskPrettyRepository;
    private final TenantSettingsRepository tenantSettingsRepository;


    public TaskCreation getTaskCreation(RootTaskInput taskInputFactory) {
        return new TaskCreation(taskInputFactory.identifier(),
                TaskInputArgs.rootTaskInput(taskInputFactory, objectMapper));
    }

    public JsonNode getTaskInputAsJson(Task task) {
        val sourceTask = taskPrettyRepository.findBySequence(task.getSequence());
        return sourceTask.map(TaskPretty::getInputJson).orElse(objectMapper.createObjectNode());
    }

    public long getTenantId(Task task) {
        return getTaskInputAsJson(task).at("/tenantId").asLong(-1);
    }

    public Optional<TaskInputArgs> getTaskInput(Task task) {
        return task.getInput().flatMap(v -> {
            return ThrowingSupplier.getCapturingExceptions(() -> {
                return objectMapper.readValue(v, TaskInputArgs.class);
            });
        });
    }

    public Optional<TaskInputArgs> getTaskInput(TaskPretty task) {
        return task.getInput().flatMap(v -> {
            return ThrowingSupplier.getCapturingExceptions(() -> {
                return objectMapper.readValue(v, TaskInputArgs.class);
            });
        });
    }

    public <T> Optional<T> getCreationArgs(Task task, Class<T> type) {
        return task.getInput().flatMap(i -> ThrowingSupplier.getCapturingExceptions(() -> {
            val node = objectMapper.readValue(i, TaskInputArgs.class);
            val creationArgs = node.creationArgs();
            return creationArgs == null ? null : objectMapper.treeToValue(creationArgs, type);
        }));
    }

    public TaskOutputArgs getOutput(Task task) {
        val sourceTask = taskPrettyRepository.findBySequence(task.getSequence());
        return sourceTask.flatMap(v -> ThrowingSupplier.getCapturingExceptions(() -> objectMapper.readValue(v.getOutput(),
                TaskOutputArgs.class))).orElse(TaskOutputArgs.create());
    }

    public Optional<TaskRoute> pickRouteFromRootMeta(TaskTree tree) {
        return ThrowingSupplier.getCapturingExceptions(() -> objectMapper.treeToValue(tree.getMeta().at("/route"),
                TaskRoute.class));

    }
}
