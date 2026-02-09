package com.brimmatech.docflow.v2.task.dto;

import com.brimmatech.docflow.v2.task.dto.CreationArgs.SupervisorTaskInput;
import com.brimmatech.general.config.TemplateConfig.TOPICS;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.Optional;

@Data
@Builder
public class TaskStepDef {
    public record SupervisorDef(SupervisorTaskInput creationMeta) {
    };

    TOPICS topic;
    ObjectNode creationMeta;
    Optional<SupervisorDef> supervisor;
    Optional<String> notificationTopic;
    private JsonNode sftp;

    public static boolean hasCheckEnabled(Optional<TaskStepDef> taskDefOptional) {
        return taskDefOptional
                .flatMap(v -> {
                    return Optional.of(v.hasCheckEnabled());
                }).orElse(false);
    }

    public boolean hasCheckEnabled() {
        return this.supervisor
                .map(v -> v.creationMeta().hasCheckEnabled())
                .orElse(false);

    }

 
}