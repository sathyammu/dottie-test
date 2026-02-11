package com.brimmatech.docflow.v2.models;

import com.brimmatech.docflow.v2.task.dto.TaskStepDef;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "task_routes")
public class TaskRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    @Column(columnDefinition = "json", name = "config")
    @Type(JsonType.class)
    private List<TaskStepDef> config;

    @Transient
    private String triggeredEventType;

}
