package com.brimmatech.docflow.v2.task.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.TaskState;

import java.util.ArrayList;
import java.util.List;

public class EncompassUpdateDtos {

    @Builder
    public record EncompassUploadStatuses(long id, JsonNode meta, Class<?> type, List<EncompassUploadStatus> statuses){
        public EncompassUploadStatuses {
            statuses = (statuses == null) ? new ArrayList<>() : new ArrayList<>(statuses);
        }

    }

    @Builder
    public record EncompassUploadStatus(TaskState status, String statusReason, String resolvedTarget){}
}
