package com.brimmatech.docflow.v2.task.listeners;

import com.brimmatech.docflow.v2.repository.TaskTreeRepository;
import com.brimmatech.docflow.v2.task.dto.CreationArgs;
import com.brimmatech.docflow.v2.task.dto.CreationArgs.SupervisorTaskInput;
import com.brimmatech.docflow.v2.task.dto.CreationArgs.TaskInputArgs;
import com.brimmatech.general.config.TemplateConfig.TOPICS;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.Task;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.TaskDecision;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.TaskResult;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.TaskState;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class SupervisorWaker extends BatchedItemCollector<String> {

    private final ObjectMapper objectMapper;
    private final TaskTreeRepository taskTreeRepository;

    @Override
    public void onComplete(String topic, String trace, Map<Task, TaskDecision> decisions) {
        if (topic.equals(TOPICS.NOOP_SUPERVISOR.toString())) {
            return;
        }
        val collectedSupervisorIds = decisions.entrySet().stream()
                .filter(e -> e.getValue().getResult().equals(TaskResult.SUCCESS) || e.getValue().getResult().equals(TaskResult.FAILURE))
                .map(e -> {
                    return taskUtils.getTaskInput(e.getKey())
                            .map(v -> v.checkingSupervisor() != null && v.checkingSupervisor().hasCheckEnabled()
                                    ? String.format("%s:%s", v.qualifier(), v.checkingSupervisor().check())
                                    : null);
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .distinct()
                .toList();

        log.info("Adding supervisor nodes to unhibernate queue: {}", collectedSupervisorIds);
        addAll(collectedSupervisorIds);
    }

    public boolean check(CheckOptions doneAndStoppedAtLevel, TaskInputArgs actualInput) {
        return switch (doneAndStoppedAtLevel) {
            case CheckOptions.ALL_LEAVES_SUCCESS_AT_TERMINATION_LEVEL ->
                    checkIfAllNodesAreDoneAndStoppedAtLevel(actualInput,
                            List.of(TaskState.SUCCEEDED), true);
            case CheckOptions.ALL_LEAVES_SUCCESS_OR_FAILED_AT_TERMINATION_LEVEL ->
                    checkIfAllNodesAreDoneAndStoppedAtLevel(actualInput,
                            List.of(TaskState.SUCCEEDED, TaskState.FAILED), true);
            case CheckOptions.ALL_LEAVES_SUCCESS_OR_FAILED_AT_ANY_LEVEL ->
                    checkIfAllNodesAreDoneAndStoppedAtLevel(actualInput,
                            List.of(TaskState.SUCCEEDED, TaskState.FAILED), false);
        };
    }

    private boolean checkIfAllNodesAreDoneAndStoppedAtLevel(TaskInputArgs input, List<TaskState> taskState, boolean onlyAtTerminatedLevels
    ) {
        val supervisorArgs = input.getTypedInput(SupervisorTaskInput.class, objectMapper,
                () -> SupervisorTaskInput.builder().build());
        var qualifier = input.qualifier();

        String supervisorQualifier = String.format("%s:%s", input.qualifier(), supervisorArgs.id());

        val taskAndTreeNodeList = taskTreeRepository.findLeavesWatchedBySupervisor(
                supervisorQualifier,
                qualifier);

        if(taskAndTreeNodeList.isEmpty()){
            return false;
        }

        val result = taskAndTreeNodeList.stream()
                .allMatch(v -> {
                    CreationArgs.SupervisorTerminationInput termination = supervisorArgs.termination();
                    val isNodeSelectedByTermination = onlyAtTerminatedLevels
                            ? v.getNumLevels() == termination.stepNumber() + 1
                            && v.getTopic().equals(termination.topic())
                            : v.getNumLevels() >= 1;

                    return isNodeSelectedByTermination
                            && taskState.stream().anyMatch(targetState -> v.getState().equals(targetState.toString()));
                });
        log.debug("Checking is all children are done with args: <SupQualifier>({}), <TreeQualifier>({}): Result:{}",
                supervisorQualifier, qualifier, result);
        return result;
    }

    public enum CheckOptions {
        ALL_LEAVES_SUCCESS_AT_TERMINATION_LEVEL,
        ALL_LEAVES_SUCCESS_OR_FAILED_AT_TERMINATION_LEVEL,
        ALL_LEAVES_SUCCESS_OR_FAILED_AT_ANY_LEVEL,


    }
}
