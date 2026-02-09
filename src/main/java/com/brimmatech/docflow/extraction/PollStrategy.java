package com.brimmatech.docflow.extraction;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PollStrategy {

    REST_API("REST_API"),
    REST_API_SUSPEND("REST_API_SUSPEND"),
    SDK("SDK");

    private final String name;
}
