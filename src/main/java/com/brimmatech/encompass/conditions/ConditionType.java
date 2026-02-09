package com.brimmatech.encompass.conditions;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@AllArgsConstructor
@Getter
public enum ConditionType {
    ENHANCED("enhanced"),
    PRELIMINARY("preliminary"),
    UNDERWRITING("underwriting"),
    POSTCLOSING("postclosing");

    private final String value;

    public static ConditionType fromString(String type) {
        if (type == null || type.isBlank()) {
            return ConditionType.ENHANCED;
        }

        return Arrays.stream(ConditionType.values())
                .filter(ct -> ct.getValue().equalsIgnoreCase(type.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid condition type: " + type));
    }

}

