package com.brimmatech.docflow.v2.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskPayload {
    private String loanId;
    private String conditionId;
    private String conditionType;
    private JsonNode comments;
    private String userId;
    private Long timestamp;
}