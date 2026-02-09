package com.brimmatech.encompass.conditions.dto;

import lombok.Data;

import java.util.List;

@Data
public class EnhancedConditionUpdateRequest {
    private String loanNumber;
    private List<EnhancedConditionUpdate> conditions;
    private String investorLoanNumber;
}
