package com.brimmatech.princeton.dottie.conditions;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ConditionDTO {

    private String id;
    private String title;
    private String description;
    private String internalDescription;
    private String comment;
    private String category;
    private String priorTo;
    private String conditionType;
    private LocalDate statusDate;
    private boolean open;
    private int order;
    private String status;
    private boolean isCleared;
}

