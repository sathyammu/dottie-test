package com.brimmatech.encompass.loanreader.pipeline;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PipelineRequest {
    private List<String> fields;
    private Filter filter;
    @JsonProperty("includeArchivedLoans")
    private boolean includeArchivedLoans;
}
