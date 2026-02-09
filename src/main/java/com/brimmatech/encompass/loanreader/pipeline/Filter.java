package com.brimmatech.encompass.loanreader.pipeline;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Filter {
    private String canonicalName;
    private String value;
    private String matchType;
}
