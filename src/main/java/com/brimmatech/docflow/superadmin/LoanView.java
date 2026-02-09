package com.brimmatech.docflow.superadmin;

import com.brimmatech.encompass.loanreader.pipeline.PipelinePaginationResponse;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class LoanView {
    private PipelinePaginationResponse loan;
    private Map<String, String> sectionStatus = new HashMap<>();
    private Map<String, String> subStatus = new HashMap<>();
    private int pastDueCount;

    public Map<String, String> getFields() {
        return loan != null ? loan.getFields() : null;
    }
}
