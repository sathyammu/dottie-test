package com.brimmatech.encompass.loanreader.pipeline;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PipelineRequestV2 {
    private List<String> fields;
    private FilterV2 filter;
    @JsonProperty("includeArchivedLoans")
    private boolean includeArchivedLoans;
}
