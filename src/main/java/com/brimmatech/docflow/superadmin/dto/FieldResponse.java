package com.brimmatech.docflow.superadmin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FieldResponse {
    @JsonProperty("label")
    private String label;

    @JsonProperty("fieldName")
    private String fieldName;

    @JsonProperty("fieldType")
    private String fieldType;
}
