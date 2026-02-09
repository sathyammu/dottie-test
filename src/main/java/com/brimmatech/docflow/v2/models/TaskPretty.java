package com.brimmatech.docflow.v2.models;

import com.brimmatech.docflow.v2.models.converters.BooleanToRecreatedConverter;
import com.brimmatech.general.config.TemplateConfig;
import com.brimmatech.general.types.ThrowingSupplier;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.val;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.Task;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.TaskState;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Data @Entity @Immutable @Table(name = "task_pretty") public class TaskPretty {
    @Id private int sequence;

    private String identifier;

    private ZonedDateTime created;

    private ZonedDateTime completed;

    @Enumerated(EnumType.STRING) private TaskState state;

    @Convert(converter = BooleanToRecreatedConverter.class) private Boolean recreated;

    @Enumerated(EnumType.STRING) private TemplateConfig.TOPICS topic;

    @Column(columnDefinition = "json", name = "input_json") @Type(JsonType.class) private JsonNode inputJson;


    private String input;
    private String output;
    @Transient private JsonNode outputasJsonNode;

    public Optional<String> getInput() {
        return Optional.ofNullable(input);
    }

    public Task toTask() {
        return new Task(this.sequence, this.identifier, this.input);
    }

    public Optional<Integer> getTenantId() {
        return ThrowingSupplier.getCapturingExceptions(() -> inputJson.at("/tenantId").asInt());
    }

    public long getAliveTime() {
        return Duration.between(this.created, Objects.isNull(this.completed) ? this.created : this.completed)
                .toMillis();
    }

    public String qualifier() {
        return inputJson.at("/qualifier").asText();
    }

    public String parent() {
        return ThrowingSupplier.getCapturingExceptions(() -> inputJson.get("parent").asText()).orElse("");
    }

    public String level() {
        return String.format("%s.%s", parent(), getSequence());

    }

    public Map<String, Object> asUiMap(ObjectMapper objectMapper) {
        val newObjectMapper =
                objectMapper.copy();
        newObjectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        Map<String, Object> stringObjectMap = newObjectMapper.convertValue(this, new TypeReference<>() {
        });
        stringObjectMap.put("qualifier", qualifier());
        stringObjectMap.put("parent", parent());
        stringObjectMap.put("level", level());
        if (!stringObjectMap.containsKey("output")) {
            stringObjectMap.put("output", newObjectMapper.createObjectNode());
        }
        return stringObjectMap;
    }
}
