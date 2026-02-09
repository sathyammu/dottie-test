package com.brimmatech.encompass.loanreader.pipeline;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class PipelinePaginationResponse {

    private String loanId;

    private Map<String, String> fields;
}

