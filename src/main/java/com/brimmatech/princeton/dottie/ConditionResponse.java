package com.brimmatech.princeton.dottie;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ConditionResponse {
    private String conditionId;
    private String title;
    private String conditionType;
    private List<DocumentResponse> documents;
}

