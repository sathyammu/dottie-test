package com.brimmatech.docflow.superadmin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SystemOfRecordsResponse {

    @JsonProperty("system_of_record_id")
    private int systemOfRecordId;

    @JsonProperty("system_of_record_name")
    private String systemOfRecordName;

    @JsonProperty("fields")
    private List<FieldResponse> fields;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("created_time")
    private LocalDateTime createdTime;

    @JsonProperty("created_by")
    private String createdBy;

    @JsonProperty("last_updated_time")
    private LocalDateTime lastUpdatedTime;

    @JsonProperty("last_updated_by")
    private String lastUpdatedBy;
}
