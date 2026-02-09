package com.brimmatech.docflow.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SuspensionStrategy {

    SELF_SUSPENSION("SELF_SUSPENSION"),
    ACTIVATE_BY_CHILDREN("ACTIVATE_BY_CHILDREN");

    private final String name;
}
