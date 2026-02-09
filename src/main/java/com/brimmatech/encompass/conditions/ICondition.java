package com.brimmatech.encompass.conditions;

import com.brimmatech.encompass.conditions.dto.EnhancedConditionUpdate;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ICondition {

    void updateEnhancedCondition(String accessToken, String loanIdentifier, String action, List<EnhancedConditionUpdate> request);

    void updateEnhancedConditionInPostClosing(String accessToken, String loanIdentifier, String action, List<EnhancedConditionUpdate> request);

    ResponseEntity<String> getConditionForLoanNumber(String accessToken, String loanIdentifier);
}
