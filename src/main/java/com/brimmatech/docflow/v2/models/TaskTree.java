package com.brimmatech.docflow.v2.models;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Data;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.Task;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.ZonedDateTime;

@Data
@Entity
@Immutable
@Table(name = "task_tree")
@EntityListeners(AuditingEntityListener.class)
public class TaskTree {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition = "json", name = "meta")
    @Type(JsonType.class)
    private JsonNode meta;

    @Column(nullable = false, name = "qualifier")
    private String qualifier;

    @Column(name = "levels", nullable = false, columnDefinition = "ltree")
    private String levels;

    @CreatedDate
    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    public String getRootLevel() {
        return String.format("%s.%s", this.levels, this.id);
    }

    public String getNextLevel(Task task) {
        return String.format("%s.%s", this.levels, task.getSequence());
    }

    public String getRouteName() {
        return meta.at("/route/name").asText();
    }

    public long getTenantId() {
        return meta.at("/tenantId").asLong();
    }
}
