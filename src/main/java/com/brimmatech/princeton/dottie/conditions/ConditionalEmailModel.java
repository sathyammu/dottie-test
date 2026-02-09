package com.brimmatech.princeton.dottie.conditions;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ConditionalEmailModel {

    private String header;
    private String intro;
    private String loanNumber;
    private String borrowerLastName;
    private String dottieLink;

    private boolean showApprovalConditions;

    private Map<String, List<ConditionDTO>> approvalGroups;
    private Map<String, List<ConditionDTO>> borrowerGroups;
}

