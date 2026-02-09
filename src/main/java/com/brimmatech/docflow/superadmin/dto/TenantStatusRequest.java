package com.brimmatech.docflow.superadmin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TenantStatusRequest {
    @JsonProperty("column")
    private String column;
    @JsonProperty("value")
    private boolean value;
    @JsonProperty("lastUpdatedBy")
    private String lastUpdatedBy;
}
