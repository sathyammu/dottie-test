package com.brimmatech.docflow.superadmin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Tenant {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("sor_id")
    private int sorId;

    @JsonProperty("tenant_name")
    private String tenantName;

    @JsonProperty("is_active")
    private boolean isActive;

    @JsonProperty("created_by")
    private String createdBy;

    @JsonProperty("last_updated_by")
    private String lastUpdatedBy;

    @JsonProperty("created_date")
    private LocalDateTime createdDate;

    @JsonProperty("last_updated_date")
    private LocalDateTime lastUpdatedDate;

    @JsonProperty("archived_time")
    private LocalDateTime archivedTime;

    @JsonProperty("is_archived")
    private boolean isArchived;

    @JsonProperty("pipeline_request")
    private String pipelineRequest;

    @JsonProperty("connection_status")
    private String connectionStatus;

    @JsonProperty("sor_metadata")
    private String sorMetaData;
}
