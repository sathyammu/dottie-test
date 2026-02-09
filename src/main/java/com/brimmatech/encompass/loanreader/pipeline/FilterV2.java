package com.brimmatech.encompass.loanreader.pipeline;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FilterV2 {
    private String canonicalName;
    @JsonProperty("value")
    private List<String> values;
    private String matchType;
}
