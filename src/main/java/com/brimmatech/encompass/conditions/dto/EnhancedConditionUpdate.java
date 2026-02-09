package com.brimmatech.encompass.conditions.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnhancedConditionUpdate {
    private String conditionType;
    private String title;
    private String externalDescription;
    private String source;
    private String description;
    private Application application;
}