package com.brimmatech.princeton.dottie.conditions;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
public class ConditionGroup {
    private String priorTo;
    private List<ConditionDTO> conditions;
}

