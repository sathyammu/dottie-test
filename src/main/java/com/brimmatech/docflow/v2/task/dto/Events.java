package com.brimmatech.docflow.v2.task.dto;

import com.brimmatech.docflow.v2.models.TaskPretty;
import com.brimmatech.general.config.TemplateConfig;
import lombok.Builder;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.Task;

public class Events {

@Builder
    public record EventGrouper(CreationArgs.TaskInputArgs input, TemplateConfig.TOPICS topic, TaskPretty task){};
}
