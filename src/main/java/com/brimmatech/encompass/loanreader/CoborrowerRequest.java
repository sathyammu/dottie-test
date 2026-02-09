package com.brimmatech.encompass.loanreader;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CoborrowerRequest {
    private boolean bankruptcyIndicatorChapterSeven;
    private boolean bankruptcyIndicatorChapterTwelve;
    private boolean bankruptcyIndicatorChapterThirteen;
    private boolean bankruptcyIndicatorChapterEleven;
}
