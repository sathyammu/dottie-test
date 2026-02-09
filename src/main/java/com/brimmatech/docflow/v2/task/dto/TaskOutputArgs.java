package com.brimmatech.docflow.v2.task.dto;

import com.brimmatech.general.types.ThrowingSupplier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Builder;
import lombok.Data;
import lombok.With;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Data
@Builder
public class TaskOutputArgs {
    @Builder
    public record Execution(String stageName,
                            long processTime,
                            ZonedDateTime startTime,
                            ZonedDateTime endTime,
                            @With ArrayNode logs,
                            @With String errorCode,
                            @With String errorMessage) {
    };

    @Builder
    public record Suspensions(ZonedDateTime suspendedAt, ZonedDateTime toBeActiveAt, String reason) {
    };

    @Builder
    public record Internals(List<Execution> executions,
            List<Suspensions> suspensions) {
    }

    Internals internals;

    @With
    JsonNode taskOutput;

    public TaskOutputArgs appendExecution(Execution exec) {
        if (internals == null) {
            this.internals = createInternals();
        }
        this.internals.executions.add(exec);
        return this;
    }

    public String toJsonString(ObjectMapper objectMapper) {
        return ThrowingSupplier.getCapturingExceptions(() -> {
            return objectMapper.writeValueAsString(this);
        }).orElse("");
    }

    public static TaskOutputArgs create() {
        return TaskOutputArgs.builder()
                .internals(createInternals())
                .build();
    }

    public static Internals createInternals() {
        return Internals.builder()
                .executions(new ArrayList<>())
                .suspensions(new ArrayList<>())
                .build();
    }

    public <T> T getTypedOutput(Class<T> type, ObjectMapper objectMapper, Supplier<T> defaultValue) {
        return ThrowingSupplier.getCapturingExceptions(() -> {
            return objectMapper.treeToValue(taskOutput, type);
        }).orElse(defaultValue.get());

    }
}
