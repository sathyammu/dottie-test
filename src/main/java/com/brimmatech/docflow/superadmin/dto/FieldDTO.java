package com.brimmatech.docflow.superadmin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


@Data
public class FieldDTO {
    @JsonProperty("Label")
    private String label;

    @JsonProperty("FieldName")
    private String fieldName;

    @JsonProperty("FieldType")
    private String fieldType;
}
