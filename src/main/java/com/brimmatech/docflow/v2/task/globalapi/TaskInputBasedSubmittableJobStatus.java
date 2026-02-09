package com.brimmatech.docflow.v2.task.globalapi;

import com.brimmatech.docflow.v2.task.delegates.TaskDelegate;
import com.brimmatech.docflow.v2.task.dto.CreationArgs;
import com.brimmatech.docflow.v2.task.repository.ConnectionProviderTaskRepository;
import com.brimmatech.general.infra.ConnectionProvider;
import com.brimmatech.general.utils.OptionUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.Task;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Optional;


@RequiredArgsConstructor
@Service
@Primary
public class TaskInputBasedSubmittableJobStatus implements SubmittableJobStatusRepository {

    private static final String ROOT = "_twoStep";
    private final ConnectionProviderTaskRepository taskRepo;
    private final ConnectionProvider connectionProvider;

    @Override
    public Optional<SubmittableJobOutcome.JobStatus> load(TaskDelegate<?, ?> delegate, Task task, String businessKey) {
        var ti = delegate.getTaskInput(task).orElseThrow();
        var node = ti.creationArgs();
        var cps = node.path(ROOT).path("checkpoints");
        if (!cps.isObject() || !cps.has(businessKey)) return Optional.empty();
        return OptionUtils.fromJsonNode(cps.get(businessKey), v ->
                new SubmittableJobOutcome.JobStatus(businessKey, v.asText()));
    }

    @Override
    public void save(TaskDelegate<?, ?> delegate, Task task, SubmittableJobOutcome.JobStatus checkpoint) {
        try (var conn = connectionProvider.provideConnection()) {
            CreationArgs.TaskInputArgs ti = delegate.getTaskInput(task).orElseThrow();
            var root = ti.creationArgs().deepCopy();

            ArrayNode models = root.withArray("modelIds");

            if(models.isEmpty()) {
                ObjectNode newObject = models.addObject();
                newObject.put("operationLocation", checkpoint.operationLocation());
            }

            var two = ((ObjectNode) root).with(ROOT);

            var cps = two.with("checkpoints");
            cps.put(checkpoint.businessKey(), checkpoint.operationLocation());

            var updated = ti.withCreationArgs(root);
            taskRepo.updateInput(task, conn, updated);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save checkpoint", e);
        }
    }

    @Override
    public void delete(TaskDelegate<?, ?> delegate, Task task, String businessKey) {
        try (var conn = connectionProvider.provideConnection()) {
            CreationArgs.TaskInputArgs ti = delegate.getTaskInput(task).orElseThrow();
            var root = ti.creationArgs().deepCopy();
            var cps = root.path(ROOT).path("checkpoints");
            if (cps.isObject()) {
                ((ObjectNode) cps).remove(businessKey);
            }
            var updated = ti.withCreationArgs(root);
            taskRepo.updateInput(task, conn, updated);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete checkpoint", e);
        }
    }
}
