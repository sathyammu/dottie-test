package com.brimmatech.encompass.conditions.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Application {
    private String entityId;
    private String entityType;
}
