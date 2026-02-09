package com.brimmatech.encompass.conditions.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConditionStatusRequest {
    String id;
    Boolean isRequested;
    Boolean isFulfilled;
    Boolean isRerequested;
    Boolean isReviewed;
    Boolean isReceived;
    Boolean isRejected;
    Boolean isCleared;
    Boolean isWaived;
    String conditionType;
    private Application application;
    private String status;
    private Boolean isChecked;
}
